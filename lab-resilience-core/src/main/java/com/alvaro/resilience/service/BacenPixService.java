package com.alvaro.resilience.service;

import org.springframework.stereotype.Service;
import com.alvaro.resilience.model.ChavePixValidacao;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Service
public class BacenPixService {

    @CircuitBreaker(name = "bacenPixService", fallbackMethod = "validarChaveFallback")
    public ChavePixValidacao validarChave(String chave, boolean simularFalha) {
        if (simularFalha) {
            System.out.println("❌ [Bacen] Falha de comunicação ou timeout com o Banco Central!");
            throw new RuntimeException("Timeout de rede ao conectar com o Bacen");
        }

        System.out.println("✅ [Bacen] Chave validada com sucesso pelo Bacen: " + chave);
        return new ChavePixValidacao(chave, "Alvaro Silva", "001 - Banco do Brasil", "ATIVA", "Validado com sucesso");
    }

    public ChavePixValidacao validarChaveFallback(String chave, boolean simularFalha, Throwable t) {
        System.out.println("🛡️ [Fallback Ativado] Circuito desarmado ou falha capturada: " + t.getMessage());
        return new ChavePixValidacao(
                chave,
                "Indisponível no momento",
                "Desconhecido",
                "MODO_DEGRADADO",
                "Bacen temporariamente indisponível. Consulta colocada em fila assíncrona.");
    }

}
