package com.alvaro.outbox.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.alvaro.outbox.domain.Pagamento;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> {
}
