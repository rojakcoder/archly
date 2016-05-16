package com.rojakcoder.archly;

/**
 * Simple implementation of the {@code AclEntry} interface.
 * <p>
 * This simplistic implementation acts as a wrapper around the ID of the
 * role/resource.
 * </p>
 */
public class SimpleEntry implements AclEntry {
	String id;

	public SimpleEntry(String id) {
		this.id = id;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public String getEntryDescription() {
		return null;
	}

	@Override
	public AclEntry retrieveEntry(String resourceId) {
		return new SimpleEntry(resourceId);
	}
}
