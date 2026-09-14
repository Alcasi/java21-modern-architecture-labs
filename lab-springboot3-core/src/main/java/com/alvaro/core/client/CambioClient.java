package com.alvaro.core.client;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.service.annotation.GetExchange;

import com.alvaro.core.dto.CambioExternaDTO;

public interface CambioClient {

    @GetExchange("/{moedaOrigem}")
    CambioExternaDTO buscarTaxa(@PathVariable("moedaOrigem") String moedaOrigem);

}
