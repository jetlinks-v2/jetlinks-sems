package org.jetlinks.project.busi.iot;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;

/**
 * @author hsy
 * @date 2022/10/26 14:17
 * @ClassName HttpRequest
 */
public interface HttpRequest extends Closeable {

    Response get() throws IOException;

    Response post() throws IOException;

    Response put() throws IOException;

    Response delete() throws IOException;

    Response patch() throws IOException;

    HttpRequest encode(String encode);

    HttpRequest contentType(String type);

    HttpRequest param(String name, String value);

    HttpRequest params(Map<String, Object> params);

    HttpRequest header(String name, String value);

    HttpRequest headers(Map<String, String> headers);

    HttpRequest requestBody(String body);

    HttpRequest resultAsJsonString();
}
