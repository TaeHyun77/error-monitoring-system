package com.errormonitor.server.auth;

import com.errormonitor.server.project.repository.ProjectRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ProjectRepository projectRepository;

    public ApiKeyAuthFilter(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if ("/api/errors".equals(path) && "POST".equalsIgnoreCase(request.getMethod())) {
            String apiKey = request.getHeader("X-API-Key");

            if (apiKey == null || apiKey.isBlank()) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "API Key가 누락되었습니다");
                return;
            }

            if (!projectRepository.existsByApiKey(apiKey)) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "유효하지 않은 API Key입니다");
                return;
            }

            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    apiKey, null, List.of(new SimpleGrantedAuthority("ROLE_SDK"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        filterChain.doFilter(request, response);
    }
}
