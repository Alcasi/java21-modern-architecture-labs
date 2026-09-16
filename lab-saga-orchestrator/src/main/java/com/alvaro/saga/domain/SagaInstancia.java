package com.alvaro.saga.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "saga_instancias")
public class SagaInstancia {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String contaOrigem;

    @Column(nullable = false)
    private String contaDestino;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal valor;

    @Column(nullable = false)
    private String estadoAtual;

    private String idReserva;

    private String motivoFalha;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected SagaInstancia() {
    }

    public SagaInstancia(String contaOrigem, String contaDestino, BigDecimal valor) {
        this.id = UUID.randomUUID();
        this.contaOrigem = contaOrigem;
        this.contaDestino = contaDestino;
        this.valor = valor;
        this.estadoAtual = "INICIADA";
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public void transicionarEstado(String novoEstado, String idReserva) {
        this.estadoAtual = novoEstado;
        this.idReserva = idReserva;
        this.updatedAt = Instant.now();
    }

    public void registrarFalhaCompensada(String motivoFalha) {
        this.estadoAtual = "FALHA_COMPENSADA";
        this.motivoFalha = motivoFalha;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getContaOrigem() {
        return contaOrigem;
    }

    public String getContaDestino() {
        return contaDestino;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public String getEstadoAtual() {
        return estadoAtual;
    }

    public String getIdReserva() {
        return idReserva;
    }

    public String getMotivoFalha() {
        return motivoFalha;
    }
}
