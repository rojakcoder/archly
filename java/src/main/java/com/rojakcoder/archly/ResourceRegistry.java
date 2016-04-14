package com.rojakcoder.archly;

/**
 * ResourceRegistry keeps track of all registered resources.
 */
class ResourceRegistry extends Registry {
	/**
	 * The singleton instance of ResourceRegistry.
	 */
	private static ResourceRegistry instance;

	private ResourceRegistry() {
	}

	/**
	 * Gets the singleton instance of ResourceRegistry.
	 *
	 * @return The singleton instance.
	 */
	static ResourceRegistry getSingleton() {
		if (instance == null) {
			instance = new ResourceRegistry();
		}

		return instance;
	}
}
