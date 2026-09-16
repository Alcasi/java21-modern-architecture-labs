package com.alvaro.saga.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.alvaro.saga.domain.SagaInstancia;

@Repository
public interface SagaInstanciaRepository extends JpaRepository<SagaInstancia, UUID> {
}
