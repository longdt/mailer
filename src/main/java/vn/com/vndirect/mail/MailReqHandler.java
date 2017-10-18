package vn.com.vndirect.mail;

import com.fizzed.rocker.*;
import org.rapidoid.data.JSON;
import org.rapidoid.http.Req;
import org.rapidoid.http.ReqHandler;
import org.rapidoid.job.Jobs;
import org.rapidoid.u.U;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.com.vndirect.pool.ObjectPool;
import vn.com.vndirect.util.Response;

import javax.mail.Address;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by naruto on 6/7/17.
 */
public class MailReqHandler implements ReqHandler {
    static final Path templatesDir = Paths.get("templates");
    static final String templateFile = "temp.rocker.html";
    private static final Logger logger = LoggerFactory.getLogger(MailReqHandler.class);
    private Session session;
    private String user;
    private String password;
    private Address fromAddress;
    private ObjectPool<Transport> pool;

    public MailReqHandler(Properties conf, Session session, ObjectPool<Transport> pool) throws AddressException {
        user = conf.getProperty(MailerService.MAIL_USER);
        password = conf.getProperty(MailerService.MAIL_PWD);
        this.session = session;
        String from = conf.getProperty(MailerService.MAIL_FROM);
        fromAddress = new InternetAddress(from == null ? user : from);
        this.pool = pool;
    }

    @Override
    public Object execute(Req req) throws Exception {
        Map<String, Object> data = req.posted();
        String to = (String) data.get("to");
        if (to == null) {
            return Response.bad(req, "missing field to");
        }
        String subject = (String) data.get("subject");
        if (subject == null) {
            return Response.bad(req, "missing field subject");
        }
        String template = (String) data.get("template");
        if (template == null) {
            return Response.bad(req, "missing field template");
        }
        String cc = (String) data.get("cc");
        String bc = (String) data.get("bc");
        Map<String, Object> tempFields = (Map<String, Object>) data.get("tempfields");
        try {
            BindableRockerModel emailTemp = Rocker.template(template + "/" + templateFile);
            if (tempFields != null) {
                for (Map.Entry<String, Object> entry : tempFields.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof Map || value instanceof List) {
                        value = JSON.MAPPER.valueToTree(value);
                    }
                    emailTemp.bind(entry.getKey(), value);
                }
            }
            String content = emailTemp.render().toString();

            Jobs.execute(new MailSender(session, pool, user, password, fromAddress, to, cc, bc, subject, content, template), (result, error) -> {
                if (error == null) {
                    logger.info("send success '{}' to {}", subject, to);
                } else if (error instanceof AddressException || (error instanceof SendFailedException && !U.isEmpty(((SendFailedException) error).getInvalidAddresses()))) {
                    logger.error("invalid email address when send mail '{}' to {}", subject, to, error);
                } else {
                    logger.error("can't send mail '{}' to {}", subject, to, error);
                }
            });
            logger.info("sent sendmail request '{}' to {}", subject, to);
            Response.ok(req);
        } catch (RenderingException e) {
            logger.error("missing template field of template '{}'/{} when sends '{}' to {}", template, tempFields, subject, to, e);
            return Response.bad(req, "missing template field");
        } catch (TemplateNotFoundException e) {
            logger.error("invalid template '{}' when sends '{}' to {}", template, subject, to, e);
            return Response.bad(req, "invalid template: " + template);
        } catch (TemplateBindException e) {
            logger.error("can't bind template '{}'/{} when sends '{}' to {}", template, tempFields, subject, to, e);
            return Response.bad(req, e.getMessage());
        } catch (Exception e) {
            logger.error("template '{}'/{} error when sends '{}' to {}", template, tempFields, subject, to, e);
            return Response.err(req, e);
        }
        return req;
    }
}
