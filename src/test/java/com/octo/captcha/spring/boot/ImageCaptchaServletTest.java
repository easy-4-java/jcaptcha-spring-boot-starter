package com.octo.captcha.spring.boot;

import com.octo.captcha.service.image.ImageCaptchaService;
import com.octo.captcha.spring.boot.servlet.ImageCaptchaServlet;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImageCaptchaServletTest {

    @Test
    void rendersSessionChallengeAsNonCachedJpeg() throws Exception {
        ImageCaptchaService service = mock(ImageCaptchaService.class);
        when(service.getImageChallengeForID(anyString()))
                .thenReturn(new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB));
        ImageCaptchaServlet servlet = new ImageCaptchaServlet(service);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/jcaptcha.jpg");
        MockHttpServletResponse response = new MockHttpServletResponse();

        servlet.service(request, response);

        verify(service).getImageChallengeForID(request.getSession().getId());
        assertThat(response.getContentType()).isEqualTo("image/jpeg");
        assertThat(response.getHeader("Cache-Control")).contains("no-store");
        assertThat(ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()))).isNotNull();
    }
}
