package vn.com.vndirect.mail;

import org.rapidoid.http.Req;
import org.rapidoid.http.Resp;

import java.nio.charset.StandardCharsets;

/**
 * Created by naruto on 6/5/17.
 */

public class Response {
    private static final byte[] EMPTY = new byte[0];

    public static Resp err(Req req, Throwable e) {
        return req.response().code(500).body(e.getMessage().getBytes(StandardCharsets.UTF_8));
    }

    public static Resp bad(Req req, String msg) {
        return req.response().code(400).body(msg.getBytes(StandardCharsets.UTF_8));
    }

    public static Resp ok(Req req) {
        return req.response().body(EMPTY);
    }
}
