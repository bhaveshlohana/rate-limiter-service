package com.bhavesh.learn.ratelimiter.starter.aspect;

import com.bhavesh.learn.ratelimiter.starter.annotation.RateLimit;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitRequest;
import com.bhavesh.learn.ratelimiter.core.model.RateLimitResponse;
import com.bhavesh.learn.ratelimiter.core.service.RateLimiterFactory;
import com.bhavesh.learn.ratelimiter.starter.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class RateLimitAspect {

    private final RateLimiterFactory rateLimiterFactory;

    private final HttpServletRequest request;

    public RateLimitAspect(RateLimiterFactory rateLimiterFactory, HttpServletRequest request) {
        this.rateLimiterFactory = rateLimiterFactory;
        this.request = request;
    }

    @Around("@annotation(rateLimit)")
    public Object intercept(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        String clientId = request.getHeader(rateLimit.clientIdHeader());

        if (clientId == null || clientId.isEmpty()) {
            clientId = request.getRemoteAddr();
        }

        RateLimitRequest rateLimitRequest = RateLimitRequest.builder()
                .clientType(rateLimit.clientType())
                .clientId(clientId)
                .build();

        RateLimitResponse response = rateLimiterFactory.applyRateLimiting(rateLimitRequest);

        if (!response.isAllowed()) {
            throw new RateLimitExceededException(response.getReason());
        }

        return joinPoint.proceed();
    }
}
