package com.rojakcoder.archly;

import java.util.List;
import java.util.Map;

import com.rojakcoder.archly.exceptions.DuplicateEntryException;
import com.rojakcoder.archly.exceptions.EntryNotFoundException;
import com.rojakcoder.archly.exceptions.NonEmptyException;

/**
 * The public class for managing permissions and roles.
 */
public class Acl {
	private static final String NON_EMPTY = "%s registry is not empty.";

	private static Acl instance = null;

	private Permission perms;

	private Registry resources;

	private Registry roles;

	private Acl() {
		roles = RoleRegistry.getSingleton();
		perms = Permission.getSingleton();
		resources = ResourceRegistry.getSingleton();
	}

	/**
	 * Gets the singleton instance of Acl.
	 *
	 * @return The singleton instance.
	 */
	public static Acl getInstance() {
		if (instance == null) {
			instance = new Acl();
		}

		return instance;
	}

	/**
	 * Adds a resource to the registry.
	 *
	 * @param resource The resource to add.
	 * @throws DuplicateEntryException Re-throws DuplicateEntryException from
	 * the resource registry.
	 */
	public void addResource(AclEntry resource) throws DuplicateEntryException {
		resources.add(resource.getId());
	}

	/**
	 * Adds a resource under a parent resource to the registry.
	 *
	 * @param resource The resource to add.
	 * @param parent The resource under which the new resource is added.
	 * @throws DuplicateEntryException Re-throws DuplicateEntryException from
	 * the resource registry.
	 */
	public void addResource(AclEntry resource, AclEntry parent)
			throws DuplicateEntryException {
		resources.add(resource.getId(), parent.getId());
	}

	/**
	 * Adds a role to the registry.
	 *
	 * @param role The role to add.
	 * @throws DuplicateEntryException Re-throws DuplicateEntryException from
	 * the role registry.
	 */
	public void addRole(AclEntry role) throws DuplicateEntryException {
		roles.add(role.getId());
	}

	/**
	 * Adds a role under a parent role to the registry.
	 *
	 * @param role The role to add.
	 * @param parent The role under which the new role is added.
	 * @throws DuplicateEntryException Re-throws DuplicateEntryException from
	 * the role registry.
	 */
	public void addRole(AclEntry role, AclEntry parent)
			throws DuplicateEntryException {
		roles.add(role.getId(), parent.getId());
	}

