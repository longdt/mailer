package vn.com.vndirect.pool;

/**
 * @author Daniel
 */
public interface ObjectFactory<T> {

    T create();

    void destroy(T t);

    boolean validate(T t);

}
