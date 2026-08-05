/**
 * Savings account: no overdraft, enforces a minimum balance,
 * and earns monthly interest.
 */
public class SavingsAccount extends Account {
    private static final long serialVersionUID = 1L;

    private static final double MIN_BALANCE = 500.0;
    private static final double MONTHLY_INTEREST_RATE = 0.005; // 0.5% per month

    public SavingsAccount(String accountNumber, String holderName, String pin, double openingBalance) {
        super(accountNumber, holderName, pin, openingBalance);
        if (openingBalance < MIN_BALANCE) {
            throw new IllegalArgumentException("Savings account requires a minimum opening balance of " + MIN_BALANCE);
        }
    }

    @Override
    protected boolean canWithdraw(double amount) {
        return balance - amount >= MIN_BALANCE;
    }

    @Override
    public String accountType() {
        return "SAVINGS";
    }

    @Override
    public double applyMonthlySchedule() {
        double interest = balance * MONTHLY_INTEREST_RATE;
        balance += interest;
        history.add(new Transaction("INTEREST", interest, balance));
        return interest;
    }
}
