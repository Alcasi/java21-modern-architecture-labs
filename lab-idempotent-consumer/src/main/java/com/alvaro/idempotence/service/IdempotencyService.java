package com.alvaro.idempotence.service;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class IdempotencyService {

    private static final Logger log = LoggerFactory.getLogger(IdempotencyService.class);
    private static final String PREFIXO_CHAVE = "idempotency:transacao:";

    private final StringRedisTemplate redisTemplate;

    public IdempotencyService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * Tenta adquirir o lock atômico no Redis usando SETNX.
     * Retorna true se for a primeira vez que a mensagem chega.
     * Retorna false se for uma duplicata.
     */
    public boolean tentarAdquirirLock(String transacaoId, Duration lockTimeout) {
        String chave = PREFIXO_CHAVE + transacaoId;
        // setIfAbsent equivale ao comando atômico do Redis: SET key value NX EX seconds
        Boolean sucesso = redisTemplate.opsForValue().setIfAbsent(chave, "PROCESSING", lockTimeout);
        return Boolean.TRUE.equals(sucesso);
    }

    /**
     * Marca a transação como permanentemente processada no Redis com TTL de
     * segurança.
     */
    public void marcarComoProcessado(String transacaoId, Duration ttlSeguranca) {
        String chave = PREFIXO_CHAVE + transacaoId;
        redisTemplate.opsForValue().set(chave, "PROCESSED", ttlSeguranca);
        log.info("🔑 Chave de idempotência confirmada no Redis: {}", chave);
    }

    /**
     * Em caso de erro transitório no negócio, liberamos o lock para retentativa.
     */
    public void liberarLock(String transacaoId) {
        String chave = PREFIXO_CHAVE + transacaoId;
        redisTemplate.delete(chave);
        log.warn("🔓 Lock de idempotência liberado para retry: {}", chave);
    }
}
