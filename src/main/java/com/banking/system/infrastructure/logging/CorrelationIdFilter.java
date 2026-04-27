package com.banking.system.infrastructure.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Injeta um ID de Correlação no MDC (Mapped Diagnostic Context) para cada requisição.
 *
 * O ID de correlação é:
 *   1. Lido do cabeçalho X-Correlation-ID da requisição (definido pelo API gateway ou serviço upstream).
 *   2. Se ausente, um novo UUID é gerado.
 *
 * Todos os logs na mesma thread da requisição incluirão este ID automaticamente
 * (via padrão logback/log4j MDC: %X{correlationId}).
 *
 * O ID também é retornado no cabeçalho X-Correlation-ID da resposta para que os clientes
 * possam correlacionar sua requisição com os logs do servidor ao reportar problemas.
 *
 * Rastreamento distribuído: o Micrometer Tracing (Brave) adiciona traceId/spanId ao MDC automaticamente.
 * O correlationId aqui é o ID visível pelo cliente (pode ser o mesmo que o traceId em algumas configurações).
 */
@Component
@Order(1)
public class CorrelationIdFilter extends OncePerRequestFilter {

    public static final String CABECALHO_CORRELATION_ID = "X-Correlation-ID";
    public static final String MDC_CORRELATION_ID       = "correlationId";
    public static final String MDC_TRACE_ID             = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(CABECALHO_CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        MDC.put(MDC_CORRELATION_ID, correlationId);
        MDC.put(MDC_TRACE_ID, correlationId); // também expõe como traceId para compatibilidade de padrões de log

        response.setHeader(CABECALHO_CORRELATION_ID, correlationId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CORRELATION_ID);
            MDC.remove(MDC_TRACE_ID);
        }
    }
}
