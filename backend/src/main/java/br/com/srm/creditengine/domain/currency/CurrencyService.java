package br.com.srm.creditengine.domain.currency;

import br.com.srm.creditengine.domain.exception.ResourceNotFoundException;
import br.com.srm.creditengine.infrastructure.currency.CurrencyRepository;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CurrencyService {

    private final CurrencyRepository currencies;

    CurrencyService(CurrencyRepository currencies) {
        this.currencies = currencies;
    }

    @Transactional(readOnly = true)
    public List<Currency> listSupported() {
        return currencies.findAllByOrderByCodeAsc();
    }

    @Transactional(readOnly = true)
    public Currency require(String code) {
        String normalized = code.trim().toUpperCase(Locale.ROOT);
        return currencies.findById(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Moeda", normalized));
    }
}
