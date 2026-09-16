package com.alvaro.outbox.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.alvaro.outbox.domain.OutboxEvento;

@Repository
public interface OutboxEventoRepository extends JpaRepository<OutboxEvento, UUID> {
    List<OutboxEvento> findByStatus(String status);
}
