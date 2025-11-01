package org.example;

import java.util.Scanner;
import java.sql.*;

public class Main {
    public static void main(String[] args) {
        BankService service = new BankService();
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("SIMPLE BANKING SYSTEM");
            System.out.println("----------------------");
            System.out.println("1. Create Account");
            System.out.println("2. Delete Account");
            System.out.println("3. Deposit Money");
            System.out.println("4. Withdraw Money");
            System.out.println("5. Transfer Funds");
            System.out.println("6. View Account / Balance");
            System.out.println("7. Mini Statement");
            System.out.println("8. Exit");
            System.out.print("Choose your option: ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1":
                    service.createAccount();
                    break;
                case "2":
                    service.deleteAccount();
                    break;
                case "3":
                    service.deposit();
                    break;
                case "4":
                    service.withdraw();
                    break;
                case "5":
                    service.transfer();
                    break;
                case "6":
                    service.viewAccount();
                    break;
                case "7":
                    service.miniStatement();
                    break;
                case "8":
                    System.out.println("Thank you for banking with us. Goodbye!");
// Bold Magenta
                    sc.close();
                    System.exit(0);
                default:
                    System.out.println("Invalid choice! Please select a number from 1 to 8.");
                    // Bold Red
            }
        }
    }
}