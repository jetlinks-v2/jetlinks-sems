package org.jetlinks.project.busi.iot;

import java.io.IOException;

/**
 * @author hsy
 * @date 2022/10/26 14:18
 * @ClassName Response
 */
public interface Response {
    byte[] asBytes() throws IOException;
}
