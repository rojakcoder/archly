package com.rojakcoder.archly;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.rojakcoder.archly.exceptions.EntryNotFoundException;

/**
 * Permission manages the permissions assigned to role-resource tuples.
 * <p>
 * Default permission is deny on all resources.
 * </p>
 */
class Permission {
	/**
	 * A flag for testing purposes.
	 */
	static boolean RUNTEST = false;

	private static final String NOT_FOUND = "Permission %s not found on %s for %s";

	private static final String DEFAULT_KEY = "*::*";

	/**
	 * The map of role-resource tuple to permissions.
	 * <p>
	 * The first level key is the tuple, the second level key is the action.
	 * Available values are "ALL", "CREATE", "READ", "UPDATE", "DELETE"
	 * </p>
	 */
	ConcurrentMap<String, Map<String, Boolean>> permissions;

	static enum Types {
		ALL, CREATE, READ, UPDATE, DELETE
	}

	Permission() {
		permissions = new ConcurrentHashMap<>();
		makeDefaultDeny();
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Size: ");

		sb.append(permissions.size());
		sb.append("\n-------\n");

		int i = 0;
		for (Map.Entry<String, Map<String, Boolean>> entry: permissions
				.entrySet()) {
			i++;
			sb.append(i + "- ");
			sb.append(entry.getKey());
			sb.append("\n");
			for (Map.Entry<String, Boolean> e: entry.getValue().entrySet()) {
				sb.append("\t");
				sb.append(e.getKey());
				sb.append("\t");
				sb.append(e.getValue());
				sb.append("\n");
			}
		}

		return sb.toString();
	}

	/**
	 * Grants permission on resource to role.
	 * <p>
	 * Overrides any existing permissions on all actions.
	 * </p>
	 *
	 * @param role The ID of the access request object.
	 * @param resource The ID of the access control object.
	 */
	void allow(String role, String resource) {
		String key = makeKey(role, resource);
		Map<String, Boolean> perm = makePermission(Types.ALL, true);

		permissions.put(key, perm);
	}

	/**
	 * Grants action permission on resource to role.
	 * <p>
	 * Adds the action permission to any existing actions. Overrides the
	 * existing action permission if any.
	 * </p>
	 *
	 * @param role The ID of the access request object.
	 * @param resource The ID of the access control object.
	 * @param action The specific action on the resource.
	 */
	void allow(String role, String resource, Types action) {
		String key = makeKey(role, resource);
		Map<String, Boolean> perm = null;

		if (has(key)) {
			perm = permissions.get(key);
			perm.put(action.toString(), true);
		} else {
			perm = makePermission(action, true);
		}
		permissions.put(key, perm);
	}

	/**
	 * Removes all permissions.
	 */
	void clear() {
		permissions.clear();
	}

	/**
	 * Denies permission on resource to role.
	 * <p>
	 * Overrides any existing permissions on all actions.
	 * </p>
	 *
	 * @param role The ID of the access request object.
	 * @param resource The ID of the access control object.
	 */
	void deny(String role, String resource) {
		String key = makeKey(role, resource);
		Map<String, Boolean> perm = makePermission(Types.ALL, false);

		permissions.put(key, perm);
	}

	/**
	 * Denies action permission on resource to role.
	 * <p>
	 * Adds the action permission to any existing actions. Overrides the
	 * existing action permission if any.
	 * </p>
	 *
	 * @param role The ID of the access request object.
	 * @param resource The ID of the access control object.
	 * @param action The specific action on the resource.
	 */
	void deny(String role, String resource, Types action) {
		String key = makeKey(role, resource);
		Map<String, Boolean> perm = null;

		if (has(key)) {
			perm = permissions.get(key);
			perm.put(action.toString(), false);
		} else {
			perm = makePermission(action, false);
		}
		permissions.put(key, perm);
	}

	/**
	 * Exports a snapshot of the permissions map.
	 *
	 * @return A map with string keys and map values where the values are maps
	 * of string to boolean entries. Typically meant for persistent storage.
	 */
	Map<String, Map<String, Boolean>> export() {
		return new HashMap<String, Map<String, Boolean>>(permissions);
	}

	/**
	 * Re-creates the permission map with a new of permissions.
	 *
	 * @param map The map containing the new permissions. The first-level string
	 * is a permission key (<aco>::<aro>); the second-level string is the set of
	 * actions; the boolean value indicates whether the permission is explicitly
	 * granted/denied.
	 */
	void importMap(Map<String, Map<String, Boolean>> map) {
		this.permissions = new ConcurrentHashMap<>(map);
	}

