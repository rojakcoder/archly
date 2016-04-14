package com.rojakcoder.archly;

import java.util.ArrayList;
import java.util.HashMap;
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
	 * @param entry The ID of the entry to add.
	 * @throws DuplicateEntryException Throws DuplicateEntryException if the
	 * entry is already in the registry.
	 */
	void add(String entry) throws DuplicateEntryException {
		if (registry.containsKey(entry)) {
			throw new DuplicateEntryException(String.format(DUPLICATE_ENTRIES,
					entry));
		}
		registry.put(entry, "");
	}

	/**
	 * Adds an entry under a parent entry to the registry.
	 *
	 * @param child The ID of the child entry to add.
	 * @param parent The ID of the parent entry under which to place the child
	 * entry.
	 * @throws DuplicationException Throws this exception if the child entry is
	 * already in the registry.
	 */
	void add(String child, String parent) throws DuplicateEntryException {
		if (registry.containsKey(child)) {
			throw new DuplicateEntryException(String.format(DUPLICATE_ENTRIES,
					child));
		}
		registry.put(child, parent);
	}

	/**
	 * Empties the registry.
	 */
	void clear() {
		registry.clear();
	}

	/**
	 * Exports a snapshot of the registry.
	 *
	 * @return A HashMap typically meant for persistent storage.
	 */
	Map<String, String> export() {
		return new HashMap<String, String>(registry);
	}

	/**
	 * Checks if the entry is stored in the registry.
	 *
	 * @param entry The ID of the entry to check.
	 * @return True if the ID of the entry is present in the registry.
	 */
	boolean has(String entry) {
		return registry.containsKey(entry);
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
	 * Re-creates the registry with a new hierarchy.
	 *
	 * @param map The map containing the new hierarchy.
	 */
	void importRegistry(Map<String, String> map) {
		registry = new ConcurrentHashMap<String, String>(map);
	}

	/**
	 * Creates a traversal path from the entry to the root.
	 *
	 * @param entry The ID of the entry to start traversing from.
	 * @return A list of entry IDs starting from the entry and ending with the
	 * root.
	 */
	List<String> traverseRoot(String entry) {
		List<String> path = new ArrayList<>();

		if (entry == null) {
			path.add("*");

			return path;
		}

		String eId = entry;

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
	String display(AclEntry loader, String leading, String entryId) {
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
			sb.append(this.display(loader, " " + leading, childId));
		}

		return sb.toString();
	}

	/**
	 * Removes an entry from the registry.
	 *
	 * @param entry The ID of the entry to remove from the registry.
	 * @param removeDescendants If true, all child entries and descendants are
	 * removed as well.
	 * @throws NotFoundException Throws this exception if the entry or any of
	 * the descendants (if {@code removeDscendants} is true} are not found.
	 */
	void remove(String entry, boolean removeDescendants)
			throws EntryNotFoundException {
		if (!registry.containsKey(entry)) {
			throw new EntryNotFoundException(String.format(NOT_FOUND, entry));
		}

		if (hasChild(entry)) {
			String parentId = registry.get(entry);
			List<String> childIds = findChildren(entry);

			if (removeDescendants) {
				this.removeDescendants(childIds);
			} else {
				for (String childId: childIds) {
					registry.put(childId, parentId);
				}
			}
		}

		registry.remove(entry);
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
