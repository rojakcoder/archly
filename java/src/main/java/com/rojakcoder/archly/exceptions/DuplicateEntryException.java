package com.rojakcoder.archly.exceptions;

/**
 * DuplicateEntryException is for when the entry already exists in the registry.
 */
public class DuplicateEntryException extends RuntimeException {
	/**
	 * Auto-generated UID.
	 */
	private static final long serialVersionUID = -5069347447059863093L;

	/**
	 * Auto-generated constructor.
	 *
	 * @param message The message to go along with the exception.
	 */
	public DuplicateEntryException(String message) {
		super(message);
	}
}
