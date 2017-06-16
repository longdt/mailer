package vn.com.vndirect.util;

import org.rapidoid.http.Req;
import org.rapidoid.http.Resp;

/**
 * Created by naruto on 6/5/17.
 */

public class Response {
    private static final byte[] EMPTY = new byte[0];
    private String code;
    private String message;

    public Response(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public static Resp err(Req req, Throwable e) {
        return req.response().code(500).result(new Response("500", e.getMessage()));
    }

    public static Resp bad(Req req, String msg) {
        return req.response().code(400).result(new Response("400", msg));
    }

    public static Resp ok(Req req) {
        return req.response().body(EMPTY);
    }
}
