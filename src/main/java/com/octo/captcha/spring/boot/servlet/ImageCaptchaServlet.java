package com.octo.captcha.spring.boot.servlet;

import com.octo.captcha.service.image.ImageCaptchaService;

import javax.imageio.ImageIO;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;

/**
 * Renders the JCaptcha challenge associated with the current HTTP session.
 */
public class ImageCaptchaServlet extends HttpServlet {

    private final ImageCaptchaService captchaService;

    public ImageCaptchaServlet(ImageCaptchaService captchaService) {
        this.captchaService = Objects.requireNonNull(captchaService, "captchaService");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        BufferedImage challenge = captchaService.getImageChallengeForID(request.getSession().getId());
        response.setContentType("image/jpeg");
        response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        ImageIO.write(challenge, "jpg", response.getOutputStream());
    }
}
