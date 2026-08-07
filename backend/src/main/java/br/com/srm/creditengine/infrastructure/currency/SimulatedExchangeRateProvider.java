package br.com.srm.creditengine.infrastructure.currency;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;
import java.util.random.RandomGenerator;
import org.springframework.stereotype.Component;

@Component
class SimulatedExchangeRateProvider implements ExchangeRateProvider {

    private static final BigDecimal REFERENCE_USD_BRL = new BigDecimal("5.42");
    private static final BigDecimal MAX_DRIFT = new BigDecimal("0.005");
    private static final MathContext INVERSION = new MathContext(12, RoundingMode.HALF_EVEN);

    private final RandomGenerator random = RandomGenerator.getDefault();

    @Override
    public List<ProviderQuote> latestQuotes() {
        BigDecimal usdBrl = drift(REFERENCE_USD_BRL);
        BigDecimal brlUsd = BigDecimal.ONE.divide(usdBrl, INVERSION).setScale(8, RoundingMode.HALF_EVEN);

        return List.of(
                new ProviderQuote("USD", "BRL", usdBrl),
                new ProviderQuote("BRL", "USD", brlUsd));
    }

    private BigDecimal drift(BigDecimal reference) {
        BigDecimal factor = BigDecimal.ONE.add(
                MAX_DRIFT.multiply(BigDecimal.valueOf(random.nextDouble(-1.0, 1.0))));
        return reference.multiply(factor).setScale(8, RoundingMode.HALF_EVEN);
    }
}
