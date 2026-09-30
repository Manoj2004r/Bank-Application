import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Main entry point: a small REST API for the banking domain, built on
 * com.sun.net.httpserver.HttpServer so the project has zero external
 * dependencies (no Maven/Gradle download needed to build the image).
 *
 * Routes:
 *   GET    /api/health
 *   GET    /api/accounts
 *   POST   /api/accounts                        {type, holderName, pin, openingBalance}
 *   GET    /api/accounts/{accNum}/balance?pin=
 *   GET    /api/accounts/{accNum}/statement?pin=
 *   POST   /api/accounts/{accNum}/deposit        {pin, amount}
 *   POST   /api/accounts/{accNum}/withdraw       {pin, amount}
 *   DELETE /api/accounts/{accNum}?pin=
 *   POST   /api/transfer                         {fromAccountNumber, pin, toAccountNumber, amount}
 *   POST   /api/monthend
 */
public class ApiServer {
    private static final String DATA_FILE = "data/bank.dat";
    private static Bank bank;
    private static long totalRequests = 0;
    private static long totalErrors = 0;
    public static void main(String[] args) throws IOException {
        bank = loadBank();

        int port = 8080;
        String portEnv = System.getenv("PORT");
        if (portEnv != null && !portEnv.isBlank()) {
            port = Integer.parseInt(portEnv);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", ApiServer::route);
        server.setExecutor(null);
        server.start();
        System.out.println("JavaBank API listening on port " + port);
    }

    private static void route(HttpExchange ex) throws IOException {
        totalRequests++;

        String method = ex.getRequestMethod();
        String path = ex.getRequestURI().getPath();
        String query = ex.getRequestURI().getQuery();

        // CORS preflight + headers for every response
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, DELETE, OPTIONS");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        if (method.equals("OPTIONS")) {
            sendRaw(ex, 204, "");
            return;
        }

        try {
            String[] parts = path.split("/");
            // path like /api/accounts/ACC100000/deposit -> ["", "api", "accounts", "ACC100000", "deposit"]
            if (path.equals("/metrics")) {
                sendMetrics(ex);
                return;
            }
            if (path.equals("/api/health")) {
                sendJson(ex, 200, "{" + Json.str("status", "ok") + "}");
                return;
            }

            if (path.equals("/api/accounts") && method.equals("GET")) {
                handleListAccounts(ex);
                return;
            }
            if (path.equals("/api/accounts") && method.equals("POST")) {
                handleCreateAccount(ex);
                return;
            }
            if (path.equals("/api/transfer") && method.equals("POST")) {
                handleTransfer(ex);
                return;
            }
            if (path.equals("/api/monthend") && method.equals("POST")) {
                handleMonthEnd(ex);
                return;
            }
            if (parts.length == 5 && parts[1].equals("api") && parts[2].equals("accounts") && parts[4].equals("deposit") && method.equals("POST")) {
                handleDepositOrWithdraw(ex, parts[3], true);
                return;
            }
            if (parts.length == 5 && parts[1].equals("api") && parts[2].equals("accounts") && parts[4].equals("withdraw") && method.equals("POST")) {
                handleDepositOrWithdraw(ex, parts[3], false);
                return;
            }
            if (parts.length == 5 && parts[1].equals("api") && parts[2].equals("accounts") && parts[4].equals("balance") && method.equals("GET")) {
                handleBalance(ex, parts[3], query);
                return;
            }
            if (parts.length == 5 && parts[1].equals("api") && parts[2].equals("accounts") && parts[4].equals("statement") && method.equals("GET")) {
                handleStatement(ex, parts[3], query);
                return;
            }
            if (parts.length == 4 && parts[1].equals("api") && parts[2].equals("accounts") && method.equals("DELETE")) {
                handleCloseAccount(ex, parts[3], query);
                return;
            }

            sendError(ex, 404, "No such route: " + method + " " + path);
        } catch (NoSuchAccountException e) {
            sendError(ex, 404, e.getMessage());
        } catch (InsufficientFundsException e) {
            sendError(ex, 409, e.getMessage());
        } catch (IllegalArgumentException e) {
            sendError(ex, 400, e.getMessage());
        } catch (Exception e) {
            totalErrors++;
            sendError(ex, 500, "Internal error: " + e.getMessage());
        }
    }

    // ---------- handlers ----------

