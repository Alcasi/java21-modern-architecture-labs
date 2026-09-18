package com.alvaro.dynamodb.model;

import java.math.BigDecimal;
import java.time.Instant;

import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@DynamoDbBean
public class TransacaoLedger {

    // Campos privados limpos, SEM anotações
    private String pk;
    private String sk;
    private String transacaoId;
    private String contaId;
    private String descricao;
    private BigDecimal valor;
    private String tipo;
    private String status;
    private Instant criadoEm;

    // As anotações ficam obrigatoriamente nos GETTERS:

    @DynamoDbPartitionKey
    @DynamoDbAttribute("pk")
    public String getPk() {
        return this.pk;
    }

    public void setPk(String pk) {
        this.pk = pk;
    }

    @DynamoDbSortKey
    @DynamoDbAttribute("sk")
    public String getSk() {
        return this.sk;
    }

    public void setSk(String sk) {
        this.sk = sk;
    }

    public String getTransacaoId() {
        return this.transacaoId;
    }

    public void setTransacaoId(String transacaoId) {
        this.transacaoId = transacaoId;
    }

    public String getContaId() {
        return this.contaId;
    }

    public void setContaId(String contaId) {
        this.contaId = contaId;
    }

    public String getDescricao() {
        return this.descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValor() {
        return this.valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getTipo() {
        return this.tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCriadoEm() {
        return this.criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }

    public static String buildPk(String contaId) {
        return "CONTA#" + contaId;
    }

    public static String buildSk(Instant timestamp, String transacaoId) {
        return String.format("TRX#%s#%s", timestamp.toString(), transacaoId);
    }
}
