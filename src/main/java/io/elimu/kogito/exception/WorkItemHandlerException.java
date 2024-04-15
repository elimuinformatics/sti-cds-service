package io.elimu.kogito.exception;

public class WorkItemHandlerException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public WorkItemHandlerException() {
		super();
	}

	public WorkItemHandlerException(String message, Throwable cause) {
		super(message, cause);
	}

	public WorkItemHandlerException(String message) {
		super(message);
	}

	public WorkItemHandlerException(Throwable cause) {
		super(cause);
	}
}
