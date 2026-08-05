/**
 * Thrown when an operation references an account number that doesn't exist.
 */
public class NoSuchAccountException extends RuntimeException {
    public NoSuchAccountException(String message) {
        super(message);
    }
}