	/**
	 * Grants permission on all resources to the role.
	 *
	 * @param role The role to grant the permissions to.
	 */
	public void allowAllResource(AclEntry role) {
		try {
			roles.add(role.getId());
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		perms.allow(role.getId(), new RootEntry().getId());
	}

	/**
	 * Grants permission on the resource to all roles.
	 *
	 * @param resource The resource to grant the permissions on.
	 */
	public void allowAllRole(AclEntry resource) {
		try {
			resources.add(resource.getId());
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		perms.allow(new RootEntry().getId(), resource.getId());
	}

	/**
	 * Grants permission on the resource to the role.
	 *
	 * @param role The role to grant the permissions to.
	 * @param resource The resource to grant the permissions on.
	 */
	public void allow(AclEntry role, AclEntry resource) {
		try {
			roles.add(role.getId());
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		try {
			resources.add(resource.getId());
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		perms.allow(role.getId(), resource.getId());
	}

	/**
	 * Grants permission on the resource to the role for an action type.
	 *
	 * @param role The role to grant the permissions to.
	 * @param resource The resource to grant the permissions on.
	 * @param action The action type which the grant acts on. Accepted values
	 * are: "ALL", "CREATE", "UPDATE", "DELETE".
	 */
	public void allow(AclEntry role, AclEntry resource, String action) {
		Permission.Types actionType = Permission.Types.valueOf(action);

		try {
			roles.add(role.getId());
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		try {
			resources.add(resource.getId());
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		perms.allow(role.getId(), resource.getId(), actionType);
	}

	/**
	 * Resets all the registries to an empty state.
	 */
	public void clear() {
		perms.clear();
		resources.clear();
		roles.clear();
	}

	/**
	 * Denies permission on all resources to the role.
	 *
	 * @param role The role to deny the permissions to.
	 */
	public void denyAllResource(AclEntry role) {
		try {
			roles.add(role.getId());
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		perms.deny(role.getId(), new RootEntry().getId());
	}

	/**
	 * Denies permission on the resource to all roles.
	 *
	 * @param resource The resource to deny the permissions on.
	 */
	public void denyAllRole(AclEntry resource) {
		try {
			resources.add(resource.getId());
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		perms.deny(new RootEntry().getId(), resource.getId());
	}

	/**
	 * Denies permission on the resource to the role.
	 *
	 * @param role The role to deny the permissions to.
	 * @param resource The resource to deny the permissions on.
	 */
	public void deny(AclEntry role, AclEntry resource) {
		try {
			roles.add(role.getId());
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		try {
			resources.add(resource.getId());
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		perms.deny(role.getId(), resource.getId());
	}

	/**
	 * Denies permission on the resource to the role for an action type.
	 *
	 * @param role The role to deny the permissions to.
	 * @param resource The resource to deny the permissions on.
	 * @param action The action type which the deny acts on. Accepted values
	 * are: "ALL", "CREATE", "UPDATE", "DELETE".
	 */
	public void deny(AclEntry role, AclEntry resource, String action) {
		Permission.Types actionType = Permission.Types.valueOf(action);

		try {
			roles.add(role.getId());
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		try {
			resources.add(resource.getId());
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		perms.deny(role.getId(), resource.getId(), actionType);
	}

	/**
	 * Exports a snapshot of the resources registry.
	 *
	 * @return A HashMap typically meant for persistent storage.
	 */
	public Map<String, String> exportResources() {
		return this.resources.export();
	}

	/**
	 * Exports a snapshot of the roles registry.
	 *
	 * @return A HashMap typically meant for persistent storage.
	 */
	public Map<String, String> exportRoles() {
		return this.roles.export();
	}

	/**
	 * Imports a new hierarchy of resources.
	 *
	 * @param roles The map containing the new hierarchy.
	 */
	public void importResources(Map<String, String> resources) {
		if (this.resources.size() != 0) {
			throw new NonEmptyException(String.format(NON_EMPTY, "Resource"));
		}
		this.resources.importRegistry(resources);
	}

	/**
	 * Imports a new hierarchy of roles.
	 *
	 * @param roles The map containing the new hierarchy.
	 */
	public void importRoles(Map<String, String> roles) {
		if (this.roles.size() != 0) {
			throw new NonEmptyException(String.format(NON_EMPTY, "Role"));
		}
		this.roles.importRegistry(roles);
	}

	/**
	 * Determines if the role has access to the resource.
	 *
	 * @param role The access request object.
	 * @param resource The access control object.
	 * @return Returns true if the role has access to the resource, false
	 * otherwise.
	 */
	public boolean isAllowed(AclEntry role, AclEntry resource) {
		String rol = role == null ? null : role.getId();
		String res = resource == null ? null : resource.getId();

		//get the traversal path for role
		List<String> rolePath = roles.traverseRoot(rol);

		//get the traversal path for resource
		List<String> resPath = resources.traverseRoot(res);

		//check role-resource
		for (String aro: rolePath) {
			for (String aco: resPath) {
				Boolean grant = perms.isAllowed(aro, aco);

				if (grant != null) {
					return grant;
				} // else null, continue
			}
		}

		return false;
	}

	/**
	 * Determines if the role has access to the resource for the specific
	 * action.
	 *
	 * @param role The access request object.
	 * @param resource The access control object.
	 * @param action The action type to check the access for.
	 * @return Returns true if the role has access on the resource, false
	 * otherwise.
	 */
	public boolean isAllowed(AclEntry role, AclEntry resource, String action) {
		String rol = role == null ? null : role.getId();
		String res = resource == null ? null : resource.getId();

		//get the traversal path for role
		List<String> rolePath = roles.traverseRoot(rol);

		//get the traversal path for resource
		List<String> resPath = resources.traverseRoot(res);

		Permission.Types actionType = Permission.Types.valueOf(action);

		//check role-resource
		for (String aro: rolePath) {
			for (String aco: resPath) {
				Boolean grant = perms.isAllowed(aro, aco, actionType);

				if (grant != null) {
					return grant;
				} //else null, continue
			}
		}

		return false;
	}

	/**
	 * Determines if the role is denied access to the resource.
	 *
	 * @param role The access request object.
	 * @param resource The access control object.
	 * @return Returns true if the role is denied access to the resource, false
	 * otherwise.
	 */
	public boolean isDenied(AclEntry role, AclEntry resource) {
		String rol = role == null ? null : role.getId();
		String res = resource == null ? null : resource.getId();

		//get the traversal path for role
		List<String> rolePath = roles.traverseRoot(rol);

		//get the traversal path for resource
		List<String> resPath = resources.traverseRoot(res);

		//check role-resource
		for (String aro: rolePath) {
			for (String aco: resPath) {
				Boolean grant = perms.isDenied(aro, aco);

				if (grant != null) {
					return grant;
				} //else null, continue
			}
		}

		return false;
	}

	/**
	 * Determines if the role is denied access to the resource for the specific
	 * action.
	 *
	 * @param role The access request object.
	 * @param resource The access control object.
	 * @param action The action type to check the access for.
	 * @return Returns true if the role is denied access on the resource, false
	 * otherwise.
	 */
	public boolean isDenied(AclEntry role, AclEntry resource, String action) {
		String rol = role == null ? null : role.getId();
		String res = resource == null ? null : resource.getId();

		//get the traversal path for role
		List<String> rolePath = roles.traverseRoot(rol);

		//get the traversal path for resource
		List<String> resPath = resources.traverseRoot(res);

		Permission.Types actionType = Permission.Types.valueOf(action);

		//check role-resource
		for (String aro: rolePath) {
			for (String aco: resPath) {
				Boolean grant = perms.isDenied(aro, aco, actionType);

				if (grant != null) {
					return grant;
				} //else null, continue
			}
		}

		return false;
	}

	/**
	 * Makes the default permission allow.
	 */
	public void makeDefaultAllow() {
		perms.makeDefaultAllow();
	}

	/**
	 * Makes the default permission deny.
	 */
	public void makeDefaultDeny() {
		perms.makeDefaultDeny();
	}

	/**
	 * Removes the permission on the resource from the role.
	 *
	 * @param role The access request object.
	 * @param resource The access control object.
	 * @throws EntryNotFoundException Re-throws EntryNotFoundException from the
	 * permissions.
	 */
	public void remove(AclEntry role, AclEntry resource)
			throws EntryNotFoundException {
		String rol = role == null ? null : role.getId();
		String res = resource == null ? null : resource.getId();

		perms.remove(rol, res);
	}

	/**
	 * Removes the permission on the resource from the role for an action type.
	 * 
	 * @param role The access request object.
	 * @param resource The access control object.
	 * @param action
	 * @param action The action type to remove the permission from. Accepted
	 * values are: "ALL", "CREATE", "UPDATE", "DELETE".
	 * @throws EntryNotFoundException Re-throws EntryNotFoundException from the
	 * permissions.
	 */
	public void remove(AclEntry role, AclEntry resource, String action)
			throws EntryNotFoundException {
		String rol = role == null ? null : role.getId();
		String res = resource == null ? null : resource.getId();
		Permission.Types actionType = Permission.Types.valueOf(action);

		perms.remove(rol, res, actionType);
	}

	/**
	 * Removes a resource and its permissions.
	 * <p>
	 * Any permissions applicable to the resource are also removed. If the
	 * descendants are to be removed as well, the corresponding permissions are
	 * removed too.
	 * </p>
	 *
	 * @param resource The resource to remove.
	 * @param removeDescendants If true, all descendant resources of this
	 * resource are also removed.
	 */
	public void removeResource(AclEntry resource, boolean removeDescendants) {
		if (resource == null) {
			throw new RuntimeException("Cannot remove null resource");
		}

		removeResource(resource.getId(), removeDescendants);
	}

	/**
	 * Removes a resource and its permissions.
	 * <p>
	 * Any permissions applicable to the resource are also removed. If the
	 * descendants are to be removed as well, the corresponding permissions are
	 * removed too.
	 * </p>
	 *
	 * @param resourceId The ID of the resource to remove.
	 * @param removeDescendants If true, all descendant resources of this
	 * resource also removed.
	 */
	public void removeResource(String resourceId, boolean removeDescendants) {
		if (resourceId == null) {
			throw new RuntimeException("Cannot remove null resource");
		}

		List<String> res = this.resources.remove(resourceId, removeDescendants);
		for (String r: res) {
			this.perms.removeByResource(r);
		}
	}

	/**
	 * Removes a role and its permissions.
	 * <p>
	 * Any permissions applicable to the role are also removed. If the
	 * descendants are to be removed as well, the corresponding permissions are
	 * removed too.
	 * </p>
	 *
	 * @param role The role to remove.
	 * @param removeDescendants If true, all descendant resources of this
	 * resource are also removed.
	 */
	public void removeRole(AclEntry role, boolean removeDescendants) {
		if (role == null) {
			throw new RuntimeException("Cannot remove null role");
		}

		removeRole(role.getId(), removeDescendants);
	}

	/**
	 * Removes a role and its permissions.
	 * <p>
	 * Any permissions applicable to the role are also removed. If the
	 * descendants are to be removed as well, the corresponding permissions are
	 * removed too.
	 * </p>
	 *
	 * @param roleId The ID of the role to remove.
	 * @param removeDescendants If true, all descendant resources of this
	 * resource are also removed.
	 */
	public void removeRole(String roleId, boolean removeDescendants) {
		if (roleId == null) {
			throw new RuntimeException("Cannot remove null role");
		}

		List<String> rol = this.roles.remove(roleId, removeDescendants);
		for (String r: rol) {
			this.perms.removeByRole(r);
		}
	}
}

final class RootEntry implements AclEntry {
	@Override
	public String getId() {
		return "*";
	}

	@Override
	public String getEntryDescription() {
		return "ROOT";
	}

	@Override
	public AclEntry retrieveEntry(String resourceId) {
		return null;
	}
}
