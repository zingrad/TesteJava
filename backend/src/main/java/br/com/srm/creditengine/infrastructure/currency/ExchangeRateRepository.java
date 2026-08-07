package br.com.srm.creditengine.infrastructure.currency;

import br.com.srm.creditengine.domain.currency.ExchangeRate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    @Query("""
            select r from ExchangeRate r
              join fetch r.baseCurrency
              join fetch r.quoteCurrency
            where r.baseCurrency.code = :base
              and r.quoteCurrency.code = :quote
              and r.effectiveAt <= :moment
            order by r.effectiveAt desc
            """)
    Optional<ExchangeRate> findEffectiveAt(
            @Param("base") String base,
            @Param("quote") String quote,
            @Param("moment") OffsetDateTime moment,
            Limit limit);

    @Query("""
            select r from ExchangeRate r
              join fetch r.baseCurrency
              join fetch r.quoteCurrency
            where r.baseCurrency.code = :base
              and r.quoteCurrency.code = :quote
            order by r.effectiveAt desc
            """)
    List<ExchangeRate> findHistory(
            @Param("base") String base,
            @Param("quote") String quote,
            Limit limit);
}
