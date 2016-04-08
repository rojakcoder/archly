package com.rojakcoder.archly.exceptions;

/**
 * NonEmptyException is for when the registry is not empty when expected.
 */
public class NonEmptyException extends RuntimeException {
	/**
	 * Auto-generated UID.
	 */
	private static final long serialVersionUID = -8017718905895276356L;

	/**
	 * Auto-generated constructor.
	 *
	 * @param message The emssage to go along with the exception.
	 */
	public NonEmptyException(String message) {
		super(message);
	}
}
