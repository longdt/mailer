package sample;

import javax.annotation.Generated;
import java.io.IOException;
import com.fizzed.rocker.ForIterator;
import com.fizzed.rocker.RenderingException;
import com.fizzed.rocker.RockerContent;
import com.fizzed.rocker.RockerOutput;
import com.fizzed.rocker.runtime.DefaultRockerTemplate;
import com.fizzed.rocker.runtime.PlainTextUnloadedClassLoader;

/*
 * Auto generated code to render template sample/temp.rocker.html
 * Do not edit this file. Changes will eventually be overwritten by Rocker parser!
 */
@Generated("com.fizzed.rocker.compiler.JavaGenerator") @SuppressWarnings("unused")
public class temp extends com.fizzed.rocker.runtime.DefaultRockerModel {

    static public final com.fizzed.rocker.ContentType CONTENT_TYPE = com.fizzed.rocker.ContentType.HTML;
    static public final String TEMPLATE_NAME = "temp.rocker.html";
    static public final String TEMPLATE_PACKAGE_NAME = "sample";
    static public final String HEADER_HASH = "973482035";
    static public final long MODIFIED_AT = 1496745946000L;
    static public final String[] ARGUMENT_NAMES = { "message" };

    // argument @ [1:2]
    private String message;

    public temp message(String message) {
        this.message = message;
        return this;
    }

    public String message() {
        return this.message;
    }

    static public temp template(String message) {
        return new temp()
            .message(message);
    }

    @Override
    protected DefaultRockerTemplate buildTemplate() throws RenderingException {
        // optimized for convenience (runtime auto reloading enabled if rocker.reloading=true)
        return com.fizzed.rocker.runtime.RockerRuntime.getInstance().getBootstrap().template(this.getClass(), this);
    }

    static public class Template extends com.fizzed.rocker.runtime.DefaultRockerTemplate {

        // <html>\n<body>\n<h2>Chào 
        static private final byte[] PLAIN_TEXT_0_0;
        // !</h2>\nVote thử hệ thống\n<img src=\"cid:image.jpg\">\n<img src=\"cid:cat.jpg\">\n<img src=\"cid:cat1.jpg\">\n</body>\n</html>
        static private final byte[] PLAIN_TEXT_1_0;

        static {
            PlainTextUnloadedClassLoader loader = PlainTextUnloadedClassLoader.tryLoad(temp.class.getClassLoader(), temp.class.getName() + "$PlainText", "UTF-8");
            PLAIN_TEXT_0_0 = loader.tryGet("PLAIN_TEXT_0_0");
            PLAIN_TEXT_1_0 = loader.tryGet("PLAIN_TEXT_1_0");
        }

        // argument @ [1:2]
        protected final String message;

        public Template(temp model) {
            super(model);
            __internal.setCharset("UTF-8");
            __internal.setContentType(CONTENT_TYPE);
            __internal.setTemplateName(TEMPLATE_NAME);
            __internal.setTemplatePackageName(TEMPLATE_PACKAGE_NAME);
            this.message = model.message();
        }

        @Override
        protected void __doRender() throws IOException, RenderingException {
            // PlainText @ [1:23]
            __internal.aboutToExecutePosInTemplate(1, 23);
            __internal.writeValue(PLAIN_TEXT_0_0);
            // ValueExpression @ [5:10]
            __internal.aboutToExecutePosInTemplate(5, 10);
            __internal.renderValue(message, false);
            // PlainText @ [5:18]
            __internal.aboutToExecutePosInTemplate(5, 18);
            __internal.writeValue(PLAIN_TEXT_1_0);
        }
    }

    private static class PlainText {

        static private final String PLAIN_TEXT_0_0 = "<html>\n<body>\n<h2>Ch\u00E0o ";
        static private final String PLAIN_TEXT_1_0 = "!</h2>\nVote th\u1EED h\u1EC7 th\u1ED1ng\n<img src=\"cid:image.jpg\">\n<img src=\"cid:cat.jpg\">\n<img src=\"cid:cat1.jpg\">\n</body>\n</html>";

    }

}