package com.rojakcoder.archly;

import java.util.HashMap;
import java.util.Map;
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
	private static final String NOT_FOUND = "Permission %s not found on %s for %s";

	private static Permission instance;

	/**
	 * The map of role-resource tuple to permissions.
	 * <p>
	 * The first level key is the tuple, the second level key is the action.
	 * Available values are "ALL", "CREATE", "READ", "UPDATED", "DELETE"
	 * </p>
	 */
	ConcurrentMap<String, Map<String, Boolean>> permissions;

	static enum Types {
		ALL, CREATE, READ, UPDATE, DELETE
	}

	private Permission() {
		permissions = new ConcurrentHashMap<>();
		makeDefaultDeny();
	}

	/**
	 * Gets the singleton instance of Permission.
	 *
	 * @return The singleton instance.
	 */
	static Permission getSingleton() {
		if (instance == null) {
			instance = new Permission();
		}

		return instance;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("Size: ");

		sb.append(permissions.size());
		sb.append("\n-------\n");

		for (Map.Entry<String, Map<String, Boolean>> entry: permissions
				.entrySet()) {
			sb.append("- ");
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
	 * @param role The access request object.
	 * @param resource The access control object.
	 */
	void allow(AclEntry role, AclEntry resource) {
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
	 * @param role The access request object.
	 * @param resource The access control object.
	 * @param action The specific action on the resource.
	 */
	void allow(AclEntry role, AclEntry resource, Types action) {
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
	 * Denies permission on resource to role.
	 * <p>
	 * Overrides any existing permissions on all actions.
	 * </p>
	 *
	 * @param role The access request object.
	 * @param resource The access control object.
	 */
	void deny(AclEntry role, AclEntry resource) {
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
	 * @param role The access request object.
	 * @param resource The access control object.
	 * @param action The specific action on the resource.
	 */
	void deny(AclEntry role, AclEntry resource, Types action) {
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
	 * Determines if the role-resource tuple is available.
	 *
	 * @param key The tuple of role and resource.
	 * @return Returns true if the permission is available for this tuple.
	 */
	private boolean has(String key) {
		return permissions.containsKey(key);
	}

	/**
	 * Determines if the role has access on the resource.
	 *
	 * @param role The access request object.
	 * @param resource The access control object.
	 * @return Returns true only if the role has access to all actions on the
	 * resource. Returns false otherwise.
	 */
	boolean isAllowed(AclEntry role, AclEntry resource) {
		String aroId = role == null ? null : role.getId();
		String acoId = resource == null ? null : resource.getId();

		return isAllowed(aroId, acoId);
	}

	/**
	 * Determines if the role has access on the resource.
	 *
	 * @param role The access request object.
	 * @param resource The access control object.
	 * @return Returns true only if the role has access to all actions on the
	 * resource. Returns false otherwise.
	 */
	boolean isAllowed(String role, String resource) {
		String key = makeKey(role, resource);
		int allSet = 0;

		if (!has(key)) {
			return false;
		}

		Map<String, Boolean> perm = permissions.get(key);
		for (Map.Entry<String, Boolean> entry: perm.entrySet()) {
			//if any entry is false, resource is NOT allowed
			if (!entry.getValue()) {
				return false;
			}
			allSet++;
		}
		if (perm.containsKey(Types.ALL.toString())) {
			return true; //true because ALL = false would be caught in the loop
		}

		return allSet == 4;
	}

	boolean isAllowed(AclEntry role, AclEntry resource, Types action) {
		String aroId = role == null ? null : role.getId();
		String acoId = resource == null ? null : resource.getId();

		return isAllowed(aroId, acoId, action);
	}

	/**
	 * Determines if the role has access on the resource for the specified
	 * action.
	 *
	 * @param role The access request object.
	 * @param resource The access control object.
	 * @param action The access action.
	 * @return Returns true if the role has access to the specified action on
	 * the resource. If the ALL permission has been granted on the resource for
	 * the role, this returns true as well. Returns false otherwise.
	 */
	boolean isAllowed(String role, String resource, Types action) {
		String key = makeKey(role, resource);

		if (!has(key)) {
			return false;
		}

		Map<String, Boolean> perm = permissions.get(key);
		if (!perm.containsKey(action.toString())) {
			//if specific action is not present, check for ALL
			if (!perm.containsKey(Types.ALL.toString())) {
				return false;
			}

			return perm.get(Types.ALL.toString());
		} //else specific action is present

		return perm.get(action.toString());
	}

	/**
	 * Determines if the role is denied access on the resource.
	 *
	 * @param role The access request object.
	 * @param resource The access control object.
	 * @return Returns true only if the role has been explicitly denied access
	 * to all actions on the resource. Returns false otherwise.
	 */
	boolean isDenied(AclEntry role, AclEntry resource) {
		String aro = role == null ? null : role.getId();
		String aco = resource == null ? null : resource.getId();

		return isDenied(aro, aco);
	}

	/**
	 * Determines if the role is denied access on the resource.
	 *
	 * @param role The ID of the access request object.
	 * @param resource The ID of the access control object.
	 * @return Returns true only if the role has been explicitly denied access
	 * to all actions on the resource. Returns false otherwise.
	 */
	boolean isDenied(String role, String resource) {
		String key = makeKey(role, resource);
		int allSet = 0;

		if (!has(key)) {
			return false;
		}

		Map<String, Boolean> perm = permissions.get(key);
		for (Map.Entry<String, Boolean> entry: perm.entrySet()) {
			//if any entry is true, resource is NOT denied
			if (entry.getValue()) {
				return false;
			}
			allSet++;
		}
		if (perm.containsKey(Types.ALL.toString())) {
			return true; //true because ALL = true would be caught in the loop
		}

		return allSet == 4;
	}

	boolean isDenied(AclEntry role, AclEntry resource, Types action) {
		String aroId = role == null ? null : role.getId();
		String acoId = resource == null ? null : resource.getId();

		return isDenied(aroId, acoId, action);
	}

	/**
	 * Determines if the role is denied access on the resource for the specified
	 * action.
	 *
	 * @param role The access request object.
	 * @param resource The access control object.
	 * @param action The access action.
	 * @return Returns true if the role has been explicitly denied access to the
	 * specified action for the resource. If the ALL permission has been denied
	 * on the resource for the role, this returns true as well. Returns false
	 * otherwise.
	 */
	boolean isDenied(String role, String resource, Types action) {
		String key = makeKey(role, resource);

		if (!has(key)) {
			return false;
		}

		Map<String, Boolean> perm = permissions.get(key);
		if (!perm.containsKey(action.toString())) {
			//if specific action is not present, check for ALL
			if (!perm.containsKey(Types.ALL.toString())) {
				return false;
			}

			return !perm.get(Types.ALL.toString());
		} //else specific action is present

		return !perm.get(action.toString());
	}

	/**
	 * Makes the default permission allow.
	 */
	void makeDefaultAllow() {
		permissions.put("*::*", makePermission(Types.ALL, true));
	}

	/**
	 * Makes the default permission deny.
	 */
	void makeDefaultDeny() {
		permissions.put("*::*", makePermission(Types.ALL, false));
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
	void remove(AclEntry role, AclEntry resource) throws EntryNotFoundException {
		String key = makeKey(role, resource);
		String resId = resource == null ? "*" : resource.getId();
		String roleId = role == null ? "*" : role.getId();

		if (!has(key)) {
			throw new EntryNotFoundException(String.format(NOT_FOUND, "-",
					resId, roleId));
		}
		permissions.remove(key);
	}

	/**
	 * Removes the specified permission on resource from role.
	 *
	 * @param role The access request object.
	 * @param resource The access control object.
	 * @param action The access action.
	 * @throws EntryNotFoundException Throws EntryNotFoundException if the
	 * permission is not available.
	 */
	void remove(AclEntry role, AclEntry resource, Types action)
			throws EntryNotFoundException {
		String key = makeKey(role, resource);
		String resId = resource == null ? "*" : resource.getId();
		String roleId = role == null ? "*" : role.getId();

		if (!has(key)) {
			throw new EntryNotFoundException(String.format(NOT_FOUND, "-",
					resId, roleId));
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
					action.toString(), resId, roleId));
		}

		if (perm.size() == 0) {
			permissions.remove(key);
		} else {
			permissions.put(key, perm);
		}

//
//		
//		if (!perm.containsKey(action.toString())) {
//			throw new EntryNotFoundException(String.format(NOT_FOUND,
//					action.toString(), resId, roleId));
//		}
//		perm.remove(action.toString());
	}

	/**
	 * The number of specified permissions.
	 *
	 * @return The number of permissions in the registry.
	 */
	int size() {
		return permissions.size();
	}

	private String makeKey(AclEntry role, AclEntry resource) {
		String aro;
		String aco;

		if (role == null) {
			aro = "*";
		} else {
			aro = role.getId();
		}
		if (resource == null) {
			aco = "*";
		} else {
			aco = resource.getId();
		}

		return makeKey(aro, aco);
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
