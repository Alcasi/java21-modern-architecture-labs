package com.alvaro.core.dto;

import java.time.Instant;

public record StatusServico(
        String servico,
        String status,
        String threadName,
        boolean isVirtualThread,
        Instant timestamp) {
}
