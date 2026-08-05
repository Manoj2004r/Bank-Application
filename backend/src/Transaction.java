import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Immutable record of a single transaction on an account.
 */
public class Transaction implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String type;          // DEPOSIT, WITHDRAW, TRANSFER_OUT, TRANSFER_IN, INTEREST, OPEN
    private final double amount;
    private final double balanceAfter;
    private final LocalDateTime timestamp;

    public Transaction(String type, double amount, double balanceAfter) {
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.timestamp = LocalDateTime.now();
    }

    public String getType() {
        return type;
    }

    public double getAmount() {
        return amount;
    }

    public double getBalanceAfter() {
        return balanceAfter;
    }

    public String getTimestampFormatted() {
        return timestamp.format(FMT);
    }

    @Override
    public String toString() {
        return String.format("%s | %-12s | %10.2f | Balance after: %10.2f",
                getTimestampFormatted(), type, amount, balanceAfter);
    }
}
