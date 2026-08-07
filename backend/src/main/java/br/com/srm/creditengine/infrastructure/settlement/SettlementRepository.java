package br.com.srm.creditengine.infrastructure.settlement;

import br.com.srm.creditengine.domain.settlement.Settlement;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    /**
     * Carrega o agregado inteiro de uma vez. Com {@code open-in-view} desligado, a sessao já fechou
     * quando a resposta e montada; deixar qualquer associação preguiçosa aqui vira
     * {@code LazyInitializationException} na serialização.
     */
    @Query("""
            select s from Settlement s
              join fetch s.assignor
              join fetch s.faceCurrency
              join fetch s.paymentCurrency
              left join fetch s.items item
              left join fetch item.receivableType
            where s.reference = :reference
            """)
    Optional<Settlement> findByReference(@Param("reference") String reference);

    boolean existsByReference(String reference);
}
