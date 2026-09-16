package com.alvaro.outbox.domain;

import java.math.BigDecimal;
import java.time.Instant;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Pagamento {

    @Id
    private UUID id;
    @Column(nullable = false)
    private String contaOrigem;
    @Column(nullable = false)
    private String contaDestino;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal valor;
    @Column(nullable = false)
    private String status;
    @Column(nullable = false)
    private Instant createAt;

    protected Pagamento() {

    }

    public Pagamento(String contaOrigem, String contaDestino, BigDecimal valor) {
        this.id = UUID.randomUUID();
        this.contaOrigem = contaOrigem;
        this.contaDestino = contaDestino;
        this.valor = valor;
        this.status = "CRIADO";
        this.createAt = Instant.now();
    }

    public UUID getId() {
        return this.id;
    }

    public String getContaOrigem() {
        return this.contaOrigem;
    }

    public String getContaDestino() {
        return this.contaDestino;
    }

    public BigDecimal getValor() {
        return this.valor;
    }

    public String getStatus() {
        return this.status;
    }

    public Instant getCreateAt() {
        return this.createAt;
    }

}
