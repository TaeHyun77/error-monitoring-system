package com.errormonitor.sdk.capture;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

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
            if (!is4xxException(e)) {
                errorCaptor.captureException(e, request);
            }
            throw e;
        } catch (IOException | ServletException e) {
            if (!is4xxException(e)) {
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

    // catch 시점에 response.getStatus()는 아직 결정되지 않으므로(기본값 200),
    // 예외 객체 자체에서 4xx 여부를 판단한다
    private boolean is4xxException(Exception e) {
        // 방법 A: ResponseStatusException 상태 코드 확인
        if (e instanceof ResponseStatusException rse) {
            return rse.getStatusCode().is4xxClientError();
        }
        // 방법 B: @ResponseStatus 어노테이션 확인
        ResponseStatus annotation = e.getClass().getAnnotation(ResponseStatus.class);
        return annotation != null && annotation.value().is4xxClientError();
    }
}
