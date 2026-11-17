package circle.big.cirver.core;

/** Lifecycle failure :c */
public final class SystemLifecycleException extends RuntimeException {
    public SystemLifecycleException(String message, Throwable cause) {
        super(message, cause);
    }
}
