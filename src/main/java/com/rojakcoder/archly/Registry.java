package com.rojakcoder.archly;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.rojakcoder.archly.exceptions.DuplicateEntryException;
import com.rojakcoder.archly.exceptions.EntryNotFoundException;

/**
 * Registry is a generic registry that can be sub-classed for resources and
 * roles.
 */
class Registry {
	static final String DUPLICATE_CHILD = "Entry '%s' is already a child of '%s'";

	static final String DUPLICATE_ENTRIES = "Entry '%s' is already in the registry - cannot add duplicate.";

	static final String NOT_FOUND = "Entry '%s' not in registry.";

	/**
	 * The internal representation of the registry.
	 */
	protected ConcurrentMap<String, String> registry;

	/**
	 * The constructor for creating the registry.
	 * <p>
	 * This is accessible only to child classes for making singleton
	 * implementations.
	 * </p>
	 */
	protected Registry() {
		registry = new ConcurrentHashMap<>();
	}

	/**
	 * Prints the traversal path from the entry to the root.
	 *
	 * @param path The list representing the path.
	 * @return The string illustrating the path.
	 */
	static String print(List<String> path) {
		StringBuilder sb = new StringBuilder("-");

		for (String step: path) {
			sb.append(" -> ");
			sb.append(step);
		}
		sb.append(" <");

		return sb.toString();
	}

	/**
	 * Adds an entry to the registry.
	 *
	 * @param entry The entry to add.
	 * @throws DuplicateEntryException Throws DuplicateEntryException if the
	 * entry is already in the registry.
	 */
	void add(AclEntry entry) throws DuplicateEntryException {
		if (registry.containsKey(entry.getId())) {
			throw new DuplicateEntryException(String.format(DUPLICATE_ENTRIES,
					entry.getId()));
		}
		registry.put(entry.getId(), "");
	}

	/**
	 * Adds an entry under a parent entry to the registry.
	 *
	 * @param child The child entry to add.
	 * @param parent The parent entry under which to place the child entry.
	 * @throws DuplicationException Throws this exception if the child entry is
	 * already in the registry.
	 */
	void add(AclEntry child, AclEntry parent) throws DuplicateEntryException {
		if (registry.containsKey(child.getId())) {
			throw new DuplicateEntryException(String.format(DUPLICATE_ENTRIES,
					child.getId()));
		}
		registry.put(child.getId(), parent.getId());
	}

	/**
	 * Checks if the entry is stored in the registry.
	 *
	 * @param entry The entry to check.
	 * @return True if the ID of the entry is present in the registry.
	 */
	boolean has(AclEntry entry) {
		return registry.containsKey(entry.getId());
	}

	/**
	 * Checks if there are children IDs under the specified ID.
	 *
	 * @param parentId The ID of the parent to check for.
	 * @return True if there is at least one child ID.
	 */
	boolean hasChild(String parentId) {
		return registry.containsValue(parentId);
	}

	/**
	 * Creates a traversal path from the entry to the root.
	 *
	 * @param entry The entry to start traversing from.
	 * @return A list of entry IDs starting from the entry and ending with the
	 * root.
	 */
	List<String> traverseRoot(AclEntry entry) {
		List<String> path = new ArrayList<>();

		if (entry == null) {
			path.add("*");

			return path;
		}

		String eId = entry.getId();

		while (registry.containsKey(eId)) {
			path.add(eId);
			eId = registry.get(eId);
		}
		path.add("*");

		return path;
	}

	/**
	 * Prints a cascading list of entries in this registry.
	 *
	 * @param loader The instance that retrieves other instances.
	 * @param leading The leading space for indented entries.
	 * @param entryId The ID of the entry to start traversing from.
	 * @return The string representing the parent-child relationships between
	 * the entries.
	 */
	String print(AclEntry loader, String leading, String entryId) {
		StringBuilder sb = new StringBuilder();
		List<String> childIds = null;

		if (leading == null) {
			leading = "";
		}
		if (entryId == null) {
			entryId = "";
		}

		childIds = findChildren(entryId);
		for (String childId: childIds) {
			AclEntry entry = loader.retrieveEntry(childId);
			sb.append(leading);
			sb.append("- ");
			sb.append(entry.getEntryDescription());
			sb.append("\n");
			sb.append(print(loader, " " + leading, childId));
		}

		return sb.toString();
	}

	/**
	 * Removes an entry from the registry.
	 *
	 * @param entry The entry to remove from the registry.
	 * @param removeDescendants If true, all child entries and descendants are
	 * removed as well.
	 * @throws NotFoundException Throws this exception if the entry or any of
	 * the descendants (if {@code removeDscendants} is true} are not found.
	 */
	void remove(AclEntry entry, boolean removeDescendants)
			throws EntryNotFoundException {
		if (!registry.containsKey(entry.getId())) {
			throw new EntryNotFoundException(String.format(NOT_FOUND,
					entry.getId()));
		}

		if (hasChild(entry.getId())) {
			String parentId = registry.get(entry.getId());
			List<String> childIds = findChildren(entry.getId());

			if (removeDescendants) {
				removeDescendants(childIds);
			} else {
				for (String childId: childIds) {
					registry.put(childId, parentId);
				}
			}
		}

		registry.remove(entry.getId());
	}

	/**
	 * Gets the size of the registry.
	 * <p>
	 * Note that the registry contains individual entries, including all parent
	 * and child entries. This means that the size is a simple raw count of all
	 * entries.
	 * </p>
	 *
	 * @return The size of the registry.
	 */
	int size() {
		return registry.size();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();

		for (Map.Entry<String, String> entry: registry.entrySet()) {
			sb.append("\t");
			sb.append(entry.getKey());
			if (entry.getKey().length() >= 8) {
				sb.append("\t - \t");
			} else {
				sb.append("\t\t - \t");
			}
			if (entry.getValue().equals("")) {
				sb.append("*");
			} else {
				sb.append(entry.getValue());
			}
			sb.append("\n");
		}

		return sb.toString();
	}

	private void removeDescendants(List<String> entryIds) {
		for (String entryId: entryIds) {
			String node = entryId;

			registry.remove(node);
			while (hasChild(node)) {
				removeDescendants(findChildren(node));
			}
		}
	}

	private List<String> findChildren(String parentId) {
		List<String> children = new ArrayList<>();

		for (Map.Entry<String, String> entry: registry.entrySet()) {
			if (entry.getValue().equals(parentId)) {
				children.add(entry.getKey());
			}
		}

		return children;
	}
}
