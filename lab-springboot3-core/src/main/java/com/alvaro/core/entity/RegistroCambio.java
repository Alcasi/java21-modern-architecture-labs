package com.alvaro.core.entity;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "registro_cambio")
public class RegistroCambio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String moedaOrigem;

    private String moedaDestino;

    @Column(precision = 19, scale = 6)
    private BigDecimal taxa;

    private Instant dataHora;

    protected RegistroCambio() {
    }

    public RegistroCambio(String moedaOrigem, String moedaDestino, BigDecimal taxa) {
        this.moedaOrigem = moedaOrigem;
        this.moedaDestino = moedaDestino;
        this.taxa = taxa;
        this.dataHora = Instant.now();
    }

    public String getMoedaOrigem() {
        return moedaOrigem;
    }

    public String getMoedaDestino() {
        return moedaDestino;
    }

    public BigDecimal getTaxa() {
        return taxa;
    }

    public Instant getDataHora() {
        return dataHora;
    }

}
