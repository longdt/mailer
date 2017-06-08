package vn.com.vndirect.mail;

import com.fizzed.rocker.BindableRockerModel;
import com.fizzed.rocker.RenderingException;
import com.fizzed.rocker.Rocker;
import com.fizzed.rocker.TemplateNotFoundException;
import org.rapidoid.http.Req;
import org.rapidoid.http.ReqHandler;
import org.rapidoid.job.Jobs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.com.vndirect.pool.ObjectFactory;
import vn.com.vndirect.pool.ObjectPool;
import vn.com.vndirect.pool.PoolConfig;

import javax.mail.*;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by naruto on 6/7/17.
 */
public class MailReqHandler implements ReqHandler {
    private static final Logger logger = LoggerFactory.getLogger(MailerService.class);
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
        pool = createPool();
    }

    private ObjectPool<Transport> createPool() {
        PoolConfig config = new PoolConfig();
        config.setPartitionSize(5);
        config.setMaxSize(10);
        config.setMinSize(5);
        config.setMaxIdleMilliseconds(60 * 1000 * 5);
        ObjectPool<Transport> pool = new ObjectPool<>(config, new ObjectFactory<Transport>() {
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
        Map<String, Object> data = new HashMap<>(req.data());
        String to = (String) data.remove("to");
        if (to == null) {
            return Response.bad(req, "missing field to");
        }
        String subject = (String) data.remove("subject");
        if (subject == null) {
            return Response.bad(req, "missing field subject");
        }
        String template = (String) data.remove("template");
        if (template == null) {
            return Response.bad(req, "missing field template");
        }
        try {
            BindableRockerModel emailTemp = Rocker.template(template + "/" + templateFile);
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                emailTemp.bind(entry.getKey(), entry.getValue());
            }
            String content = emailTemp.render().toString();
            req.async();
            Jobs.execute(new MailSender(session, user, password, fromAddress, to, subject, content, template, req));
        } catch (RenderingException e) {
            return Response.bad(req, "missing template field");
        } catch (TemplateNotFoundException e) {
            logger.error("can't build email content", e);
            return Response.bad(req, "invalid template");
        } catch (Exception e) {
            logger.error("template error", e);
            return Response.err(req, e);
        }
        return req;
    }
}