    private static void handleListAccounts(HttpExchange ex) throws IOException {
        StringJoiner arr = new StringJoiner(",", "[", "]");
        for (Account acc : bank.allAccounts()) {
            arr.add(accountJson(acc));
        }
        sendJson(ex, 200, arr.toString());
    }

    private static void handleCreateAccount(HttpExchange ex) throws IOException {
        Map<String, String> body = Json.parseFlatObject(readBody(ex));
        String type = require(body, "type");
        String holderName = require(body, "holderName");
        String pin = require(body, "pin");
        double opening = Double.parseDouble(require(body, "openingBalance"));

        Account acc;
        if (type.equalsIgnoreCase("SAVINGS")) {
            acc = bank.openSavingsAccount(holderName, pin, opening);
        } else if (type.equalsIgnoreCase("CURRENT")) {
            acc = bank.openCurrentAccount(holderName, pin, opening);
        } else {
            sendError(ex, 400, "type must be SAVINGS or CURRENT");
            return;
        }
        saveBank();
        sendJson(ex, 201, accountJson(acc));
    }

    private static void handleDepositOrWithdraw(HttpExchange ex, String accNum, boolean isDeposit) throws IOException {
        Account acc = authenticate(accNum, null, ex, true);
        if (acc == null) return;
        Map<String, String> body = Json.parseFlatObject(readBody(ex));
        String pin = require(body, "pin");
        double amount = Double.parseDouble(require(body, "amount"));
        if (!acc.verifyPin(pin)) {
            sendError(ex, 401, "Incorrect PIN.");
            return;
        }
        if (isDeposit) {
            acc.deposit(amount);
        } else {
            acc.withdraw(amount);
        }
        saveBank();
        sendJson(ex, 200, accountJson(acc));
    }

    private static void handleBalance(HttpExchange ex, String accNum, String query) throws IOException {
        Map<String, String> q = parseQuery(query);
        Account acc = bank.getAccount(accNum);
        if (acc == null) {
            sendError(ex, 404, "No account found with number: " + accNum);
            return;
        }
        if (!acc.verifyPin(q.getOrDefault("pin", ""))) {
            sendError(ex, 401, "Incorrect PIN.");
            return;
        }
        sendJson(ex, 200, accountJson(acc));
    }

    private static void handleStatement(HttpExchange ex, String accNum, String query) throws IOException {
        Map<String, String> q = parseQuery(query);
        Account acc = bank.getAccount(accNum);
        if (acc == null) {
            sendError(ex, 404, "No account found with number: " + accNum);
            return;
        }
        if (!acc.verifyPin(q.getOrDefault("pin", ""))) {
            sendError(ex, 401, "Incorrect PIN.");
            return;
        }
        StringJoiner txArr = new StringJoiner(",", "[", "]");
        for (Transaction t : acc.getHistory()) {
            txArr.add("{" + Json.str("type", t.getType()) + ","
                    + Json.num("amount", t.getAmount()) + ","
                    + Json.num("balanceAfter", t.getBalanceAfter()) + ","
                    + Json.str("timestamp", t.getTimestampFormatted()) + "}");
        }
        String json = "{" + Json.str("accountNumber", acc.getAccountNumber()) + ","
                + Json.str("holderName", acc.getHolderName()) + ","
                + Json.str("type", acc.accountType()) + ","
                + Json.num("balance", acc.getBalance()) + ","
                + "\"transactions\":" + txArr + "}";
        sendJson(ex, 200, json);
    }

    private static void handleTransfer(HttpExchange ex) throws IOException {
        Map<String, String> body = Json.parseFlatObject(readBody(ex));
        String fromAccNum = require(body, "fromAccountNumber");
        String toAccNum = require(body, "toAccountNumber");
        String pin = require(body, "pin");
        double amount = Double.parseDouble(require(body, "amount"));

        Account from = bank.getAccount(fromAccNum);
        if (from == null) {
            sendError(ex, 404, "Source account not found: " + fromAccNum);
            return;
        }
        if (!from.verifyPin(pin)) {
            sendError(ex, 401, "Incorrect PIN.");
            return;
        }
        bank.transfer(fromAccNum, toAccNum, amount);
        saveBank();
        sendJson(ex, 200, accountJson(bank.getAccount(fromAccNum)));
    }

