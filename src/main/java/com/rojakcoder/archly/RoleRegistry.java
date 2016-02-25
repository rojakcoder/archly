package com.rojakcoder.archly;

/**
 * RoleRegistry keeps track of all registered roles/users.
 */
class RoleRegistry extends Registry {
	/**
	 * The singleton instance of RoleRegistry.
	 */
	private static RoleRegistry instance;

	private RoleRegistry() {
	}

	/**
	 * Gets the singleton instance of RoleRegistry.
	 *
	 * @return The singleton instance.
	 */
	static RoleRegistry getSingleton() {
		if (instance == null) {
			instance = new RoleRegistry();
		}

		return instance;
	}
}
