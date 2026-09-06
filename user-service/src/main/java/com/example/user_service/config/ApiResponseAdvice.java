package com.example.user_service.config;

import com.example.user_service.sharedLogic.dto.ApiResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import tools.jackson.databind.json.JsonMapper;

@RestControllerAdvice(basePackages = "com.example.user_service")
public class ApiResponseAdvice implements ResponseBodyAdvice<Object> {

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Override
    public boolean supports(MethodParameter returnType,
            Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
            MediaType contentType, Class<? extends HttpMessageConverter<?>> converterType,
            ServerHttpRequest request, ServerHttpResponse response) {
        if (body instanceof ApiResponse) {
            return body;
        }
        int status = response instanceof ServletServerHttpResponse servletResponse
                ? servletResponse.getServletResponse().getStatus()
                : 200;
        // These HTTP responses must not contain a body.
        if (status == 204 || status == 304 || status < 200
                || request.getMethod() == org.springframework.http.HttpMethod.HEAD) {
            return body;
        }
        boolean success = status >= 200 && status < 300;
        HttpStatus httpStatus = HttpStatus.resolve(status);
        String message = success ? "Success"
                : httpStatus != null ? httpStatus.getReasonPhrase() : "Request failed";
        ApiResponse wrapped = new ApiResponse(message, body, success);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().remove(org.springframework.http.HttpHeaders.CONTENT_LENGTH);
        if (StringHttpMessageConverter.class.isAssignableFrom(converterType)) {
            return jsonMapper.writeValueAsString(wrapped);
        }
        return wrapped;
    }
}
