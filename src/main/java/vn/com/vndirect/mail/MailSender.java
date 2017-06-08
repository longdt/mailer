package vn.com.vndirect.mail;

import org.rapidoid.http.Req;
import vn.com.vndirect.pool.ObjectPool;
import vn.com.vndirect.pool.Poolable;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.event.ConnectionAdapter;
import javax.mail.event.ConnectionEvent;
import javax.mail.event.ConnectionListener;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by naruto on 6/6/17.
 */
public class MailSender implements Runnable {
    private Session session;
    private ObjectPool<Transport> pool;
    private String user;
    private String password;
    private Address fromAddress;
    private String toAddress;
    private String subject;
    private String htmlContent;
    private String imgPath;
    private Req req;

    public MailSender(Session session, ObjectPool<Transport> pool, String user, String password, Address fromAddress, String toAddress, String subject, String htmlContent, String imgPath, Req req) {
        this.session = session;
        this.pool = pool;
        this.user = user;
        this.password = password;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
        this.subject = subject;
        this.htmlContent = htmlContent;
        this.imgPath = imgPath;
        this.req = req;
    }

    Message buildMessage() throws MessagingException, IOException {
        Message message = new MimeMessage(session);
        message.setFrom(fromAddress);
        message.setRecipients(javax.mail.Message.RecipientType.TO,
                InternetAddress.parse(toAddress));
        message.setSubject(subject);

        MimeMultipart content = new MimeMultipart("related");
        BodyPart textPart = new MimeBodyPart();
        textPart.setContent(htmlContent, "text/html; charset=UTF-8");

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
        Transport transport = obj != null ? obj.getObject() : session.getTransport("smtp");
        if (!transport.isConnected()) {
            transport.connect(user, password);
        }
        transport.sendMessage(message, message.getAllRecipients());
        if (obj != null) {
            pool.returnObject(obj);
        }
    }

    @Override
    public void run() {
        try {
            Message message = buildMessage();
            send(message);
            Response.ok(req);
        } catch (Exception e) {
            e.printStackTrace();
            Response.err(req, e);
        } finally {
            req.done();
        }
    }
}
