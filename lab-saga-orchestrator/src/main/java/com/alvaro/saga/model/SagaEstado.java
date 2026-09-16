package com.alvaro.saga.model;

import java.time.Instant;

public sealed interface SagaEstado
        permits SagaEstado.Iniciada,
        SagaEstado.SaldoReservado,
        SagaEstado.FraudeAprovada,
        SagaEstado.Concluida,
        SagaEstado.FalhaCompensada {

    record Iniciada(Instant dataInicio) implements SagaEstado {
    }

    record SaldoReservado(String idReserva, Instant dataReserva) implements SagaEstado {
    }

    record FraudeAprovada(String protocoloFraude, Instant dataAprovacao) implements SagaEstado {
    }

    record Concluida(String protocoloLiquidacao, Instant dataConclusao) implements SagaEstado {
    }

    record FalhaCompensada(String motivoFalha, Instant dataEstorno) implements SagaEstado {
    }
}
