package com.octo.captcha.spring.boot.filter.image;

import com.octo.captcha.service.CaptchaServiceException;
import com.octo.captcha.service.image.DefaultManageableImageCaptchaService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterConfig;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.imageio.ImageIO;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class LegacyImageCaptchaFilterBehaviorTest {

    @Test
    void rendersJpegAndReturnsNotFoundWhenChallengeCannotBeGenerated() throws Exception {
        ImageCaptchaFilter filter = new ImageCaptchaFilter();
        filter.init(config());
        FilterChain chain = mock(FilterChain.class);
        MockHttpServletRequest request = request("/image");
        request.setQueryString("refresh=1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);

        assertThat(response.getContentType()).isEqualTo("image/jpeg");
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");
        assertThat(ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray())).getWidth()).isEqualTo(10);
        verifyNoInteractions(chain);

        MockHttpServletRequest unavailable = request("/image");
        unavailable.addPreferredLocale(Locale.FRENCH);
        MockHttpServletResponse unavailableResponse = new MockHttpServletResponse();
        filter.doFilter(unavailable, unavailableResponse, chain);
        assertThat(unavailableResponse.getStatus()).isEqualTo(404);
        filter.destroy();
    }

    @Test
    void addsQuestionAndContinuesOrdinaryRequests() throws Exception {
        ImageCaptchaFilter filter = new ImageCaptchaFilter();
        filter.init(config());
        MockHttpServletRequest request = request("/form");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(request, response, chain);

        assertThat(request.getAttribute("question")).isEqualTo("fixture question");
        verify(chain).doFilter(request, response);
    }

    @Test
    void verifiesAnswerAndRedirectsMissingOrIncorrectAnswers() throws Exception {
        ImageCaptchaFilter filter = new ImageCaptchaFilter();
        filter.init(config());
        MockHttpServletRequest valid = request("/verify");
        valid.setParameter("answer", "correct");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);
        filter.doFilter(valid, response, chain);
        verify(chain).doFilter(valid, response);

        for (String answer : new String[]{null, "wrong", "expired"}) {
            MockHttpServletRequest invalid = request("/verify");
            if (answer != null) {
                invalid.setParameter("answer", answer);
            }
            invalid.setContextPath("/app");
            MockHttpServletResponse invalidResponse = new MockHttpServletResponse();
            filter.doFilter(invalid, invalidResponse, chain);
            assertThat(invalidResponse.getRedirectedUrl()).isEqualTo("/app/retry");
        }

        FilterChain brokenChain = (req, res) -> { throw new IOException("fixture failure"); };
        assertThrows(ServletException.class, () -> filter.doFilter(valid, response, brokenChain));
    }

    @Test
    void rejectsInconsistentMappingsAndMissingService() {
        MockFilterConfig invalid = config();
        invalid.addInitParameter("CaptchaFailURLs", "/retry;/other");
        assertThrows(ServletException.class, () -> new ImageCaptchaFilter().init(invalid));

        MockFilterConfig missingService = config();
        missingService.addInitParameter("ImageCaptchaServiceClass", "invalid.MissingService");
        assertThrows(CaptchaServiceException.class, () -> new ImageCaptchaFilter().init(missingService));
    }

    private static MockHttpServletRequest request(String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setServletPath(path);
        return request;
    }

    private static MockFilterConfig config() {
        MockFilterConfig config = new MockFilterConfig();
        config.addInitParameter("CaptchaRenderingURL", "/image");
        config.addInitParameter("CaptchaErrorURL", "/error");
        config.addInitParameter("CaptchaVerificationURLs", "/verify");
        config.addInitParameter("CaptchaFailURLs", "/retry");
        config.addInitParameter("CaptchaQuestionParameterName", "question");
        config.addInitParameter("CaptchaChallengeResponseParameterName", "answer");
        config.addInitParameter("ImageCaptchaServiceClass", FixtureService.class.getName());
        return config;
    }

    public static class FixtureService extends DefaultManageableImageCaptchaService {

        @Override
        public BufferedImage getImageChallengeForID(String id, Locale locale) {
            if (Locale.FRENCH.equals(locale)) {
                throw new CaptchaServiceException("fixture unavailable");
            }
            return new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        }

        @Override
        public String getQuestionForID(String id, Locale locale) {
            return "fixture question";
        }

        @Override
        public Boolean validateResponseForID(String id, Object response) {
            if ("expired".equals(response)) {
                throw new CaptchaServiceException("fixture expired");
            }
            return "correct".equals(response);
        }
    }
}
