package io.aik.steins.grimoire.core.utils;

import cn.hutool.core.util.StrUtil;

import javax.servlet.http.HttpServletRequest;

/**
 * IP 工具 -anchor
 *
 * @author a I k .
 */
public final class IpUtils {

    private IpUtils() {
    }

    private static final String UNKNOWN = "unknown";

    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
    };

    /**
     * 获取客户端真实 IP
     *
     * <p>优先从代理头获取，获取不到则取 remoteAddr</p>
     *
     * @param request HttpServletRequest
     * @return IP 地址
     */
    public static String getIpAddr(HttpServletRequest request) {
        if (request == null) {
            return UNKNOWN;
        }

        for (String header : IP_HEADER_CANDIDATES) {
            String ip = request.getHeader(header);
            if (StrUtil.isNotBlank(ip) && !UNKNOWN.equalsIgnoreCase(ip)) {
                //anchor X-Forwarded-For 可能包含多个 IP，取第一个
                int index = ip.indexOf(",");
                if (index != -1) {
                    return ip.substring(0, index).trim();
                }
                return ip.trim();
            }
        }

        return request.getRemoteAddr();
    }
}
