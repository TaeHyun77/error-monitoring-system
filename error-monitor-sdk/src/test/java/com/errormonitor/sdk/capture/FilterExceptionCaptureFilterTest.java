package com.errormonitor.sdk.capture;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FilterExceptionCaptureFilterTest {

    private ErrorCaptor errorCaptor;
    private FilterExceptionCaptureFilter filter;

    @BeforeEach
    void setUp() {
        errorCaptor = mock(ErrorCaptor.class);
        filter = new FilterExceptionCaptureFilter(errorCaptor, List.of());
    }

    // 1. 커스텀 필터 RuntimeException → 서버에 포착됨
    @Test
    void 커스텀_필터_RuntimeException_포착() throws Exception {
        RuntimeException exception = new RuntimeException("서버 오류");
        FilterChain chain = (req, res) -> { throw exception; };

        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain))
                .isSameAs(exception);

        verify(errorCaptor).captureException(eq(exception), any(HttpServletRequest.class));
    }

    // 2. ResponseStatusException(BAD_REQUEST) 던지는 필터 → 서버에 포착 안 됨
    @Test
    void ResponseStatusException_4xx_제외() throws Exception {
        ResponseStatusException exception = new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 요청");
        FilterChain chain = (req, res) -> { throw exception; };

        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain))
                .isSameAs(exception);

        verify(errorCaptor, never()).captureException(any(Exception.class), any());
    }

    // 3. @ResponseStatus(NOT_FOUND) 커스텀 예외 → 서버에 포착 안 됨
    @Test
    void ResponseStatus_어노테이션_4xx_제외() throws Exception {
        NotFoundException exception = new NotFoundException();
        FilterChain chain = (req, res) -> { throw exception; };

        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain))
                .isSameAs(exception);

        verify(errorCaptor, never()).captureException(any(Exception.class), any());
    }

    // 4. MVC 컨트롤러 예외가 필터까지 전파 → captureException 호출됨 (중복 방지는 ErrorCaptor 내부 WeakHashMap이 처리)
    @Test
    void MVC_컨트롤러_예외_포착() throws Exception {
        IllegalStateException exception = new IllegalStateException("컨트롤러 예외");
        FilterChain chain = (req, res) -> { throw exception; };

        assertThatThrownBy(() -> filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), chain))
                .isSameAs(exception);

        verify(errorCaptor).captureException(eq(exception), any(HttpServletRequest.class));
    }

    // 5. ignoreUrls 적용 → 해당 URL 예외 포착 안 됨
    @Test
    void ignoreUrls_적용_시_예외_포착_안됨() throws Exception {
        filter = new FilterExceptionCaptureFilter(errorCaptor, List.of("/actuator"));
        RuntimeException exception = new RuntimeException("헬스체크 오류");
        FilterChain chain = (req, res) -> { throw exception; };

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/actuator/health");

        assertThatThrownBy(() -> filter.doFilter(request, new MockHttpServletResponse(), chain))
                .isSameAs(exception);

        verify(errorCaptor, never()).captureException(any(Exception.class), any());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    static class NotFoundException extends RuntimeException {}
}
