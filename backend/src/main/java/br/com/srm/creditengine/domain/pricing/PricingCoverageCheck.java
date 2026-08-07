package br.com.srm.creditengine.domain.pricing;

import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Um tipo de recebivel cadastrado sem estrategia correspondente so seria descoberto na primeira
 * operacao que o usasse. Falhar na subida troca uma precificacao recusada em producao por um erro
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
