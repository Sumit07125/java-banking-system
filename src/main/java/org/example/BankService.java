package org.example;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.*;
import java.util.*;

public class BankService {
    private final Scanner sc = new Scanner(System.in);

    // ANSI escape codes for colors
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_CYAN = "\u001B[36m";

    // validation regexes
    private static final String NAME_REGEX = "^[A-Za-z ]{2,100}$";
    private static final String EMAIL_REGEX = "^[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$";

    private static final String PIN_REGEX = "^\\d{4}$";

    // Create account
    public void createAccount() {
        try (Connection conn = DBUtil.getConnection()) {
            System.out.println(ANSI_CYAN + "--- Create Account ---" + ANSI_RESET);

            String name = inputUntilValid(ANSI_YELLOW + "Holder name (letters & spaces only): " + ANSI_RESET,
                    NAME_REGEX, ANSI_RED + "Invalid name. Use letters and spaces only, 2-100 chars." + ANSI_RESET);
            String email = inputUntilValid(ANSI_YELLOW + "Email: " + ANSI_RESET, EMAIL_REGEX,
                    ANSI_RED + "Invalid email format." + ANSI_RESET);
            String ifsc = "BANK0000001";
            String pin = inputUntilValid(ANSI_YELLOW + "Set 4-digit PIN: " + ANSI_RESET, PIN_REGEX,
                    ANSI_RED + "PIN must be 4 digits." + ANSI_RESET);

            double initialDeposit = -1;
            while (initialDeposit < 0) {
                try {
                    System.out.print(ANSI_YELLOW + "Initial deposit (>=0): " + ANSI_RESET);
                    initialDeposit = Double.parseDouble(sc.nextLine().trim());
                    if (initialDeposit < 0) {
                        System.out.println(ANSI_RED + "Amount cannot be negative." + ANSI_RESET);
                    }
                } catch (NumberFormatException ex) {
                    System.out.println(ANSI_RED + "Enter a valid number." + ANSI_RESET);
                }
            }

            // generate unique 12-digit numeric account number
            String accNo = generateUniqueAccountNumber(conn);

            String sql = "INSERT INTO accounts (account_number, holder_name, email, atm_pin, ifsc, balance) VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, accNo);
                ps.setString(2, name);
                ps.setString(3, email);
                ps.setString(4, pin);
                ps.setString(5, ifsc);
                ps.setDouble(6, initialDeposit);
                ps.executeUpdate();
            }

            // record initial deposit as transaction if > 0
            if (initialDeposit > 0) {
                recordTransaction(conn, accNo, "DEPOSIT", initialDeposit, initialDeposit, "Initial deposit");
            }

            System.out.println(ANSI_GREEN + "Account created successfully!" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "Account Number: " + accNo + ANSI_RESET);

        } catch (SQLException e) {
            System.err.println(ANSI_RED + "DB error while creating account: " + e.getMessage() + ANSI_RESET);
        }
    }

    private String inputUntilValid(String prompt, String regex, String errMsg) {
        while (true) {
            System.out.print(prompt);
            String input = sc.nextLine().trim();
            if (input.matches(regex))
                return input;
            System.out.println(errMsg);
        }
    }

    private String generateUniqueAccountNumber(Connection conn) throws SQLException {
        Random rnd = new Random();
        while (true) {
            // 12-digit number (leading digits not zero)
            long part = (long) (rnd.nextDouble() * 1_000_000_000_000L);
            String acc = String.format("%012d", part);
            // ensure it doesn't start with 0 (optional). If you want truly any 12-digit,
            // this is fine.
            if (acc.charAt(0) == '0')
                acc = "1" + acc.substring(1);

            String check = "SELECT 1 FROM accounts WHERE account_number = ?";
            try (PreparedStatement ps = conn.prepareStatement(check)) {
                ps.setString(1, acc);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        return acc;
                    }
                }
            }
        }
    }

    // Deposit
    public void deposit() {
        try (Connection conn = DBUtil.getConnection()) {
            System.out.println(ANSI_CYAN + "--- Deposit ---" + ANSI_RESET);
            System.out.print(ANSI_YELLOW + "Enter account number: " + ANSI_RESET);
            String acc = sc.nextLine().trim();
            if (!accountExists(conn, acc)) {
                System.out.println(ANSI_RED + "Account not found." + ANSI_RESET);
                return;
            }
            double amount = inputPositiveAmount(ANSI_YELLOW + "Enter deposit amount: " + ANSI_RESET);
            if (amount <= 0)
                return;

            String sql = "UPDATE accounts SET balance = balance + ? WHERE account_number = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, amount);
                ps.setString(2, acc);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    double newBal = getBalance(conn, acc);
                    recordTransaction(conn, acc, "DEPOSIT", amount, newBal, null);
                    System.out.printf(ANSI_GREEN + "Deposit successful. New balance: %.2f%n" + ANSI_RESET, newBal);
                } else {
                    System.out.println(ANSI_RED + "Deposit failed." + ANSI_RESET);
                }
            }
        } catch (SQLException e) {
            System.err.println(ANSI_RED + "DB error during deposit: " + e.getMessage() + ANSI_RESET);
        }
    }

    // Withdraw
    public void withdraw() {
        try (Connection conn = DBUtil.getConnection()) {
            System.out.println(ANSI_CYAN + "--- Withdraw ---" + ANSI_RESET);
            System.out.print(ANSI_YELLOW + "Enter account number: " + ANSI_RESET);
            String acc = sc.nextLine().trim();
            if (!accountExists(conn, acc)) {
                System.out.println(ANSI_RED + "Account not found." + ANSI_RESET);
                return;
            }
            if (!verifyPin(conn, acc)) {
                System.out.println(ANSI_RED + "PIN verification failed." + ANSI_RESET);
                return;
            }
            double amount = inputPositiveAmount(ANSI_YELLOW + "Enter amount to withdraw: " + ANSI_RESET);
            if (amount <= 0)
                return;

            double balance = getBalance(conn, acc);
            if (balance < amount) {
                System.out.println(ANSI_RED + "Insufficient funds. Current balance: " + balance + ANSI_RESET);
                return;
            }

            String sql = "UPDATE accounts SET balance = balance - ? WHERE account_number = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setDouble(1, amount);
                ps.setString(2, acc);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    double newBal = getBalance(conn, acc);
                    recordTransaction(conn, acc, "WITHDRAW", amount, newBal, null);
                    System.out.printf(ANSI_GREEN + "Withdrawal successful. New balance: %.2f%n" + ANSI_RESET, newBal);
                } else {
                    System.out.println(ANSI_RED + "Withdrawal failed." + ANSI_RESET);
                }
            }
        } catch (SQLException e) {
            System.err.println(ANSI_RED + "DB error during withdrawal: " + e.getMessage() + ANSI_RESET);
        }
    }

    // Transfer
    public void transfer() {
        try (Connection conn = DBUtil.getConnection()) {
            System.out.println(ANSI_CYAN + "--- Transfer ---" + ANSI_RESET);
            System.out.print(ANSI_YELLOW + "Enter sender account number: " + ANSI_RESET);
            String sender = sc.nextLine().trim();
            if (!accountExists(conn, sender)) {
                System.out.println(ANSI_RED + "Sender account not found." + ANSI_RESET);
                return;
            }

            // verify sender PIN
            if (!verifyPin(conn, sender)) {
                System.out.println(ANSI_RED + "Sender PIN verification failed." + ANSI_RESET);
                return;
            }

            System.out.print(ANSI_YELLOW + "Enter receiver account number: " + ANSI_RESET);
            String receiver = sc.nextLine().trim();
            if (!accountExists(conn, receiver)) {
                System.out.println(ANSI_RED + "Receiver account not found." + ANSI_RESET);
                return;
            }

            // show receiver name & IFSC
            try (PreparedStatement ps = conn
                    .prepareStatement("SELECT holder_name, ifsc FROM accounts WHERE account_number = ?")) {
                ps.setString(1, receiver);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Receiver: " + rs.getString("holder_name"));
                        System.out.println("Receiver IFSC: " + rs.getString("ifsc"));
                    }
                }
            }

            double amount = inputPositiveAmount(ANSI_YELLOW + "Enter amount to transfer: " + ANSI_RESET);
            if (amount <= 0)
                return;

            double senderBal = getBalance(conn, sender);
            if (senderBal < amount) {
                System.out.println(ANSI_RED + "Sender has insufficient funds. Balance: " + senderBal + ANSI_RESET);
                return;
            }

            // do in transaction
            try {
                conn.setAutoCommit(false);
                try (PreparedStatement dec = conn
                        .prepareStatement("UPDATE accounts SET balance = balance - ? WHERE account_number = ?");
                        PreparedStatement inc = conn.prepareStatement(
                                "UPDATE accounts SET balance = balance + ? WHERE account_number = ?")) {

                    dec.setDouble(1, amount);
                    dec.setString(2, sender);
                    inc.setDouble(1, amount);
                    inc.setString(2, receiver);

                    int r1 = dec.executeUpdate();
                    int r2 = inc.executeUpdate();
                    if (r1 > 0 && r2 > 0) {
                        double senderNew = getBalance(conn, sender);
                        double receiverNew = getBalance(conn, receiver);
                        recordTransaction(conn, sender, "TRANSFER_OUT", amount, senderNew, "To " + receiver);
                        recordTransaction(conn, receiver, "TRANSFER_IN", amount, receiverNew, "From " + sender);
                        conn.commit();
                        System.out.println(ANSI_GREEN + "Transfer completed successfully." + ANSI_RESET);
                        System.out.printf(
                                ANSI_GREEN + "Sender new balance: %.2f%nReceiver new balance: %.2f%n" + ANSI_RESET,
                                senderNew, receiverNew);
                    } else {
                        conn.rollback();
                        System.out.println(ANSI_RED + "Transfer failed; rolled back." + ANSI_RESET);
                    }
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (SQLException ex) {
                conn.rollback();
                System.err.println(ANSI_RED + "Error during transfer: " + ex.getMessage() + ANSI_RESET);
            }
        } catch (SQLException e) {
            System.err.println(ANSI_RED + "DB error during transfer: " + e.getMessage() + ANSI_RESET);
        }
    }

    // Check balance / View account details (view-only does not ask PIN)
    public void viewAccount() {
        try (Connection conn = DBUtil.getConnection()) {
            System.out.println(ANSI_CYAN + "--- View Account ---" + ANSI_RESET);
            System.out.print(ANSI_YELLOW + "Enter account number: " + ANSI_RESET);
            String acc = sc.nextLine().trim();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT holder_name,email,ifsc,balance,created_at FROM accounts WHERE account_number = ?")) {
                ps.setString(1, acc);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("Holder: " + rs.getString("holder_name"));
                        System.out.println("Email: " + rs.getString("email"));
                        System.out.println("IFSC: " + rs.getString("ifsc"));
                        System.out.printf("Balance: %.2f%n", rs.getDouble("balance"));
                        System.out.println("Created at: " + rs.getTimestamp("created_at"));
                    } else {
                        System.out.println(ANSI_RED + "Account not found." + ANSI_RESET);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println(ANSI_RED + "DB error when viewing account: " + e.getMessage() + ANSI_RESET);
        }
    }

    // Delete account
    public void deleteAccount() {
        try (Connection conn = DBUtil.getConnection()) {
            System.out.println(ANSI_CYAN + "--- Delete Account ---" + ANSI_RESET);
            System.out.print(ANSI_YELLOW + "Enter account number to delete: " + ANSI_RESET);
            String acc = sc.nextLine().trim();
            if (!accountExists(conn, acc)) {
                System.out.println(ANSI_RED + "Account not found." + ANSI_RESET);
                return;
            }
            if (!verifyPin(conn, acc)) {
                System.out.println(ANSI_RED + "PIN verification failed. Aborting." + ANSI_RESET);
                return;
            }

            System.out.print(ANSI_YELLOW + "Are you sure you want to delete this account? (yes/no): " + ANSI_RESET);
            String confirm = sc.nextLine().trim().toLowerCase();
            if (!confirm.equals("yes")) {
                System.out.println("Deletion canceled.");
                return;
            }

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM accounts WHERE account_number = ?")) {
                ps.setString(1, acc);
                int rows = ps.executeUpdate();
                if (rows > 0) {
                    System.out.println(ANSI_GREEN + "Account deleted successfully." + ANSI_RESET);
                } else {
                    System.out.println(ANSI_RED + "Deletion failed." + ANSI_RESET);
                }
            }
        } catch (SQLException e) {
            System.err.println(ANSI_RED + "DB error during delete: " + e.getMessage() + ANSI_RESET);
        }
    }

    // Mini-statement (passbook)
    public void miniStatement() {
        try (Connection conn = DBUtil.getConnection()) {
            System.out.println(ANSI_CYAN + "--- Mini Statement ---" + ANSI_RESET);
            System.out.print(ANSI_YELLOW + "Enter account number: " + ANSI_RESET);
            String acc = sc.nextLine().trim();
            if (!accountExists(conn, acc)) {
                System.out.println(ANSI_RED + "Account not found." + ANSI_RESET);
                return;
            }

            if (!verifyPin(conn, acc)) {
                System.out.println(ANSI_RED + "PIN incorrect. Aborting mini-statement." + ANSI_RESET);
                return;
            }

            List<String[]> rows = new ArrayList<>();
            String q = "SELECT created_at, transaction_type, amount, balance_after, remark FROM transactions WHERE account_number = ? ORDER BY created_at DESC";
            try (PreparedStatement ps = conn.prepareStatement(q)) {
                ps.setString(1, acc);
                try (ResultSet rs = ps.executeQuery()) {
                    System.out.printf(ANSI_CYAN + "%-20s %-15s %-12s %-12s %-25s%n" + ANSI_RESET, "Date", "Type",
                            "Amount", "Balance", "Remark");
                    while (rs.next()) {
                        Timestamp ts = rs.getTimestamp("created_at");
                        String type = rs.getString("transaction_type");
                        double amt = rs.getDouble("amount");
                        double bal = rs.getDouble("balance_after");
                        String remark = rs.getString("remark");
                        System.out.printf("%-20s %-15s %-12.2f %-12.2f %-25s%n", ts.toString(), type, amt, bal,
                                (remark == null ? "" : remark));
                        rows.add(new String[] { ts.toString(), type, String.format("%.2f", amt),
                                String.format("%.2f", bal), (remark == null ? "" : remark) });
                    }
                }
            }

            System.out
                    .print(ANSI_YELLOW + "Do you want to download this mini-statement as CSV? (yes/no): " + ANSI_RESET);
            String ans = sc.nextLine().trim().toLowerCase();
            if (ans.equals("yes")) {
                String filename = "mini_statement_" + acc + ".csv";
                try (PrintWriter pw = new PrintWriter(new FileWriter(filename))) {
                    pw.println("date,type,amount,balance,remark");
                    for (String[] r : rows) {
                        // escape commas simply by quoting
                        pw.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n", r[0], r[1], r[2], r[3],
                                r[4].replace("\"", "'"));
                    }
                }
                System.out.println(ANSI_GREEN + "CSV saved as: " + filename + ANSI_RESET);
            }

        } catch (SQLException e) {
            System.err.println(ANSI_RED + "DB error while fetching mini-statement: " + e.getMessage() + ANSI_RESET);
        } catch (Exception ex) {
            System.err.println(ANSI_RED + "Error writing CSV: " + ex.getMessage() + ANSI_RESET);
        }
    }

    // helper: check account exists
    private boolean accountExists(Connection conn, String acc) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT 1 FROM accounts WHERE account_number = ?")) {
            ps.setString(1, acc);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    // helper: get balance
    private double getBalance(Connection conn, String acc) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT balance FROM accounts WHERE account_number = ?")) {
            ps.setString(1, acc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }
        }
        return -1;
    }

    // helper: record transaction
    private void recordTransaction(Connection conn, String acc, String type, double amount, double balanceAfter,
            String remark) throws SQLException {
        String sql = "INSERT INTO transactions (account_number, transaction_type, amount, balance_after, remark) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, acc);
            ps.setString(2, type);
            ps.setDouble(3, amount);
            ps.setDouble(4, balanceAfter);
            ps.setString(5, remark);
            ps.executeUpdate();
        }
    }

    // helper: verify PIN (asks user to input)
    private boolean verifyPin(Connection conn, String acc) throws SQLException {
        System.out.print(ANSI_YELLOW + "Enter 4-digit PIN for account " + acc + ": " + ANSI_RESET);
        String pin = sc.nextLine().trim();
        if (!pin.matches(PIN_REGEX)) {
            System.out.println(ANSI_RED + "Invalid PIN format." + ANSI_RESET);
            return false;
        }
        try (PreparedStatement ps = conn.prepareStatement("SELECT atm_pin FROM accounts WHERE account_number = ?")) {
            ps.setString(1, acc);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return pin.equals(rs.getString("atm_pin"));
                }
            }
        }
        return false;
    }

    private double inputPositiveAmount(String prompt) {
        while (true) {
            try {
                System.out.print(prompt);
                double amount = Double.parseDouble(sc.nextLine().trim());
                if (amount <= 0) {
                    System.out.println(ANSI_RED + "Amount must be positive." + ANSI_RESET);
                    continue;
                }
                return amount;
            } catch (NumberFormatException ex) {
                System.out.println(ANSI_RED + "Enter a valid number." + ANSI_RESET);
            }
        }
    }
}