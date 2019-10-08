import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

public class SendMail {

    public static void main(String[] args) {

        final String username = "name@myhost.com";
        final String password = "password";

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", "some.mailhost.com");
        props.put("mail.smtp.port", "25");

        Session session = Session.getInstance(props, new javax.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse("some.user@some.host.com"));
            message.setSubject("Testing Subject");
            message.setText("This is the body of the email...");

            Transport.send(message);

            System.out.println("Mail successfully sent.");

        } catch (MessagingException e) {
            throw new RuntimeException(e);
        }
    }
}
 
/*
Output:
        To compile:
        $ java -cp .:/path/javax.mail.jar SendMail

        To run:
        $ java -cp .:/path/javax.mail.jar SendMail
        Mail successfully sent.*/
