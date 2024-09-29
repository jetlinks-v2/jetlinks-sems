package org.jetlinks.pro.sems.iot.util;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * @author hsy
 * @date 2022/10/26 14:16
 * @ClassName HeaderUtils
 */
public class HeaderUtils {
    public static Map<String, String> createHeadersOfParams(Map<String, Object> params, String xClientId, String secureKey, String token) {
        //时间戳
        String xTimestamp = String.valueOf(System.currentTimeMillis());
        //将参数按ASCII排序后拼接为k1=v1&k2=v2的格式
        String paramString = new TreeMap<>(params).entrySet()
                .stream()
                .map(e -> e.getKey().concat("=").concat(String.valueOf(e.getValue())))
                .collect(Collectors.joining("&"));

        // System.out.println(paramString);

        //param+X-Timestamp+SecureKey通过MD5加密
        MessageDigest digest = DigestUtils.getMd5Digest();
        // System.out.println(paramString + xTimestamp + secureKey);
        digest.update(paramString.getBytes());
        digest.update(xTimestamp.getBytes());
        digest.update(secureKey.getBytes());
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Sign", Hex.encodeHexString(digest.digest()));
        headers.put("X-Client-Id", xClientId);
        headers.put("X-Timestamp", xTimestamp);
        headers.put("X-Access-Token", token);
        return headers;
    }


    static Map<String, String> createHeadersOfJsonString(String jsonString, String xClientId, String secureKey, String token) {
        //时间戳
        String xTimestamp = String.valueOf(System.currentTimeMillis());

        //param+X-Timestamp+SecureKey通过MD5加密
        MessageDigest digest = DigestUtils.getMd5Digest();
        // System.out.println(jsonString + xTimestamp + secureKey);
        digest.update(jsonString.getBytes());
        digest.update(xTimestamp.getBytes());
        digest.update(secureKey.getBytes());

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Sign", Hex.encodeHexString(digest.digest()));
        headers.put("X-Client-Id", xClientId);
        headers.put("X-Timestamp", xTimestamp);
        headers.put("X-Access-Token", token);
//        System.out.println(Hex.encodeHexString(digest.digest()) + "|" + xClientId + "|" + xTimestamp);
        return headers;
    }

    public static Map<String, String> createToken(String jsonString, String xClientId, String secureKey) {
        String xTimestamp = String.valueOf(System.currentTimeMillis());

        //param+X-Timestamp+SecureKey通过MD5加密
        MessageDigest digest = DigestUtils.getMd5Digest();
        // System.out.println(jsonString + xTimestamp + secureKey);
        digest.update(jsonString.getBytes());
        digest.update(xTimestamp.getBytes());
        digest.update(secureKey.getBytes());

        Map<String, String> headers = new HashMap<>();
        headers.put("X-Sign", Hex.encodeHexString(digest.digest()));
        headers.put("X-Client-Id", xClientId);
        headers.put("X-Timestamp", xTimestamp);
        return headers;
    }
}
