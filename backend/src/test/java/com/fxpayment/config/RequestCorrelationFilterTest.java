package com.fxpayment.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("RequestCorrelationFilter unit tests")
class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("generates request ID when none provided in header")
    void shouldGenerateRequestIdWhenNoneProvided() throws Exception {
        filter.doFilterInternal(request, response, new MockFilterChain());

        String responseId = response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER);
        assertNotNull(responseId);
        UUID.fromString(responseId);
    }

    @Test
    @DisplayName("uses client-provided request ID when present")
    void shouldUseClientProvidedRequestId() throws Exception {
        String clientId = UUID.randomUUID().toString();
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, clientId);

        filter.doFilterInternal(request, response, new MockFilterChain());

        assertEquals(clientId, response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER));
    }

    @Test
    @DisplayName("sets request ID in MDC during request processing")
    void shouldSetMdcDuringRequest() throws Exception {
        AtomicReference<String> capturedMdcValue = new AtomicReference<>();
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) {
                capturedMdcValue.set(MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID));
            }
        };

        filter.doFilterInternal(request, response, chain);

        assertNotNull(capturedMdcValue.get());
        assertEquals(response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER), capturedMdcValue.get());
    }

    @Test
    @DisplayName("clears MDC after request completes")
    void shouldClearMdcAfterRequest() throws Exception {
        filter.doFilterInternal(request, response, new MockFilterChain());

        assertNull(MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID));
    }

    @Test
    @DisplayName("clears MDC even when filter chain throws exception")
    void shouldClearMdcOnException() {
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res) throws ServletException {
                throw new ServletException("test exception");
            }
        };

        assertThrows(ServletException.class,
                () -> filter.doFilterInternal(request, response, chain));
        assertNull(MDC.get(RequestCorrelationFilter.MDC_REQUEST_ID));
    }

    @Test
    @DisplayName("generates new ID when header is blank")
    void shouldGenerateIdWhenHeaderIsBlank() throws Exception {
        request.addHeader(RequestCorrelationFilter.REQUEST_ID_HEADER, "   ");

        filter.doFilterInternal(request, response, new MockFilterChain());

        String responseId = response.getHeader(RequestCorrelationFilter.REQUEST_ID_HEADER);
        assertNotNull(responseId);
        UUID.fromString(responseId);
    }
}
