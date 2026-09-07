package com.bancoxyz.bffcajeros.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class PinFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Pin";

    @Value("${atm.pin.esperado}")
    private String pinEsperado;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        if (request.getRequestURI().startsWith("/cajero/")) {
            String pinRecibido = request.getHeader(HEADER_NAME);
            if (pinRecibido == null || !pinRecibido.equals(pinEsperado)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"PIN invalido o ausente (X-Pin)\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}