    private static void handleMonthEnd(HttpExchange ex) throws IOException {
        int count = bank.allAccounts().size();
        bank.runMonthEnd();
        saveBank();
        sendJson(ex, 200, "{" + Json.str("message", "Month-end processing complete") + ","
                + Json.num("accountsProcessed", count) + "}");
    }

    private static void handleCloseAccount(HttpExchange ex, String accNum, String query) throws IOException {
        Map<String, String> q = parseQuery(query);
        Account acc = bank.getAccount(accNum);
        if (acc == null) {
            sendError(ex, 404, "No account found with number: " + accNum);
            return;
        }
        if (!acc.verifyPin(q.getOrDefault("pin", ""))) {
            sendError(ex, 401, "Incorrect PIN.");
            return;
        }
        if (acc.getBalance() != 0) {
            sendError(ex, 409, "Account has a non-zero balance. Withdraw or transfer funds before closing.");
            return;
        }
        bank.closeAccount(accNum);
        saveBank();
        sendJson(ex, 200, "{" + Json.str("message", "Account closed successfully") + "}");
    }

    // ---------- helpers ----------

    private static Account authenticate(String accNum, String pin, HttpExchange ex, boolean deferPinCheck) throws IOException {
        Account acc = bank.getAccount(accNum);
        if (acc == null) {
            sendError(ex, 404, "No account found with number: " + accNum);
            return null;
        }
        if (!deferPinCheck && (pin == null || !acc.verifyPin(pin))) {
            sendError(ex, 401, "Incorrect PIN.");
            return null;
        }
        return acc;
    }

    private static String accountJson(Account acc) {
        return "{" + Json.str("accountNumber", acc.getAccountNumber()) + ","
                + Json.str("holderName", acc.getHolderName()) + ","
                + Json.str("type", acc.accountType()) + ","
                + Json.num("balance", acc.getBalance()) + "}";
    }

    private static String require(Map<String, String> body, String key) {
        String v = body.get(key);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Missing required field: " + key);
        }
        return v;
    }

    private static Map<String, String> parseQuery(String query) {
        Map<String, String> map = new java.util.LinkedHashMap<>();
        if (query == null) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }

    private static String readBody(HttpExchange ex) throws IOException {
        InputStream is = ex.getRequestBody();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = is.read(buf)) != -1) {
            bos.write(buf, 0, n);
        }
        return bos.toString(StandardCharsets.UTF_8);
    }

    private static void sendJson(HttpExchange ex, int status, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json");
        sendRaw(ex, status, json);
    }

    private static void sendError(HttpExchange ex, int status, String message) throws IOException {
        sendJson(ex, status, "{" + Json.str("error", message) + "}");
    }

    private static void sendRaw(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            ex.getResponseBody().write(bytes);
        }
        ex.close();
    }

    private static Bank loadBank() {
        File f = new File(DATA_FILE);
        if (f.exists()) {
            try {
                return Bank.loadFromFile(DATA_FILE);
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("Could not load existing data, starting fresh. (" + e.getMessage() + ")");
            }
        }
        return new Bank();
    }

    private static synchronized void saveBank() {
        try {
            new File("data").mkdirs();
            bank.saveToFile(DATA_FILE);
        } catch (IOException e) {
            System.out.println("Warning: failed to save data: " + e.getMessage());
        }
    }

    private static void sendMetrics(HttpExchange ex) throws IOException {
    	String metrics =
            "# HELP bank_http_requests_total Total HTTP requests\n" +
            "# TYPE bank_http_requests_total counter\n" +
            "bank_http_requests_total " + totalRequests + "\n" +

            "# HELP bank_http_errors_total Total HTTP errors\n" +
            "# TYPE bank_http_errors_total counter\n" +
            "bank_http_errors_total " + totalErrors + "\n" +

            "# HELP bank_accounts_total Total bank accounts\n" +
            "# TYPE bank_accounts_total gauge\n" +
            "bank_accounts_total " + bank.allAccounts().size() + "\n";

    	ex.getResponseHeaders().set(
            "Content-Type",
            "text/plain; version=0.0.4"
    );

    byte[] bytes = metrics.getBytes(StandardCharsets.UTF_8);

    ex.sendResponseHeaders(200, bytes.length);

    try (var os = ex.getResponseBody()) {
        os.write(bytes);
    }
}
}