	/**
	 * Determines if the role has access on the resource.
	 *
	 * @param role The ID of the access request object.
	 * @param resource The ID of the access control object.
	 * @return Returns true only if the role has been explicitly given access to
	 * all actions on the resource. Returns false if the role has been
	 * explicitly denied access. Returns null otherwise.
	 */
	Boolean isAllowed(String role, String resource) {
		String key = makeKey(role, resource);
		int allSet = 0;

		if (!has(key)) {
			return null;
		}

		Map<String, Boolean> perm = permissions.get(key);
		for (Map.Entry<String, Boolean> entry: perm.entrySet()) {
			//if any entry is false, resource is NOT allowed
			if (!entry.getValue()) {
				return false;
			}
			if (!entry.getKey().equals(Types.ALL.toString())) {
				allSet++;
			}
		}
		if (perm.containsKey(Types.ALL.toString())) {
			return true; //true because ALL = false would be caught in the loop
		}

		if (allSet == 4) {
			return true;
		}

		return null;
	}

	/**
	 * Determines if the role has access on the resource for the specific
	 * action.
	 * <p>
	 * The permission on the specific action is evaluated to see if it has been
	 * specified. If not specified, the permission on the <code>ALL</code>
	 * permission is evaluated. If both are not specified, <code>null</code> is
	 * returned.
	 * </p>
	 *
	 * @param role The ID of the access request object.
	 * @param resource The ID of the access control object.
	 * @param action The access action.
	 * @return Returns true if the role has access to the specified
	 * action on the resource. Returns false if the role is denied
	 * access. Returns null if no permission is specified.
	 */
	Boolean isAllowed(String role, String resource, Types action) {
		String key = makeKey(role, resource);

		if (!has(key)) {
			return null;
		}

		Map<String, Boolean> perm = permissions.get(key);
		if (!perm.containsKey(action.toString())) {
			//if specific action is not present, check for ALL
			if (!perm.containsKey(Types.ALL.toString())) {
				return null;
			}

			return perm.get(Types.ALL.toString());
		} //else specific action is present

		return perm.get(action.toString());
	}

	/**
	 * Determines if the role is denied access on the resource.
	 *
	 * @param role The ID of the access request object.
	 * @param resource The ID of the access control object.
	 * @return Returns true only if the role has been explicitly denied access
	 * to all actions on the resource. Returns false if the role has been
	 * explicitly granted access. Returns null otherwise.
	 */
	Boolean isDenied(String role, String resource) {
		String key = makeKey(role, resource);
		int allSet = 0;

		if (!has(key)) {
			return null;
		}

		Map<String, Boolean> perm = permissions.get(key);
		for (Map.Entry<String, Boolean> entry: perm.entrySet()) {
			//if any entry is true, resource is NOT denied
			if (entry.getValue()) {
				return false;
			}
			if (!entry.getKey().equals(Types.ALL.toString())) {
				allSet++;
			}
		}
		if (perm.containsKey(Types.ALL.toString())) {
			return true; //true because ALL = true would be caught in the loop
		}

		if (allSet == 4) {
			return true;
		}

		return null;
	}

	/**
	 * Determines if the role is denied access on the resource for the specific
	 * action.
	 * <p>
	 * The permission on the specific action is evaluated to see if it has been
	 * specified. If not specified, the permission on the <code>ALL</code>
	 * permission is evaluated. If both are not specified, <code>null</code> is
	 * returned.
	 * </p>
	 *
	 * @param role The ID of the access request object.
	 * @param resource The ID of the access control object.
	 * @param action The access action.
	 * @return Returns true if the role is denied access to the specified
	 * action on the resource. Returns false if the role has
	 * access. Returns null if no permission is specified.
	 */
	Boolean isDenied(String role, String resource, Types action) {
		String key = makeKey(role, resource);

		if (!has(key)) {
			return null;
		}

		Map<String, Boolean> perm = permissions.get(key);
		if (!perm.containsKey(action.toString())) {
			//if specific action is not present, check for ALL
			if (!perm.containsKey(Types.ALL.toString())) {
				return null;
			}

			return !perm.get(Types.ALL.toString());
		} //else specific action is present

		return !perm.get(action.toString());
	}

