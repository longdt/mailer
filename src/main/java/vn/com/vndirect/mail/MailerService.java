package vn.com.vndirect.mail;

import com.fizzed.rocker.runtime.RockerRuntime;
import org.rapidoid.http.ReqHandler;
import org.rapidoid.setup.On;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.com.vndirect.pool.*;
import vn.com.vndirect.util.ConfigUtils;

import javax.mail.MessagingException;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by naruto on 6/5/17.
 */
public class MailerService {
    private static final Logger logger = LoggerFactory.getLogger(MailReqHandler.class);
    static final String MAIL_USER = "mail.user";
    static final String MAIL_PWD = "mail.pwd";
    static final String MAIL_FROM = "mail.from";
    static final String MAILER_POOL_PARTITION = "mailer.pool.partition";
    static final String MAILER_POOL_MINSIZE = "mailer.pool.minSize";
    static final String MAILER_POOL_MAXSIZE = "mailer.pool.maxSize";
    static final String MAILER_POOL_MAX_IDLE_MS = "mailer.pool.maxIdleMs";
    static final String MAILER_POOL_INTERVAL_CHECK = "mailer.pool.intervalCheck";
    static final String MAILER_HOST = "mailer.listen.host";
    static final String MAILER_PORT = "mailer.listen.port";

    private static Properties loadConf() throws IOException {
        Properties props = new Properties();
        try (BufferedReader in = new BufferedReader(new FileReader("conf/mailer.conf"))) {
            props.load(in);
        }
        return props;
    }


    public static ObjectPool<Transport> createPool(Properties conf, Session session) {
        String user = conf.getProperty(MailerService.MAIL_USER);
        String password = conf.getProperty(MailerService.MAIL_PWD);
        PoolConfig config = new PoolConfig();
        int partition = ConfigUtils.getInt(conf, MailerService.MAILER_POOL_PARTITION, 8);
        int maxSize = ConfigUtils.getInt(conf, MailerService.MAILER_POOL_MAXSIZE, 10);
        int minSize = ConfigUtils.getInt(conf, MailerService.MAILER_POOL_MINSIZE, 5);
        int maxIdleMs = ConfigUtils.getInt(conf, MailerService.MAILER_POOL_MAX_IDLE_MS, 120000);
        int intervalCheck = ConfigUtils.getInt(conf, MailerService.MAILER_POOL_INTERVAL_CHECK, 60000);
        config.setPartitionSize(partition);
        config.setMaxSize(maxSize);
        config.setMinSize(minSize);
        config.setMaxIdleMilliseconds(maxIdleMs);
        config.setScavengeIntervalMilliseconds(intervalCheck);
        ObjectPool<Transport> pool = createPool(config, new ObjectFactory<Transport>() {
            @Override
            public Transport create() {
                try {
                    return session.getTransport("smtp");
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

            @Override
            public boolean refresh(Transport transport) {
                try {
                    if (!transport.isConnected()) {
                        transport.connect(user, password);
                    }
                    return true;
                } catch (MessagingException e) {
                    return false;
                }
            }
        });
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                pool.shutdown();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));
        return pool;
    }

    public static ObjectPool<Transport> createPool(PoolConfig config, ObjectFactory factory) {
        if (config.getPartitionSize() <= 1) {
            return new ConcurrentPool<Transport>(config, factory);
        } else {
            config.setScavengeIntervalMilliseconds(0);
            return new PartitionPool<Transport>(config, factory);
        }
    }


    public static void main(String[] args) throws IOException, AddressException {
        RockerRuntime.getInstance().setReloading(true);
        Properties conf = loadConf();
        Session session = Session.getInstance(conf);
        ObjectPool<Transport> pool = createPool(conf, session);
        ReqHandler mailReqHandler = new MailReqHandler(conf, session, pool);
        String host = conf.getProperty(MAILER_HOST, "0.0.0.0");
        int port = ConfigUtils.getInt(conf, MAILER_PORT, 9999);
        On.address(host).port(port);
        On.post("/").json(mailReqHandler);
        On.post("/wlMail").json(new WLMailRequestHandler(conf, session, pool));
    }
}
