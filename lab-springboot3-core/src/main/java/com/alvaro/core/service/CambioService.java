package com.alvaro.core.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.alvaro.core.client.CambioClient;
import com.alvaro.core.dto.CambioExternaDTO;

@Service
public class CambioService {

    private final CambioClient client;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CambioService.class);

    public CambioService(CambioClient client) {
        this.client = client;
    }

    public BigDecimal buscarTaxa(String moedaOrigem, String moedaDestino) {

        log.info("Iniciando busca de taxa de câmbio de {} para {}", moedaOrigem, moedaDestino);

        CambioExternaDTO dto = client.buscarTaxa(moedaOrigem);

        if (dto == null || !"success".equals(dto.result()))
            throw new RuntimeException("Não foi possível obter a cotação");

        BigDecimal taxa = dto.rates().get(moedaDestino);

        log.info("Taxa obtida com sucesso: {}", taxa);

        return taxa;

    }

}
