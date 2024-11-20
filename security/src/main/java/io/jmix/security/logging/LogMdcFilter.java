package io.jmix.security.logging;

import io.jmix.security.authentication.CurrentAuthentication;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that sets up MDC (Mapped Diagnostic Context) for each http request.
 */
public class LogMdcFilter extends OncePerRequestFilter {

    protected CurrentAuthentication currentAuthentication;

    public LogMdcFilter(CurrentAuthentication currentAuthentication) {
        this.currentAuthentication = currentAuthentication;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        if (currentAuthentication.isSet()) {
            LogMdc.setup(currentAuthentication.getAuthentication());
            try {
                filterChain.doFilter(request, response);
            } finally {
                LogMdc.setup(null);
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
