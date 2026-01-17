package com.billsplit.config;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Order(1)
public class RateLimitingFilter implements Filter {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);
    
    // Rate limit configuration (requests per minute)
    @Value("${rate.limit.enabled:true}")
    private boolean enabled;
    
    @Value("${rate.limit.requests:100}")
    private int maxRequests;
    
    @Value("${rate.limit.window.minutes:1}")
    private int windowMinutes;
    
    // Store request counts per IP address
    private final Map<String, RequestCounter> requestCounts = new ConcurrentHashMap<>();
    
    // Cleanup thread to remove old entries
    private static final long CLEANUP_INTERVAL_MS = 60000; // 1 minute
    
    static {
        // Start cleanup thread
        Thread cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(CLEANUP_INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        cleanupThread.setDaemon(true);
        cleanupThread.start();
    }
    
    @Override
    public void doFilter(jakarta.servlet.ServletRequest request, jakarta.servlet.ServletResponse response, 
                         FilterChain chain) throws IOException, ServletException {
        // Skip rate limiting if disabled
        if (!enabled) {
            chain.doFilter(request, response);
            return;
        }
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Get client IP address
        String clientIp = getClientIpAddress(httpRequest);
        
        // Check rate limit
        if (isRateLimited(clientIp)) {
            logger.warn("Rate limit exceeded for IP: {}", clientIp);
            httpResponse.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Rate limit exceeded. Please try again later.\"}");
            return;
        }
        
        // Increment request count
        incrementRequestCount(clientIp);
        
        // Continue with the filter chain
        chain.doFilter(request, response);
    }
    
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
    
    private boolean isRateLimited(String clientIp) {
        RequestCounter counter = requestCounts.get(clientIp);
        if (counter == null) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long windowMs = windowMinutes * 60 * 1000L;
        
        // Check if window has expired
        if (currentTime - counter.getStartTime() > windowMs) {
            requestCounts.remove(clientIp);
            return false;
        }
        
        return counter.getCount().get() >= maxRequests;
    }
    
    private void incrementRequestCount(String clientIp) {
        requestCounts.compute(clientIp, (key, value) -> {
            if (value == null) {
                return new RequestCounter();
            }
            
            long currentTime = System.currentTimeMillis();
            long windowMs = windowMinutes * 60 * 1000L;
            
            // Reset if window has expired
            if (currentTime - value.getStartTime() > windowMs) {
                return new RequestCounter();
            }
            
            value.getCount().incrementAndGet();
            return value;
        });
    }
    
    private static class RequestCounter {
        private final AtomicInteger count;
        private final long startTime;
        
        public RequestCounter() {
            this.count = new AtomicInteger(1);
            this.startTime = System.currentTimeMillis();
        }
        
        public AtomicInteger getCount() {
            return count;
        }
        
        public long getStartTime() {
            return startTime;
        }
    }
}
