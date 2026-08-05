import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Central bank engine: owns all accounts, generates account numbers,
 * enforces transfer rules, and handles save/load to disk.
 */
public class Bank implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Map<String, Account> accounts = new HashMap<>();
    private final AtomicInteger nextAccountNumber = new AtomicInteger(100000);

    public Account openSavingsAccount(String holderName, String pin, double openingBalance) {
        String accNum = generateAccountNumber();
        Account acc = new SavingsAccount(accNum, holderName, pin, openingBalance);
        accounts.put(accNum, acc);
        return acc;
    }

    public Account openCurrentAccount(String holderName, String pin, double openingBalance) {
        String accNum = generateAccountNumber();
        Account acc = new CurrentAccount(accNum, holderName, pin, openingBalance);
        accounts.put(accNum, acc);
        return acc;
    }

    private String generateAccountNumber() {
        return "ACC" + nextAccountNumber.getAndIncrement();
    }

    public Account getAccount(String accountNumber) {
        return accounts.get(accountNumber);
    }

    public boolean accountExists(String accountNumber) {
        return accounts.containsKey(accountNumber);
    }

    public void closeAccount(String accountNumber) {
        accounts.remove(accountNumber);
    }

    public Collection<Account> allAccounts() {
        return accounts.values();
    }

    /**
     * Transfers money between two accounts atomically: either both
     * legs succeed or neither does.
     */
    public void transfer(String fromAccNum, String toAccNum, double amount) {
        Account from = accounts.get(fromAccNum);
        Account to = accounts.get(toAccNum);
        if (from == null) throw new NoSuchAccountException("Source account not found: " + fromAccNum);
        if (to == null) throw new NoSuchAccountException("Destination account not found: " + toAccNum);
        if (from == to) throw new IllegalArgumentException("Cannot transfer to the same account.");

        // Use the source account's own withdrawal rules (overdraft/min balance).
        from.withdraw(amount, "TRANSFER_OUT");
        to.deposit(amount, "TRANSFER_IN");
    }

    /** Runs each account's monthly schedule (interest or fees). */
    public void runMonthEnd() {
        for (Account acc : accounts.values()) {
            acc.applyMonthlySchedule();
        }
    }

    public void saveToFile(String path) throws IOException {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path))) {
            oos.writeObject(this);
        }
    }

    public static Bank loadFromFile(String path) throws IOException, ClassNotFoundException {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path))) {
            return (Bank) ois.readObject();
        }
    }
}
