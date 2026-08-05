import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class representing a bank account.
 * Concrete account types (SavingsAccount, CurrentAccount) extend this.
 */
public abstract class Account implements Serializable {
    private static final long serialVersionUID = 1L;

    protected final String accountNumber;
    protected String holderName;
    protected String pin;
    protected double balance;
    protected final List<Transaction> history;

    public Account(String accountNumber, String holderName, String pin, double openingBalance) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.pin = pin;
        this.balance = openingBalance;
        this.history = new ArrayList<>();
        if (openingBalance > 0) {
            history.add(new Transaction("OPEN", openingBalance, openingBalance));
        }
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getHolderName() {
        return holderName;
    }

    public double getBalance() {
        return balance;
    }

    public boolean verifyPin(String candidate) {
        return this.pin.equals(candidate);
    }

    public void changePin(String newPin) {
        this.pin = newPin;
    }

    public List<Transaction> getHistory() {
        return history;
    }

    public void deposit(double amount) {
        deposit(amount, "DEPOSIT");
    }

    void deposit(double amount, String transactionType) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive.");
        }
        balance += amount;
        history.add(new Transaction(transactionType, amount, balance));
    }

    public void withdraw(double amount) {
        withdraw(amount, "WITHDRAW");
    }

    void withdraw(double amount, String transactionType) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdrawal amount must be positive.");
        }
        if (!canWithdraw(amount)) {
            throw new InsufficientFundsException(
                    "Insufficient funds: cannot withdraw " + amount + " from account " + accountNumber);
        }
        balance -= amount;
        history.add(new Transaction(transactionType, amount, balance));
    }

    protected abstract boolean canWithdraw(double amount);

    /** Short label describing the account type, e.g. "SAVINGS". */
    public abstract String accountType();

    /** Applies any periodic account-type-specific adjustment (e.g. interest). Returns amount applied. */
    public abstract double applyMonthlySchedule();

    @Override
    public String toString() {
        return String.format("[%s] %-6s | %-20s | Balance: %10.2f", accountNumber, accountType(), holderName, balance);
    }
}
