package vn.com.vndirect.mail;

import com.fizzed.rocker.runtime.RockerRuntime;
import org.rapidoid.http.ReqHandler;
import org.rapidoid.job.Jobs;
import org.rapidoid.setup.On;

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

    private static Properties loadConf() throws IOException {
        Properties props = new Properties();
        try (BufferedReader in = new BufferedReader(new FileReader("conf/mailer.conf"))) {
            props.load(in);
        }
        return props;
    }

    public static void main(String[] args) throws IOException, AddressException {
        RockerRuntime.getInstance().setReloading(true);
        ReqHandler mailReqHandler = new MailReqHandler(loadConf());
        On.address("127.0.0.1").port(9999);
        On.get("/").plain(mailReqHandler);
        On.post("/").plain(mailReqHandler);
    }
}
