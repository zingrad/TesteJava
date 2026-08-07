package br.com.srm.creditengine.domain.pricing;

import br.com.srm.creditengine.domain.exception.BusinessRuleException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PricingStrategyRegistry {

    private final Map<String, PricingStrategy> byReceivableType;

    PricingStrategyRegistry(List<PricingStrategy> strategies) {
        this.byReceivableType = strategies.stream().collect(Collectors.toUnmodifiableMap(
                PricingStrategy::receivableType,
                Function.identity(),
                (first, second) -> {
                    throw new IllegalStateException(
                            "More than one pricing strategy declares the receivable type "
                                    + first.receivableType());
                }));
    }

    public PricingStrategy forType(String receivableTypeCode) {
        PricingStrategy strategy = byReceivableType.get(receivableTypeCode);
        if (strategy == null) {
            throw new BusinessRuleException("PRICING_STRATEGY_MISSING",
                    "Não ha regra de precificação para o tipo de recebível %s.".formatted(receivableTypeCode));
        }
        return strategy;
    }

    Set<String> coveredTypes() {
        return byReceivableType.keySet();
    }
}
