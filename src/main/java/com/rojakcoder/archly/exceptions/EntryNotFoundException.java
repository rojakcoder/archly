/**
 * 
 */
package com.rojakcoder.archly.exceptions;

/**
 * EntryNotFoundException is for when the entry is not in the registry.
 */
public class EntryNotFoundException extends RuntimeException {
	/**
	 * Auto-generated UID.
	 */
	private static final long serialVersionUID = 8939838184155256529L;

	/**
	 * Auto-generated constructor.
	 * 
	 * @param message The message to go along with the exception.
	 */
	public EntryNotFoundException(String message) {
		super(message);
	}
}
