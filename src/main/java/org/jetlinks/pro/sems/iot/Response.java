package org.jetlinks.pro.sems.iot;

import java.io.IOException;

/**
 * @author hsy
 * @date 2022/10/26 14:18
 * @ClassName Response
 */
public interface Response {
    byte[] asBytes() throws IOException;
}
