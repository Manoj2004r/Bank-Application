/**
 * Thrown when a withdrawal or transfer would violate an account's
 * balance/overdraft rules.
 */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
