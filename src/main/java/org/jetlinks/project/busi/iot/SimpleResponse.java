package org.jetlinks.project.busi.iot;

import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * @author hsy
 * @date 2022/10/26 14:24
 * @ClassName SimpleResponse
 */
public class SimpleResponse implements Response {
    HttpResponse response;

    public SimpleResponse(HttpResponse response) {
        this.response = response;
    }

    @Override
    public byte[] asBytes() throws IOException {
        return EntityUtils.toByteArray(response.getEntity());
    }
}
