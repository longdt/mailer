package vn.com.vndirect.mail;

import com.fizzed.rocker.*;
import org.rapidoid.http.Req;
import org.rapidoid.http.ReqHandler;
import org.rapidoid.job.Jobs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.com.vndirect.pool.ObjectFactory;
import vn.com.vndirect.pool.ObjectPool;
import vn.com.vndirect.pool.PartitionObjectPool;
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

    public MailReqHandler(Properties conf) throws AddressException {
        user = conf.getProperty(MailerService.MAIL_USER);
        password = conf.getProperty(MailerService.MAIL_PWD);
        session = Session.getInstance(conf);
        String from = conf.getProperty(MailerService.MAIL_FROM);
        fromAddress = new InternetAddress(from == null ? user : from);
        pool = createPool(conf, session);
    }

    private ObjectPool<Transport> createPool(Properties conf, Session session) {
        PoolConfig config = new PoolConfig();
        int partition = ConfigUtils.getInt(conf, MailerService.MAILER_POOL_PARTITION, 8);
        int maxSize = ConfigUtils.getInt(conf, MailerService.MAILER_POOL_MAXSIZE, 10);
        int minSize = ConfigUtils.getInt(conf, MailerService.MAILER_POOL_MINSIZE, 5);
        config.setPartitionSize(partition);
        config.setMaxSize(maxSize);
        config.setMinSize(minSize);
        config.setScavengeIntervalMilliseconds(0);
        ObjectPool<Transport> pool = new PartitionObjectPool<>(config, new ObjectFactory<Transport>() {
            @Override
            public Transport create() {
                try {
                    Transport transport = session.getTransport("smtp");
                    return transport;
                } catch (NoSuchProviderException e) {
                    logger.error("can't create a connected Transport object", e);
                    return null;
                }
            }

            @Override
            public void destroy(Transport transport) {
                try {
                    transport.close();
                } catch (MessagingException e) {
                    logger.error("can't close transport object: {}", transport, e);
                }
            }

            @Override
            public boolean validate(Transport transport) {
                return true;
            }
        });
        return pool;
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
            logger.error("can't build email content", e);
            return Response.bad(req, "invalid template: " + template);
        } catch(TemplateBindException e) {
            return Response.bad(req, e.getMessage());
        } catch (Exception e) {
            logger.error("template error", e);
            return Response.err(req, e);
        }
        return req;
    }
}
