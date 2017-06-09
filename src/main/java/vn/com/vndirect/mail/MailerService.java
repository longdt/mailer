package vn.com.vndirect.mail;

import com.fizzed.rocker.runtime.RockerRuntime;
import org.rapidoid.http.ReqHandler;
import org.rapidoid.setup.On;
import vn.com.vndirect.util.ConfigUtils;

import javax.mail.internet.AddressException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by naruto on 6/5/17.
 */
public class MailerService {
    static final String MAIL_USER = "mail.user";
    static final String MAIL_PWD = "mail.pwd";
    static final String MAIL_FROM = "mail.from";
    static final String MAILER_POOL_PARTITION = "mailer.pool.partition";
    static final String MAILER_POOL_MINSIZE = "mailer.pool.minSize";
    static final String MAILER_POOL_MAXSIZE = "mailer.pool.maxSize";
    static final String MAILER_HOST = "mailer.listen.host";
    static final String MAILER_PORT = "mailer.listen.port";

    private static Properties loadConf() throws IOException {
        Properties props = new Properties();
        try (BufferedReader in = new BufferedReader(new FileReader("conf/mailer.conf"))) {
            props.load(in);
        }
        return props;
    }

    public static void main(String[] args) throws IOException, AddressException {
        RockerRuntime.getInstance().setReloading(true);
        Properties props = loadConf();
        ReqHandler mailReqHandler = new MailReqHandler(props);
        String host = props.getProperty(MAILER_HOST, "0.0.0.0");
        int port = ConfigUtils.getInt(props, MAILER_PORT, 9999);
        On.address(host).port(port);
        On.post("/").plain(mailReqHandler);
    }
}
