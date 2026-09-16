package com.alvaro.idempotence.consumer;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import com.alvaro.idempotence.event.PagamentoEvento;
import com.alvaro.idempotence.service.ContaCorrenteService;
import com.alvaro.idempotence.service.IdempotencyService;

@Component
public class PagamentoConsumer {

    private static final Logger log = LoggerFactory.getLogger(PagamentoConsumer.class);
    public static final String TOPICO_PAGAMENTOS = "pagamentos-idempotentes";

    private final IdempotencyService idempotencyService;
    private final ContaCorrenteService contaCorrenteService;

    public PagamentoConsumer(IdempotencyService idempotencyService,
            ContaCorrenteService contaCorrenteService) {
        this.idempotencyService = idempotencyService;
        this.contaCorrenteService = contaCorrenteService;
    }

    @KafkaListener(topics = TOPICO_PAGAMENTOS, groupId = "pagamento-idempotent-group")
    public void consumir(@Payload PagamentoEvento evento) {
        log.info("📥 Evento recebido do Kafka. TransacaoId: {} | Conta: {} | Valor: R$ {}",
                evento.transacaoId(), evento.contaOrigem(), evento.valor());

        // 1. Tenta adquirir o lock atômico no Redis por 2 minutos
        boolean lockAdquirido = idempotencyService.tentarAdquirirLock(evento.transacaoId(), Duration.ofMinutes(2));

        // 2. Se o lock falhou, é uma mensagem duplicada!
        if (!lockAdquirido) {
            log.warn(
                    "🛑 [DUPLICATA DETECTADA] A transação {} já foi processada anteriormente! Descartando mensagem sem duplicar o débito.",
                    evento.transacaoId());
            return; // ACK silencioso no Kafka
        }

        try {
            // 3. Executa o negócio com garantia de exclusividade global
            contaCorrenteService.processarDebito(evento);

            // 4. Confirma o processamento com retenção de 24 horas no Redis
            idempotencyService.marcarComoProcessado(evento.transacaoId(), Duration.ofHours(24));
            log.info("✅ Transação {} finalizada com sucesso e blindada contra duplicatas no Redis!",
                    evento.transacaoId());

        } catch (Exception ex) {
            // Se o core banking falhou por erro transitório, liberamos o lock para retry
            idempotencyService.liberarLock(evento.transacaoId());
            throw ex;
        }
    }
}
