package com.alvaro.resilience;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.alvaro.resilience.model.ChavePixValidacao;
import com.alvaro.resilience.service.BacenPixService;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BacenPixServiceIntegrationTest {

    @Autowired
    private BacenPixService bacenPixService;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    private CircuitBreaker circuitBreaker;

    @BeforeEach
    void setUp() {
        // Recupera o Circuit Breaker configurado no application.yml
        circuitBreaker = circuitBreakerRegistry.circuitBreaker("bacenPixService");
        circuitBreaker.reset(); // Garante estado inicial CLOSED para cada teste
    }

    @Test
    @DisplayName("1. Deve validar chave com sucesso quando o circuito estiver CLOSED")
    void deveValidarChaveComSucessoEmEstadoFechado() {
        ChavePixValidacao resultado = bacenPixService.validarChave("11999998888", false);

        assertThat(resultado.status()).isEqualTo("ATIVA");
        assertThat(resultado.titular()).isEqualTo("Alvaro Silva");
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }

    @Test
    @DisplayName("2. Deve abrir o circuito (OPEN) após taxa de falhas > 50% e acionar Fallback instantâneo")
    void deveAbrirCircuitoEAtivarFallbackAposFalhasConsecutivas() {
        // 1. O circuito começa FECHADO
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // 2. Simulamos 5 chamadas consecutivas com falha (conforme sliding-window-size
        // = 5)
        for (int i = 0; i < 5; i++) {
            // O fallback captura a exceção e devolve MODO_DEGRADADO
            ChavePixValidacao resposta = bacenPixService.validarChave("11999998888", true);
            assertThat(resposta.status()).isEqualTo("MODO_DEGRADADO");
        }

        // 3. Validação de Staff Engineer: o disjuntor DESARMOU para OPEN!
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // 4. Nova chamada: mesmo pedindo para NÃO falhar, o circuito está ABERTO!
        // A chamada é bloqueada localmente em < 1ms sem bater no Bacen
        ChavePixValidacao respostaComCircuitoAberto = bacenPixService.validarChave("11999998888", false);

        assertThat(respostaComCircuitoAberto.status()).isEqualTo("MODO_DEGRADADO");
        assertThat(respostaComCircuitoAberto.mensagem()).contains("temporariamente indisponível");
    }
}
