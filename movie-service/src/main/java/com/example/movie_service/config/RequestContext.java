package com.example.movie_service.config;

import java.util.Map;
import com.example.movie_service.sharedLogic.dto.AuthInfo;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Component
public class RequestContext {
    public static final String AUTH_INFO = "authInfo";

    public AuthInfo getAuthInfo() {
        return (AuthInfo) getContextValue(AUTH_INFO);
    }

    public void setContext(Map<String, Object> context) {
        for (String key : context.keySet()) {
            RequestContextHolder.currentRequestAttributes()
                    .setAttribute(
                            key,
                            context.get(key),
                            RequestAttributes.SCOPE_REQUEST);
        }
    }

    public Object getContextValue(String key) {
        return RequestContextHolder.currentRequestAttributes()
                .getAttribute(
                        key,
                        RequestAttributes.SCOPE_REQUEST);
    }

}

