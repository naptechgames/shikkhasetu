package bd.bubt.shikkhasetu.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import bd.bubt.shikkhasetu.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Runs before every /api call (except login, register and health). It reads
 * "Authorization: Bearer <token>", finds the user and stores it on the request
 * as "currentUser". Without a valid token the call ends with 401.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String CURRENT_USER = "currentUser";

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(CURRENT_USER, authService.resolve(tokenOf(request)));
        return true;
    }

    public static String tokenOf(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return header != null && header.startsWith("Bearer ") ? header.substring(7).trim() : null;
    }

    @Configuration
    public static class WebConfig implements WebMvcConfigurer {

        private final AuthInterceptor authInterceptor;

        public WebConfig(AuthInterceptor authInterceptor) {
            this.authInterceptor = authInterceptor;
        }

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            registry.addInterceptor(authInterceptor)
                    .addPathPatterns("/api/**")
                    .excludePathPatterns("/api/auth/login", "/api/auth/register", "/api/health");
        }
    }
}
