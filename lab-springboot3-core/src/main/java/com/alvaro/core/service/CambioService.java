package com.alvaro.core.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import com.alvaro.core.client.CambioClient;
import com.alvaro.core.dto.CambioExternaDTO;

@Service
public class CambioService {

    private final CambioClient client;

    public CambioService(CambioClient client) {
        this.client = client;
    }

    public BigDecimal buscarTaxa(String moedaOrigem, String moedaDestino) {

        CambioExternaDTO dto = client.buscarTaxa(moedaOrigem);

        if (dto == null || !"success".equals(dto.result()))
            throw new RuntimeException("Não foi possível obter a cotação");

        return dto.rates().get(moedaDestino);

    }

}
