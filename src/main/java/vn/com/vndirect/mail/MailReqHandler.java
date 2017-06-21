package vn.com.vndirect.mail;

import com.fizzed.rocker.*;
import org.rapidoid.http.Req;
import org.rapidoid.http.ReqHandler;
import org.rapidoid.job.Jobs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.com.vndirect.pool.ObjectFactory;
import vn.com.vndirect.pool.ObjectPool;
import vn.com.vndirect.pool.PartitionPool;
import vn.com.vndirect.pool.PoolConfig;
import vn.com.vndirect.util.ConfigUtils;
import vn.com.vndirect.util.Response;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;

/**
 * Created by naruto on 6/7/17.
 */
public class MailReqHandler implements ReqHandler {
    private static final Logger logger = LoggerFactory.getLogger(MailReqHandler.class);
    static final Path templatesDir = Paths.get("templates");
    static final String templateFile = "temp.rocker.html";
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
                    emailTemp.bind(entry.getKey(), entry.getValue());
                }
            }
            String content = emailTemp.render().toString();
            req.async();
            Jobs.execute(new MailSender(session, pool, user, password, fromAddress, to, cc, bc, subject, content, template, req));
        } catch (RenderingException e) {
            return Response.bad(req, "missing template field");
        } catch (TemplateNotFoundException e) {
            logger.error("invalid template '{}'", template, e);
            return Response.bad(req, "invalid template: " + template);
        } catch (TemplateBindException e) {
            return Response.bad(req, e.getMessage());
        } catch (Exception e) {
            logger.error("template error for sending email '{}' to {}", subject, to, e);
            return Response.err(req, e);
        }
        return req;
    }
}
