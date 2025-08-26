package project1.ares.util;

import org.springframework.web.server.ServerWebExchange;
import org.springframework.stereotype.Component;

@Component
public class IpUtils {

    public String getClientIp(ServerWebExchange exchange) {
        // Primeiro tenta pelo header (se o app estiver atrás de proxy/load balancer)
        String ip = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return ip;
    }
}