	/**
	 * Makes the default permission allow.
	 */
	void makeDefaultAllow() {
		permissions.put(DEFAULT_KEY, makePermission(Types.ALL, true));
	}

	/**
	 * Makes the default permission deny.
	 */
	void makeDefaultDeny() {
		permissions.put(DEFAULT_KEY, makePermission(Types.ALL, false));
	}

	/**
	 * Removes all permissions on resource from role.
	 * <p>
	 * All permissions are removed, not just the ALL permission type.
	 * </p>
	 *
	 * @param role The access request object.
	 * @param resource The access control object.
	 * @throws EntryNotFoundException Throws EntryNotFoundException if the
	 * permission is not available.
	 */
	void remove(String role, String resource) throws EntryNotFoundException {
		String key = makeKey(role, resource);
		String resId = resource == null ? "*" : resource;
		String roleId = role == null ? "*" : role;

		if (!has(key)) {
			throw new EntryNotFoundException(String.format(NOT_FOUND, key,
					resId, roleId));
		}
		permissions.remove(key);
	}

	/**
	 * Removes the specified permission on resource from role.
	 *
	 * @param role The ID of the access request object.
	 * @param resource The ID of the access control object.
	 * @param action The access action.
	 * @throws EntryNotFoundException Throws EntryNotFoundException if the
	 * permission is not available.
	 */
	void remove(String role, String resource, Types action)
			throws EntryNotFoundException {
		String key = makeKey(role, resource);

		if (!has(key)) {
			throw new EntryNotFoundException(String.format(NOT_FOUND, key,
					resource, role));
		}

		Map<String, Boolean> perm = permissions.get(key);

		if (perm.containsKey(action.toString())) {
			perm.remove(action.toString());
		} else if (perm.containsKey(Types.ALL.toString())) {
			boolean originalValue = perm.get(Types.ALL.toString());

			//has ALL - remove and put in the others
			perm.remove(Types.ALL.toString());
			for (Types type: Types.values()) {
				if (type != action && type != Types.ALL) {
					perm.put(type.toString(), originalValue);
				}
			}
		} else {
			throw new EntryNotFoundException(String.format(NOT_FOUND,
					action.toString(), resource, role));
		}

		if (perm.size() == 0) {
			permissions.remove(key);
		} else {
			permissions.put(key, perm);
		}
	}

	/**
	 * Removes all permissions related to the resource.
	 *
	 * @param resource The ID of the resource to remove.
	 * @return The number of removed permissions. This number may be different
	 * from what is expected due to the concurrent nature of the permission map.
	 */
	int removeByResource(String resource) {
		Set<String> toRemove = new HashSet<>();
		resource = "::" + resource;

		for (String key: permissions.keySet()) {
			if (key.endsWith(resource)) {
				toRemove.add(key);
			}
		}

		return del(toRemove);
	}

	/**
	 * Removes all permissions related to the role.
	 *
	 * @param role The ID of the role to remove.
	 * @return The number of removed permissions. This number may be different
	 * from what is expected due to the concurrent nature of the permission map.
	 */
	int removeByRole(String role) {
		Set<String> toRemove = new HashSet<>();
		role += "::";

		for (String key: permissions.keySet()) {
			if (key.startsWith(role)) {
				toRemove.add(key);
			}
		}

		return del(toRemove);
	}

	/**
	 * The number of specified permissions.
	 *
	 * @return The number of permissions in the registry.
	 */
	int size() {
		return permissions.size();
	}

	private int del(Set<String> keys) {
		int removed = 0;
		if (RUNTEST) {
			System.out.println("Expecting to remove " + keys.size());
		}
		for (String key: keys) {
			if (permissions.remove(key) != null) {
				removed++;
			}
			if (RUNTEST) {
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {
				}
			} //else do nothing
		}

		return removed;
	}

	/**
	 * Determines if the role-resource tuple is available.
	 *
	 * @param key The tuple of role and resource.
	 * @return Returns true if the permission is available for this tuple.
	 */
	private boolean has(String key) {
		return permissions.containsKey(key);
	}

	private String makeKey(String aro, String aco) {
		String role = aro == null ? "*" : aro;
		String resource = aco == null ? "*" : aco;

		return role + "::" + resource;
	}

	private Map<String, Boolean> makePermission(Types action, boolean allow) {
		Map<String, Boolean> perm = new HashMap<>();

		perm.put(action.toString(), allow);

		return perm;
	}
}
