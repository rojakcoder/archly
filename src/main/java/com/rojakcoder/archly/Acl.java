package com.rojakcoder.archly;

import java.util.List;

import com.rojakcoder.archly.exceptions.DuplicateEntryException;
import com.rojakcoder.archly.exceptions.EntryNotFoundException;

/**
 * The public class for managing permissions and roles.
 */
public class Acl {
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
		resources.add(resource);
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
		resources.add(resource, parent);
	}

	/**
	 * Adds a role to the registry.
	 *
	 * @param role The role to add.
	 * @throws DuplicateEntryException Re-throws DuplicateEntryException from
	 * the role registry.
	 */
	public void addRole(AclEntry role) throws DuplicateEntryException {
		roles.add(role);
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
		roles.add(role, parent);
	}

	/**
	 * Grants permission on all resources to the role.
	 *
	 * @param role The role to grant the permissions to.
	 */
	public void allowAllResource(AclEntry role) {
		try {
			roles.add(role);
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		perms.allow(role, new RootEntry());
	}

	/**
	 * Grants permission on the resource to all roles.
	 *
	 * @param resource The resource to grant the permissions on.
	 */
	public void allowAllRole(AclEntry resource) {
		try {
			resources.add(resource);
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		perms.allow(new RootEntry(), resource);
	}

	/**
	 * Grants permission on the resource to the role.
	 *
	 * @param role The role to grant the permissions to.
	 * @param resource The resource to grant the permissions on.
	 */
	public void allow(AclEntry role, AclEntry resource) {
		try {
			roles.add(role);
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		try {
			resources.add(resource);
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		perms.allow(role, resource);
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
			roles.add(role);
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		try {
			resources.add(resource);
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		perms.allow(role, resource, actionType);
	}

	/**
	 * Denies permission on all resources to the role.
	 *
	 * @param role The role to deny the permissions to.
	 */
	public void denyAllResource(AclEntry role) {
		try {
			roles.add(role);
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		perms.deny(role, new RootEntry());
	}

	/**
	 * Denies permission on the resource to all roles.
	 *
	 * @param resource The resource to deny the permissions on.
	 */
	public void denyAllRole(AclEntry resource) {
		try {
			resources.add(resource);
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		perms.deny(new RootEntry(), resource);
	}

	/**
	 * Denies permission on the resource to the role.
	 *
	 * @param role The role to deny the permissions to.
	 * @param resource The resource to deny the permissions on.
	 */
	public void deny(AclEntry role, AclEntry resource) {
		try {
			roles.add(role);
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		try {
			resources.add(resource);
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		perms.deny(role, resource);
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
			roles.add(role);
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		try {
			resources.add(resource);
		} catch (DuplicateEntryException e) {
			//do nothing
		}
		perms.deny(role, resource, actionType);
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
		//get the traversal path for role
		List<String> rolePath = roles.traverseRoot(role);

		//get the traversal path for resource
		List<String> resPath = resources.traverseRoot(resource);

		//check role-resource
		for (String aro: rolePath) {
			for (String aco: resPath) {
				if (perms.isAllowed(aro, aco)) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Determines if the role has access to the resource.
	 *
	 * @param role The access request object.
	 * @param resource The access control object.
	 * @param action The action type to check the access for.
	 * @return Returns true if the role has access on the resource, false
	 * otherwise.
	 */
	public boolean isAllowed(AclEntry role, AclEntry resource, String action) {
		//get the traversal path for role
		List<String> rolePath = roles.traverseRoot(role);

		//get the traversal path for resource
		List<String> resPath = resources.traverseRoot(resource);

		Permission.Types actionType = Permission.Types.valueOf(action);

		//check role-resource
		for (String aro: rolePath) {
			for (String aco: resPath) {
				if (perms.isAllowed(aro, aco, actionType)) {
					return true;
				}
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
		//get the traversal path for role
		List<String> rolePath = roles.traverseRoot(role);
//
//		System.out.printf(" ** rolePath-");
//		for (String aro: rolePath) {
//			System.out.printf("> %s ", aro);
//		}
//		System.out.println();

		//get the traversal path for resource
		List<String> resPath = resources.traverseRoot(resource);

		//check role-resource
		for (String aro: rolePath) {
			for (String aco: resPath) {
//				System.out.printf(" > %s(aro) %s(aco) => %s\n", aro, aco,
//						perms.isDenied(aro, aco));
				if (perms.isDenied(aro, aco)) {
					return true;
				}
			}
		}

		return false;
	}

	public boolean isDenied(AclEntry role, AclEntry resource, String action) {
		//get the traversal path for role
		List<String> rolePath = roles.traverseRoot(role);

		//get the traversal path for resource
		List<String> resPath = resources.traverseRoot(resource);

		Permission.Types actionType = Permission.Types.valueOf(action);

		//check role-resource
		for (String aro: rolePath) {
			for (String aco: resPath) {
				if (perms.isDenied(aro, aco, actionType)) {
					return true;
				}
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
		perms.remove(role, resource);
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
		Permission.Types actionType = Permission.Types.valueOf(action);

		perms.remove(role, resource, actionType);
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
