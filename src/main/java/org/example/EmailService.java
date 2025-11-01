package org.example;

import java.util.Properties;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;


/**
 * Utility class for sending email alerts (withdrawal, deposit, low balance).
 * Uses JavaMail (Jakarta Mail) to send messages through Gmail SMTP.
 */
public class EmailService {

    // Change these credentials to your Gmail account (use App Password, not actual password)
    private static final String FROM_EMAIL = "yourbankemail@gmail.com";  // 🔹 your sender email
    private static final String FROM_PASSWORD = "your-app-password";     // 🔹 use Gmail App Password

    private static final String SMTP_HOST = "smtp.gmail.com";
    private static final int SMTP_PORT = 587;

    /**
     * Sends an email notification to the account holder.
     *
     * @param toEmail   Recipient email address
     * @param subject   Subject line of the email
     * @param message   Email body text
     */
    public static void sendEmail(String toEmail, String subject, String message) {
        try {
            // SMTP configuration
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", SMTP_HOST);
            props.put("mail.smtp.port", String.valueOf(SMTP_PORT));

            // Create session with authentication
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(FROM_EMAIL, FROM_PASSWORD);
                }
            });

            // Build message
            Message mimeMessage = new MimeMessage(session);
            mimeMessage.setFrom(new InternetAddress(FROM_EMAIL, "🏦 Simple Bank System"));
            mimeMessage.setRecipient(Message.RecipientType.TO, new InternetAddress(toEmail));
            mimeMessage.setSubject(subject);
            mimeMessage.setText(message);

            // Send message
            Transport.send(mimeMessage);
            System.out.println("📧 Email sent successfully to " + toEmail);

        } catch (Exception e) {
            System.out.println("⚠️ Failed to send email: " + e.getMessage());
        }
    }

    /**
     * Sends a low balance alert when balance drops below a certain limit.
     *
     * @param toEmail  Account holder email
     * @param name     Account holder name
     * @param balance  Current account balance
     */
    public static void sendLowBalanceAlert(String toEmail, String name, double balance) {
        String subject = "⚠️ Low Balance Alert";
        String body = "Dear " + name + ",\n\n" +
                "Your account balance has dropped below the minimum limit.\n" +
                "Current Balance: ₹" + balance + "\n\n" +
                "Please deposit funds soon to avoid restrictions.\n\n" +
                "Regards,\nSimple Bank System";
        sendEmail(toEmail, subject, body);
    }

    /**
     * Sends transaction alert (deposit/withdrawal/transfer)
     */
    public static void sendTransactionAlert(String toEmail, String name, String type, double amount, double newBalance) {
        String subject = "💰 " + type + " Transaction Alert";
        String body = "Dear " + name + ",\n\n" +
                "Your account was just updated with the following transaction:\n" +
                "Type: " + type + "\n" +
                "Amount: ₹" + amount + "\n" +
                "New Balance: ₹" + newBalance + "\n\n" +
                "Thank you for using Simple Bank System.\n\n" +
                "Best Regards,\nSimple Bank Team";
        sendEmail(toEmail, subject, body);
    }
}
