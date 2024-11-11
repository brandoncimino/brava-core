package brava.core.exceptions;

/**
 * Indicates code that hasn't been written yet.
 */
@SuppressWarnings("unused")
public final class NotImplementedException extends RuntimeException {
    public NotImplementedException() {
    }

    public NotImplementedException(String message) {
        super(message);
    }

    public NotImplementedException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotImplementedException(Throwable cause) {
        super(cause);
    }
}
