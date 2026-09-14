package com.alvaro.core.dto;

import java.math.BigDecimal;
import java.util.Map;

public record CambioExternaDTO(
        String result,
        String base_code,
        Map<String, BigDecimal> rates) {
}
