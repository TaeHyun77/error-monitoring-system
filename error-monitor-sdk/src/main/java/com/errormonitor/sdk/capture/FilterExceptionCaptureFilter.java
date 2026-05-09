package com.errormonitor.sdk.capture;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

// 필터 레이어에서 발생한 예외 중 실제 장애(5xx급)만 포착하는 필터
// 의도된 4xx(ResponseStatusException, @ResponseStatus)는 제외하고,
// Spring Security 예외(AccessDeniedException, AuthenticationException)는 ExceptionTranslationFilter가 내부 처리하므로 자연스럽게 제외됨
@RequiredArgsConstructor
public class FilterExceptionCaptureFilter extends OncePerRequestFilter {

    private final ErrorCaptor errorCaptor;
    private final List<String> ignoreUrls;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (RuntimeException e) {
            if (!HttpStatusExceptionUtils.is4xxException(e)) {
                errorCaptor.captureException(e, request);
            }
            throw e;
        } catch (IOException | ServletException e) {
            if (!HttpStatusExceptionUtils.is4xxException(e)) {
                errorCaptor.captureException((Exception) e, request);
            }
            throw e;
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return ignoreUrls != null && ignoreUrls.stream().anyMatch(uri::startsWith);
    }

}
