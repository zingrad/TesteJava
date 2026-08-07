package br.com.srm.creditengine.infrastructure.currency;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

@Component
class SimulatedExchangeRateProvider implements ExchangeRateProvider {

    private static final BigDecimal REFERENCE_USD_BRL = new BigDecimal("5.42");
    private static final BigDecimal MAX_DRIFT = new BigDecimal("0.005");
    private static final MathContext INVERSION = new MathContext(12, RoundingMode.HALF_EVEN);

    @Override
    public List<ProviderQuote> latestQuotes() {
        BigDecimal usdBrl = drift(REFERENCE_USD_BRL);
        BigDecimal brlUsd = BigDecimal.ONE.divide(usdBrl, INVERSION).setScale(8, RoundingMode.HALF_EVEN);

        return List.of(
                new ProviderQuote("USD", "BRL", usdBrl),
                new ProviderQuote("BRL", "USD", brlUsd));
    }

    /**
     * {@code ThreadLocalRandom} em vez de {@code RandomGenerator.getDefault()}: o segundo depende do
     * módulo {@code jdk.random}, que a imagem JRE usada no container não traz. Passava no ambiente de
     * desenvolvimento, onde roda o JDK completo, e derrubava a aplicação no Docker.
     */
    private BigDecimal drift(BigDecimal reference) {
        BigDecimal factor = BigDecimal.ONE.add(
                MAX_DRIFT.multiply(BigDecimal.valueOf(ThreadLocalRandom.current().nextDouble(-1.0, 1.0))));
        return reference.multiply(factor).setScale(8, RoundingMode.HALF_EVEN);
    }
}
