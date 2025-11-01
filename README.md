# Simple Banking System (Java Console Application)

This is a command-line banking application built with Java. It demonstrates core banking functionalities, database management using JDBC, and secure practices. The application provides a menu-driven interface for users to manage their accounts.

## Features

* **Account Management**: Create new bank accounts and delete existing ones.
* **Core Banking Operations**:
    * **Deposit**: Add funds to an account.
    * **Withdraw**: Withdraw funds with balance and PIN verification.
    * **Transfer**: Transfer funds between two accounts securely.
* **Account Enquiries**:
    * **View Account**: Check account details, including holder name, email, IFSC, and current balance.
    * **Mini Statement**: View a list of recent transactions for an account.
* **Data Persistence**: All account and transaction data is stored in a MySQL database.
* **Security**: Operations like withdrawal, transfer, and deletion require 4-digit PIN verification.
* **Input Validation**: Uses regex to validate user inputs for names, emails, and PINs.
* **Statement Export**: Users can export their mini-statement as a `.csv` file.

## Technology Stack

* **Language**: Java 17
* **Build Tool**: Apache Maven
* **Database**: MySQL
* **Database Connectivity**: JDBC (using `mysql-connector-java`)
* **Email Notifications**: Jakarta Mail (for sending alerts, though not fully implemented in `BankService`)

## Prerequisites

Before running the application, you will need:
1.  Java JDK 17 or higher
2.  Apache Maven
3.  A running MySQL server

## Setup and Installation

1.  **Clone the Repository**:
    ```bash
    git clone <your-repository-url>
    cd Bank2
    ```

2.  **Database Setup**:
    * Ensure your MySQL server is running.
    * Create the database and tables by executing the `database.sql` file. You can use a tool like MySQL Workbench or the `mysql` command-line client:
        ```bash
        mysql -u root -p < database.sql
        ```

3.  **Configure Database Connection**:
    * Open `src/main/java/org/example/DBUtil.java`.
    * Update the `USERNAME` and `PASSWORD` constants to match your MySQL credentials.
        ```java
        private static final String USERNAME = "root";
        private static final String PASSWORD = "sumit"; // <-- Change this
        ```

4.  **Build and Run**:
    * Build the project using Maven:
        ```bash
        mvn clean install
        ```
    * Run the application:
        ```bash
        mvn exec:java -Dexec.mainClass="org.example.Main"
        ```

## Application Menu

Once running, you will see the following options:
