package org.example;

public class Account {
    private String accountNumber;
    private String holderName;
    private String email;
    private String atmPin; // store as CHAR(4)
    private String ifsc;
    private double balance;

    public Account() {}

    public Account(String accountNumber, String holderName, String email, String atmPin, String ifsc, double balance) {
        this.accountNumber = accountNumber;
        this.holderName = holderName;
        this.email = email;
        this.atmPin = atmPin;
        this.ifsc = ifsc;
        this.balance = balance;
    }

    /* getters and setters */
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getHolderName() { return holderName; }
    public void setHolderName(String holderName) { this.holderName = holderName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getAtmPin() { return atmPin; }
    public void setAtmPin(String atmPin) { this.atmPin = atmPin; }
    public String getIfsc() { return ifsc; }
    public void setIfsc(String ifsc) { this.ifsc = ifsc; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }
}
