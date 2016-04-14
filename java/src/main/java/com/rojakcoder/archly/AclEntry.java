package com.rojakcoder.archly;

/**
 * Any class implementing AclEntry may be managed as a resource or role.
 */
public interface AclEntry {
	/**
	 * Gets the ID of the resource/role for use in the Registry.
	 *
	 * @return The identifier for the resource/role.
	 */
	public String getId();

	/**
	 * Gets the description of the resource/role.
	 *
	 * @return The description for the resource/role.
	 */
	public String getEntryDescription();

	/**
	 * Retrieves an instance of the resource/role.
	 *
	 * @param id The ID of the resource/role to retrieve.
	 * @return Returns the resource/role instance.
	 */
	public AclEntry retrieveEntry(String resourceId);
}
