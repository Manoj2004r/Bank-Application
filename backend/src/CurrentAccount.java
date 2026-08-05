/**
 * Current (checking) account: allows overdraft up to a fixed limit,
 * no interest, small monthly maintenance fee.
 */
public class CurrentAccount extends Account {
    private static final long serialVersionUID = 1L;

    private static final double OVERDRAFT_LIMIT = 5000.0;
    private static final double MONTHLY_FEE = 25.0;

    public CurrentAccount(String accountNumber, String holderName, String pin, double openingBalance) {
        super(accountNumber, holderName, pin, openingBalance);
    }

    @Override
    protected boolean canWithdraw(double amount) {
        return balance - amount >= -OVERDRAFT_LIMIT;
    }

    @Override
    public String accountType() {
        return "CURRENT";
    }

    @Override
    public double applyMonthlySchedule() {
        balance -= MONTHLY_FEE;
        history.add(new Transaction("MAINT_FEE", MONTHLY_FEE, balance));
        return -MONTHLY_FEE;
    }
}
