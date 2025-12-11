package com.library.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Component
public class ConcurrencyFilter implements Filter {

    // Iska matlab hai sirf 10 log ek sath andar aa sakte hain.
    // 'true' ka matlab hai FIFO (First In First Out) queue banegi.
    private final Semaphore semaphore = new Semaphore(10, true);

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            // User yahan wait karega agar 10 seats full hain.
            // Hum 30 second ka timeout dete hain taaki user hamesha ke liye na atka rahe.
            if (semaphore.tryAcquire(30, TimeUnit.SECONDS)) {
                try {
                    // Seat mil gayi -> Request process karo
                    chain.doFilter(request, response);
                } finally {
                    // Kaam khatam -> Seat khali karo
                    semaphore.release();
                }
            } else {
                // 30 second tak jagah nahi mili -> Error bhejo
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.setStatus(503); // Service Unavailable
                httpResponse.getWriter().write("Server is busy (Too many users). Please try again in a moment.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}