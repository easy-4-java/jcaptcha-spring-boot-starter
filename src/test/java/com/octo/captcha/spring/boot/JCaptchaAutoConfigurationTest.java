/*
 * Copyright (c) 2018, hiwepy (https://github.com/hiwepy).
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.octo.captcha.spring.boot;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;

import com.octo.captcha.service.image.ImageCaptchaService;
import com.octo.captcha.spring.boot.ext.filter.ExtendImageCaptchaFilter;
import com.octo.captcha.spring.boot.servlet.ImageCaptchaServlet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for {{ @link JCaptchaAutoConfiguration }}.
 *
 * <p>Verifies the auto-configuration activates under the expected conditions
 * and exposes its declared beans.</p>
 *
 * @author <a href="https://github.com/loong10k">Loong Wan</a>
 * @since 1.0.0
 */
@DisplayName("JCaptchaAutoConfiguration Tests")
class JCaptchaAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner();

    @Test
    @DisplayName("Auto-configuration class can be instantiated")
    void testInstantiation() {
        JCaptchaAutoConfiguration configuration = new JCaptchaAutoConfiguration();
        assertThat(configuration).isNotNull();
    }

    @Test
    @DisplayName("Auto-configuration loads when 'jcaptcha.type=servlet'")
    void testLoadsWhenEnabledPropertySet() {
        runner.withUserConfiguration(JCaptchaAutoConfiguration.class)
                .withPropertyValues("jcaptcha.type=servlet")
                .run(context -> {
                    assertThat(context).hasSingleBean(JCaptchaAutoConfiguration.class);
                    assertThat(context).hasBean("servletRegistrationBean");
                    assertThat(context).doesNotHaveBean("imageCaptchaFilter");
                });
    }

    @Test
    @DisplayName("Filter mode registers the image CAPTCHA filter")
    void testFilterMode() {
        runner.withUserConfiguration(JCaptchaAutoConfiguration.class)
                .withPropertyValues("jcaptcha.type=filter")
                .run(context -> {
                    assertThat(context).hasBean("imageCaptchaFilter");
                    assertThat(context).doesNotHaveBean("servletRegistrationBean");
                });
    }

    @Test
    @DisplayName("No endpoint is registered when the mode is unset")
    void testNoEndpointWhenTypeAbsent() {
        runner.withUserConfiguration(JCaptchaAutoConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(JCaptchaAutoConfiguration.class);
                    assertThat(context).doesNotHaveBean("servletRegistrationBean");
                    assertThat(context).doesNotHaveBean("imageCaptchaFilter");
                });
    }

    @Test
    @DisplayName("Filter configuration binds session, endpoint and verification settings")
    void testFilterSettingsBind() {
        runner.withUserConfiguration(JCaptchaAutoConfiguration.class)
                .withPropertyValues(
                        "jcaptcha.type=filter",
                        "jcaptcha.captcha-store-key=code",
                        "jcaptcha.captcha-date-store-key=created",
                        "jcaptcha.captcha-timeout=120000",
                        "jcaptcha.captcha-servlet-pattern=/image.jpg",
                        "jcaptcha.captcha-filter-pattern=/check/*",
                        "jcaptcha.captcha-register-to-mbean-server=true",
                        "jcaptcha.captcha-question-parameter-name=question",
                        "jcaptcha.captcha-rendering-url=/render",
                        "jcaptcha.captcha-error-url=/error",
                        "jcaptcha.captcha-challenge-response-parameter-name=answer",
                        "jcaptcha.captcha-verification-urls=/verify",
                        "jcaptcha.captcha-forward-error-urls=/failure")
                .run(context -> {
                    JCaptchaProperties properties = context.getBean(JCaptchaProperties.class);
                    assertThat(properties.getType()).isEqualTo(JCaptchaProperties.JCaptchaType.FILTER);
                    assertThat(properties.getCaptchaStoreKey()).isEqualTo("code");
                    assertThat(properties.getCaptchaDateStoreKey()).isEqualTo("created");
                    assertThat(properties.getCaptchaTimeout()).isEqualTo(120000);
                    assertThat(properties.getCaptchaServletPattern()).isEqualTo("/image.jpg");
                    assertThat(properties.getCaptchaFilterPattern()).isEqualTo("/check/*");
                    assertThat(properties.isCaptchaRegisterToMBeanServer()).isTrue();
                    assertThat(properties.getCaptchaQuestionParameterName()).isEqualTo("question");
                    assertThat(properties.getCaptchaRenderingURL()).isEqualTo("/render");
                    assertThat(properties.getCaptchaErrorURL()).isEqualTo("/error");
                    assertThat(properties.getCaptchaChallengeResponseParameterName()).isEqualTo("answer");
                    assertThat(properties.getCaptchaVerificationURLs()).isEqualTo("/verify");
                    assertThat(properties.getCaptchaForwardErrorURLs()).isEqualTo("/failure");
                    FilterRegistrationBean<?> registration = context.getBean("imageCaptchaFilter", FilterRegistrationBean.class);
                    assertThat(registration.getInitParameters())
                            .containsEntry(ExtendImageCaptchaFilter.CAPTCHA_RENDERING_URL_PARAMETER, "/render")
                            .containsEntry(ExtendImageCaptchaFilter.CAPTCHA_ERROR_URL_PARAMETER, "/error");
                });
    }

    @Test
    @DisplayName("Central JCaptcha core renders a real JPEG challenge")
    void testRealImageChallenge() {
        runner.withUserConfiguration(JCaptchaAutoConfiguration.class)
                .run(context -> {
                    ImageCaptchaServlet servlet = new ImageCaptchaServlet(context.getBean(ImageCaptchaService.class));
                    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jcaptcha.jpg");
                    MockHttpServletResponse response = new MockHttpServletResponse();
                    assertThatCode(() -> {
                        servlet.service(request, response);
                        assertThat(response.getContentType()).isEqualTo("image/jpeg");
                        assertThat(ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()))).isNotNull();
                    }).doesNotThrowAnyException();
                });
    }
}
