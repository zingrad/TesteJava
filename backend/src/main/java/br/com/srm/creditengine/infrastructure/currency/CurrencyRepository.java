package br.com.srm.creditengine.infrastructure.currency;

import br.com.srm.creditengine.domain.currency.Currency;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency, String> {

    List<Currency> findAllByOrderByCodeAsc();
}
