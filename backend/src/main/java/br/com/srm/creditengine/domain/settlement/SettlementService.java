package br.com.srm.creditengine.domain.settlement;

import br.com.srm.creditengine.domain.assignor.Assignor;
import br.com.srm.creditengine.domain.assignor.AssignorService;
import br.com.srm.creditengine.domain.currency.CurrencyService;
import br.com.srm.creditengine.domain.exception.ConflictException;
import br.com.srm.creditengine.domain.exception.ResourceNotFoundException;
import br.com.srm.creditengine.domain.pricing.PricedBatch;
import br.com.srm.creditengine.domain.pricing.PricedReceivable;
import br.com.srm.creditengine.domain.pricing.PricingService;
import br.com.srm.creditengine.domain.pricing.ReceivableType;
import br.com.srm.creditengine.domain.pricing.ReceivableTypeService;
import br.com.srm.creditengine.infrastructure.settlement.SettlementRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementService {

    private final SettlementRepository settlements;
    private final AssignorService assignors;
    private final CurrencyService currencies;
    private final ReceivableTypeService receivableTypes;
    private final PricingService pricing;
    private final Clock clock;

    SettlementService(SettlementRepository settlements, AssignorService assignors,
            CurrencyService currencies, ReceivableTypeService receivableTypes,
            PricingService pricing, Clock clock) {
        this.settlements = settlements;
        this.assignors = assignors;
        this.currencies = currencies;
        this.receivableTypes = receivableTypes;
        this.pricing = pricing;
        this.clock = clock;
    }

    /**
     * Precifica e registra o lote na mesma transação. Se qualquer título for recusado, nada e
     * gravado: o lote não existe pela metade.
     */
    @Transactional
    public Settlement register(SettlementRegistration registration) {
        String reference = normalize(registration.reference());
        if (settlements.existsByReference(reference)) {
            throw new ConflictException("SETTLEMENT_ALREADY_REGISTERED",
                    "Já existe uma operação registrada com a referência %s.".formatted(reference));
        }

        Assignor assignor = assignors.requireByTaxId(registration.assignorTaxId());
        PricedBatch batch = pricing.price(registration.toPricingOrder());

        Map<String, ReceivableType> types = batch.items().stream()
                .map(PricedReceivable::receivableTypeCode)
                .distinct()
                .collect(Collectors.toMap(Function.identity(), receivableTypes::requireActive));

        Settlement settlement = new Settlement(
                reference,
                assignor,
                currencies.require(batch.faceCurrency()),
                currencies.require(batch.paymentCurrency()),
                batch,
                types,
                now());

        return settlements.save(settlement);
    }

    /**
     * A transição usa o lock otimista da coluna {@code version}: duas chamadas simultaneas para a
     * mesma operação disputam o mesmo UPDATE, e a perdedora recebe conflito em vez de liquidar duas vezes.
     */
    @Transactional
    public Settlement settle(String reference) {
        Settlement settlement = require(reference);
        settlement.settle(now());
        return settlement;
    }

    @Transactional
    public Settlement cancel(String reference) {
        Settlement settlement = require(reference);
        settlement.cancel();
        return settlement;
    }

    @Transactional(readOnly = true)
    public Settlement findByReference(String reference) {
        return require(reference);
    }

    private Settlement require(String reference) {
        String normalized = normalize(reference);
        return settlements.findByReference(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("Operação", normalized));
    }

    /**
     * A referência é a chave de idempotência do cliente, então precisa casar na gravação e na busca.
     */
    private static String normalize(String reference) {
        return reference.trim().toUpperCase(Locale.ROOT);
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
