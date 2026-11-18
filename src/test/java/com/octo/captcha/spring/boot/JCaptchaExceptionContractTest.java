package com.octo.captcha.spring.boot;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JCaptchaExceptionContractTest {

    @Test
    void defaultMessagesIdentifyCaptchaFailures() {
        assertThat(new com.octo.captcha.spring.boot.exception.CaptchaIncorrectException())
                .hasMessage("captcha incorrect");
        assertThat(new com.octo.captcha.spring.boot.exception.CaptchaNotFoundException())
                .hasMessage("no comparable captcha found");
        assertThat(new com.octo.captcha.spring.boot.exception.CaptchaTimeoutException())
                .hasMessage("captcha expired");
        assertThat(new com.octo.captcha.spring.boot.ext.exception.CaptchaIncorrectException())
                .hasMessage("captcha incorrect");
        assertThat(new com.octo.captcha.spring.boot.ext.exception.CaptchaNotFoundException())
                .hasMessage("no comparable captcha found");
        assertThat(new com.octo.captcha.spring.boot.ext.exception.CaptchaTimeoutException())
                .hasMessage("captcha expired");
    }

    @Test
    void customMessagesArePreserved() {
        assertThat(new com.octo.captcha.spring.boot.exception.CaptchaIncorrectException("invalid"))
                .hasMessage("invalid");
        assertThat(new com.octo.captcha.spring.boot.exception.CaptchaNotFoundException("missing"))
                .hasMessage("missing");
        assertThat(new com.octo.captcha.spring.boot.exception.CaptchaTimeoutException("old"))
                .hasMessage("old");
        assertThat(new com.octo.captcha.spring.boot.ext.exception.CaptchaIncorrectException("invalid"))
                .hasMessage("invalid");
        assertThat(new com.octo.captcha.spring.boot.ext.exception.CaptchaNotFoundException("missing"))
                .hasMessage("missing");
        assertThat(new com.octo.captcha.spring.boot.ext.exception.CaptchaTimeoutException("old"))
                .hasMessage("old");
    }
}
