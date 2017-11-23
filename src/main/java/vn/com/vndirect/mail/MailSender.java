package vn.com.vndirect.mail;

import org.rapidoid.u.U;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.com.vndirect.pool.ObjectPool;
import vn.com.vndirect.pool.Poolable;
import vn.com.vndirect.util.ConfigUtils;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

/**
 * Created by naruto on 6/6/17.
 */
public class MailSender implements Callable<Void> {
    private static final Logger logger = LoggerFactory.getLogger(MailSender.class);
    private static final String DEFAULT_CONTENT_TYPE = "text/html; charset=UTF-8";
    private Session session;
    private ObjectPool<Transport> pool;
    private String user;
    private String password;
    private Address fromAddress;
    private String toAddress;
    private String ccAddress;
    private String bcAddress;
    private String subject;
    private String htmlContent;
    private String imgPath;

    public MailSender(Session session, ObjectPool<Transport> pool, String user, String password, Address fromAddress, String toAddress, String ccAddress, String bcAddress, String subject, String htmlContent, String imgPath) {
        this.session = session;
        this.pool = pool;
        this.user = user;
        this.password = password;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.ccAddress = ccAddress;
        this.bcAddress = bcAddress;
        this.subject = subject;
        this.htmlContent = htmlContent;
        this.imgPath = imgPath;
    }

    Message buildMessage() throws MessagingException, IOException {
        MimeMessage message = new MimeMessage(session);
        message.setHeader("Content-Type", DEFAULT_CONTENT_TYPE);
        message.setFrom(fromAddress);
        message.setRecipients(javax.mail.Message.RecipientType.TO,
                InternetAddress.parse(toAddress));
        if (ccAddress != null) {
            message.setRecipients(javax.mail.Message.RecipientType.CC,
                    InternetAddress.parse(ccAddress));
        }
        if (bcAddress != null) {
            message.setRecipients(javax.mail.Message.RecipientType.BCC,
                    InternetAddress.parse(bcAddress));
        }

        message.setSubject(subject, "UTF-8");

        MimeMultipart content = new MimeMultipart("related");
        BodyPart textPart = new MimeBodyPart();
        textPart.setContent(htmlContent, DEFAULT_CONTENT_TYPE);

        // add it
        content.addBodyPart(textPart);

        // second part (the image)
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(MailReqHandler.templatesDir.resolve(imgPath), p ->
                Files.isRegularFile(p) && !p.endsWith(MailReqHandler.templateFile)
        )) {
            for (Path p : stream) {
                MimeBodyPart imagePart = new MimeBodyPart();
                DataSource fds = new FileDataSource(p.toString());
                imagePart.setDataHandler(new DataHandler(fds));
                imagePart.setHeader("Content-ID", "<" + p.getFileName().toString() + ">");
//                    imagePart.setDisposition(Part.INLINE);
                content.addBodyPart(imagePart);
            }
        }
        // put everything together
        message.setContent(content);
        return message;
    }

    void send(Message message) throws MessagingException {
        message.saveChanges();
        Poolable<Transport> obj = pool.borrowObject();
        Transport transport = null;
        try {
            transport = obj != null ? obj.getObject() : session.getTransport("smtp");
            if (!transport.isConnected()) {
                transport.connect(user, password);
            }
            send(transport, message, message.getAllRecipients());
        } finally {
            if (obj != null) {
                obj.close();
            } else if (transport != null) {
                transport.close();
            }
        }
    }

    private void send(Transport transport, Message message, Address[] addresses) throws MessagingException {
        try {
            transport.sendMessage(message, addresses);
        } catch (MessagingException e) {
            resendOnError(transport, message, addresses, e);
        }
    }

    private void resendOnError(Transport transport, Message message, Address[] addresses, MessagingException e) throws MessagingException {
        if (!transport.isConnected()) {
            transport.connect(user, password);
        }

        if (!(e instanceof SendFailedException)) {
            transport.sendMessage(message, addresses);
            return;
        }

        SendFailedException sfe = (SendFailedException) e;
        addresses = sfe.getValidUnsentAddresses();
        if (!U.isEmpty(sfe.getInvalidAddresses()) || U.isEmpty(addresses)) {
            throw e;
        }

        try {
            transport.sendMessage(message, addresses);
        } catch (SendFailedException newSfe) {
            Address[] validSent = ConfigUtils.mergeArray(sfe.getValidSentAddresses(), newSfe.getValidSentAddresses());
            Address[] validUnsent = newSfe.getValidUnsentAddresses();
            Address[] invalid = ConfigUtils.mergeArray(sfe.getInvalidAddresses(), newSfe.getInvalidAddresses());
            throw new SendFailedException(newSfe.getMessage(), newSfe.getNextException(), validSent, validUnsent, invalid);
        } catch (MessagingException me) {
            e.setNextException(me);
            throw e;
        }
    }

    @Override
    public Void call() throws Exception {
        Message message = buildMessage();
        send(message);
        return null;
    }
}
