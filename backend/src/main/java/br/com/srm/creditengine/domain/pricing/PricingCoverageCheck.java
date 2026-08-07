package br.com.srm.creditengine.domain.pricing;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Um tipo de recebível cadastrado sem estratégia correspondente só seria descoberto na primeira
 * operação que o usasse. Falhar na subida troca uma precificação recusada em produção por um erro
 * de deploy.
 */
@Component
class PricingCoverageCheck implements ApplicationRunner {

    private final ReceivableTypeService receivableTypes;
    private final PricingStrategyRegistry strategies;

    PricingCoverageCheck(ReceivableTypeService receivableTypes, PricingStrategyRegistry strategies) {
        this.receivableTypes = receivableTypes;
        this.strategies = strategies;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> uncovered = receivableTypes.listActive().stream()
                .map(ReceivableType::code)
                .filter(code -> !strategies.coveredTypes().contains(code))
                .toList();

        if (!uncovered.isEmpty()) {
            throw new IllegalStateException(
                    "Active receivable types without a pricing strategy: " + uncovered);
        }
    }
}
