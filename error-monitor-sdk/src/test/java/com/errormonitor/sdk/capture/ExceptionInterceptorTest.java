package com.errormonitor.sdk.capture;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ExceptionInterceptorTest {

    private static final String DISPATCHER_EXCEPTION_ATTR =
            "org.springframework.web.servlet.DispatcherServlet.EXCEPTION";

    private ErrorCaptor errorCaptor;
    private ExceptionInterceptor interceptor;

    @BeforeEach
    void setUp() {
        errorCaptor = mock(ErrorCaptor.class);
        interceptor = new ExceptionInterceptor(errorCaptor, List.of());
    }

    // 검증 4: NullPointerException 등 상태 코드 없는 예외 → 정상 캡처
    @Test
    void 상태코드_없는_예외_정상_캡처() {
        NullPointerException exception = new NullPointerException("NPE");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);

        interceptor.afterCompletion(request, response, null, exception);

        verify(errorCaptor).captureException(eq(exception), any(HttpServletRequest.class));
    }

    // 검증 1: ResponseStatusException(NOT_FOUND) → 캡처 안 됨 (2순위 is4xxException으로 필터링)
    @Test
    void ResponseStatusException_4xx_제외() {
        ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND, "없음");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.afterCompletion(request, response, null, exception);

        verify(errorCaptor, never()).captureException(any(Exception.class), any());
    }

    // 검증 2: @ResponseStatus(BAD_REQUEST) 예외 → 캡처 안 됨
    @Test
    void ResponseStatus_어노테이션_4xx_제외() {
        BadRequestException exception = new BadRequestException();
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        interceptor.afterCompletion(request, response, null, exception);

        verify(errorCaptor, never()).captureException(any(Exception.class), any());
    }

    // 검증 3: 커스텀 예외 + response status 400 → 1순위(response.getStatus())로 캡처 안 됨
    @Test
    void 커스텀_예외_응답상태_4xx_제외() {
        RuntimeException exception = new RuntimeException("커스텀 비즈니스 예외");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(400);

        interceptor.afterCompletion(request, response, null, exception);

        verify(errorCaptor, never()).captureException(any(Exception.class), any());
    }

    // 검증 5: ResponseStatusException(INTERNAL_SERVER_ERROR) → 정상 캡처
    @Test
    void ResponseStatusException_5xx_정상_캡처() {
        ResponseStatusException exception = new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);

        interceptor.afterCompletion(request, response, null, exception);

        verify(errorCaptor).captureException(eq(exception), any(HttpServletRequest.class));
    }

    // DISPATCHER_EXCEPTION_ATTR 경로에서 4xx response status → 캡처 안 됨
    @Test
    void DISPATCHER_EXCEPTION_ATTR_4xx_제외() {
        RuntimeException exception = new RuntimeException("ControllerAdvice 처리된 예외");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(DISPATCHER_EXCEPTION_ATTR, exception);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(404);

        interceptor.afterCompletion(request, response, null, null);

        verify(errorCaptor, never()).captureException(any(Exception.class), any());
    }

    // DISPATCHER_EXCEPTION_ATTR 경로에서 5xx → 정상 캡처
    @Test
    void DISPATCHER_EXCEPTION_ATTR_5xx_정상_캡처() {
        RuntimeException exception = new RuntimeException("서버 내부 오류");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(DISPATCHER_EXCEPTION_ATTR, exception);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);

        interceptor.afterCompletion(request, response, null, null);

        verify(errorCaptor).captureException(eq(exception), any(HttpServletRequest.class));
    }

    // ignoreUrls 적용 시 예외 포착 안 됨
    @Test
    void ignoreUrls_적용_시_예외_포착_안됨() {
        interceptor = new ExceptionInterceptor(errorCaptor, List.of("/actuator"));
        RuntimeException exception = new RuntimeException("헬스체크 오류");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(500);

        interceptor.afterCompletion(request, response, null, exception);

        verify(errorCaptor, never()).captureException(any(Exception.class), any());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    static class BadRequestException extends RuntimeException {}
}
