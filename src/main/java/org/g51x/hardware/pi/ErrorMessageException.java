package org.g51x.hardware.pi;

public class ErrorMessageException extends RuntimeException {

    public ErrorMessageException() {
    }

    public ErrorMessageException(String message) {
        super(message);
    }

    public ErrorMessageException(String message, Throwable cause) {
        super(message, cause);
    }

}
