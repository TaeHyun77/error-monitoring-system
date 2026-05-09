package com.errormonitor.sdk.capture;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

class HttpStatusExceptionUtilsTest {

    // 검증 1, 2: ResponseStatusException 4xx → true
    @Test
    void ResponseStatusException_4xx_true() {
        Exception ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "없음");
        assertThat(HttpStatusExceptionUtils.is4xxException(ex)).isTrue();
    }

    @Test
    void ResponseStatusException_BAD_REQUEST_true() {
        Exception ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 요청");
        assertThat(HttpStatusExceptionUtils.is4xxException(ex)).isTrue();
    }

    // 검증 5: ResponseStatusException 5xx → false
    @Test
    void ResponseStatusException_5xx_false() {
        Exception ex = new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류");
        assertThat(HttpStatusExceptionUtils.is4xxException(ex)).isFalse();
    }

    // 검증 2: @ResponseStatus 4xx 어노테이션 → true
    @Test
    void ResponseStatus_어노테이션_4xx_true() {
        Exception ex = new BadRequestException();
        assertThat(HttpStatusExceptionUtils.is4xxException(ex)).isTrue();
    }

    // @ResponseStatus 5xx 어노테이션 → false
    @Test
    void ResponseStatus_어노테이션_5xx_false() {
        Exception ex = new InternalServerErrorException();
        assertThat(HttpStatusExceptionUtils.is4xxException(ex)).isFalse();
    }

    // 검증 4: 상태 코드 없는 일반 예외 → false
    @Test
    void 일반_예외_false() {
        Exception ex = new NullPointerException("NPE");
        assertThat(HttpStatusExceptionUtils.is4xxException(ex)).isFalse();
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    static class BadRequestException extends RuntimeException {}

    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    static class InternalServerErrorException extends RuntimeException {}
}
