package com.alvaro.core.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.alvaro.core.client.CambioClient;
import com.alvaro.core.dto.CambioExternaDTO;
import com.alvaro.core.entity.RegistroCambio;
import com.alvaro.core.repository.RegistroCambioRepository;

@Service
public class CambioService {

    private final CambioClient client;
    private final RegistroCambioRepository repository;

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CambioService.class);

    public CambioService(CambioClient client, RegistroCambioRepository repository) {
        this.client = client;
        this.repository = repository;
    }

    public BigDecimal buscarTaxa(String moedaOrigem, String moedaDestino) {

        log.info("Iniciando busca de taxa de câmbio de {} para {}", moedaOrigem, moedaDestino);

        CambioExternaDTO dto = client.buscarTaxa(moedaOrigem);

        if (dto == null || !"success".equals(dto.result()))
            throw new RuntimeException("Não foi possível obter a cotação");

        BigDecimal taxa = dto.rates().get(moedaDestino);

        log.info("Taxa obtida com sucesso: {}", taxa);

        repository.save(new RegistroCambio(moedaOrigem, moedaDestino, taxa));

        return taxa;

    }

}
