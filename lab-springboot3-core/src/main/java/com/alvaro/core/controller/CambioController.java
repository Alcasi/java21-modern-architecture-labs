package com.alvaro.core.controller;

import java.math.BigDecimal;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alvaro.core.service.CambioService;

@RestController
@RequestMapping("api/cambio")
public class CambioController {

    private final CambioService cambioService;

    public CambioController(CambioService cambioService) {
        this.cambioService = cambioService;
    }

    @GetMapping("/{origem}/{destino}")
    public ResponseEntity<BigDecimal> getCambio(@PathVariable String origem, @PathVariable String destino) {
        BigDecimal taxa = cambioService.buscarTaxa(origem.toUpperCase(), destino.toUpperCase());
        return ResponseEntity.ok(taxa);
    }
}
