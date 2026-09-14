package com.alvaro.core;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.alvaro.core.entity.RegistroCambio;
import com.alvaro.core.repository.RegistroCambioRepository;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CambioIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private RegistroCambioRepository repository;

    @Test
    @DisplayName("Deve consultar cambio e persistir auditoria no PostgreSQL efemero")
    void deveConsultarCambioEPersistirAuditoria() {
        // 1. Executa chamada HTTP real ao endpoint
        ResponseEntity<BigDecimal> response = restTemplate.getForEntity("/api/cambio/USD/BRL", BigDecimal.class);

        // 2. Valida resposta HTTP
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isGreaterThan(BigDecimal.ZERO);

        // 3. Valida se o registro foi salvo no banco PostgreSQL real
        List<RegistroCambio> registros = repository.findAll();
        assertThat(registros).isNotEmpty();

        // Repare no método .getLast() do Java 21 (Sequenced Collections)!
        RegistroCambio ultimo = registros.getLast();
        assertThat(ultimo.getMoedaOrigem()).isEqualTo("USD");
        assertThat(ultimo.getMoedaDestino()).isEqualTo("BRL");
        assertThat(ultimo.getTaxa()).isEqualByComparingTo(response.getBody());
        assertThat(ultimo.getDataHora()).isNotNull();
    }
}
