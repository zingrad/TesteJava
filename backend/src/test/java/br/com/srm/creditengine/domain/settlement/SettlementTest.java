package br.com.srm.creditengine.domain.settlement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.srm.creditengine.domain.assignor.Assignor;
import br.com.srm.creditengine.domain.currency.Currency;
import br.com.srm.creditengine.domain.currency.ExchangeRate;
import br.com.srm.creditengine.domain.currency.RateSource;
import br.com.srm.creditengine.domain.exception.ConflictException;
import br.com.srm.creditengine.domain.pricing.PricedBatch;
import br.com.srm.creditengine.domain.pricing.PricedReceivable;
import br.com.srm.creditengine.domain.pricing.ReceivableType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

class SettlementTest {

    private static final OffsetDateTime NOW = OffsetDateTime.of(2026, 3, 10, 12, 0, 0, 0, ZoneOffset.UTC);
    private static final LocalDate TODAY = NOW.toLocalDate();
    private static final String INVOICE = "MERCANTILE_INVOICE";

    @Test
    void startsPendingWithoutASettlementMoment() {
        Settlement settlement = settlement(false);

        assertThat(settlement.status()).isEqualTo(SettlementStatus.PENDING);
        assertThat(settlement.settledAt()).isNull();
        assertThat(settlement.requestedAt()).isEqualTo(NOW);
    }

    @Test
    void carriesTheItemsOfTheBatch() {
        Settlement settlement = settlement(false);

        assertThat(settlement.items()).hasSize(1);
        assertThat(settlement.items().getFirst().documentNumber()).isEqualTo("DUP-1");
        assertThat(settlement.items().getFirst().presentValue()).isEqualByComparingTo("9518.14");
    }

    @Test
    void aSingleCurrencyOperationStoresNoExchangeRate() {
        assertThat(settlement(false).exchangeRateValue()).isNull();
    }

    @Test
    void aCrossCurrencyOperationLocksTheRateItUsed() {
        assertThat(settlement(true).exchangeRateValue()).isEqualByComparingTo("0.18450000");
    }

    @Test
    void settlingStampsTheMomentAndMovesTheStatus() {
        Settlement settlement = settlement(false);

        settlement.settle(NOW.plusHours(2));

        assertThat(settlement.status()).isEqualTo(SettlementStatus.SETTLED);
        assertThat(settlement.settledAt()).isEqualTo(NOW.plusHours(2));
    }

    @Test
    void anAlreadySettledOperationCannotBeSettledAgain() {
        Settlement settlement = settlement(false);
        settlement.settle(NOW);

        assertThatThrownBy(() -> settlement.settle(NOW.plusHours(1)))
                .isInstanceOf(ConflictException.class)
                .extracting(thrown -> ((ConflictException) thrown).code())
                .isEqualTo("SETTLEMENT_NOT_PENDING");
    }

    @Test
    void aSettledOperationCannotBeCancelled() {
        Settlement settlement = settlement(false);
        settlement.settle(NOW);

        assertThatThrownBy(settlement::cancel)
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("liquidada");
    }

    @Test
    void aCancelledOperationCannotBeSettled() {
        Settlement settlement = settlement(false);
        settlement.cancel();

        assertThat(settlement.status()).isEqualTo(SettlementStatus.CANCELLED);
        assertThatThrownBy(() -> settlement.settle(NOW))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("cancelada");
    }

    @Test
    void cancellingLeavesNoSettlementMoment() {
        Settlement settlement = settlement(false);

        settlement.cancel();

        assertThat(settlement.settledAt()).isNull();
    }

    private static Settlement settlement(boolean crossCurrency) {
        PricedReceivable item = new PricedReceivable(
                "DUP-1", INVOICE, new BigDecimal("10000.00"), TODAY.minusDays(5), TODAY.plusDays(60),
                60, new BigDecimal("0.015"), new BigDecimal("0.025"),
                new BigDecimal("9518.14"), crossCurrency ? new BigDecimal("1756.10") : new BigDecimal("9518.14"));

        PricedBatch batch = new PricedBatch(
                TODAY, "BRL", crossCurrency ? "USD" : "BRL", new BigDecimal("0.01"),
                crossCurrency ? new BigDecimal("0.18450000") : BigDecimal.ONE,
                crossCurrency ? rate() : null,
                new BigDecimal("10000.00"), new BigDecimal("9518.14"), item.netAmount(),
                List.of(item));

        return new Settlement("OP-1", assignor(), currency("BRL"),
                currency(crossCurrency ? "USD" : "BRL"), batch, Map.of(INVOICE, receivableType()), NOW);
    }

    private static ExchangeRate rate() {
        return new ExchangeRate(
                currency("BRL"), currency("USD"), new BigDecimal("0.18450000"), NOW,
                RateSource.MANUAL);
    }

    private static Assignor assignor() {
        Assignor assignor = BeanUtils.instantiateClass(Assignor.class);
        ReflectionTestUtils.setField(assignor, "id", 1L);
        ReflectionTestUtils.setField(assignor, "taxId", "11222333000181");
        ReflectionTestUtils.setField(assignor, "legalName", "Metalúrgica Aurora LTDA");
        return assignor;
    }

    private static Currency currency(String code) {
        Currency currency = BeanUtils.instantiateClass(Currency.class);
        ReflectionTestUtils.setField(currency, "code", code);
        ReflectionTestUtils.setField(currency, "name", code);
        ReflectionTestUtils.setField(currency, "minorUnit", (short) 2);
        return currency;
    }

    private static ReceivableType receivableType() {
        ReceivableType type = BeanUtils.instantiateClass(ReceivableType.class);
        ReflectionTestUtils.setField(type, "code", INVOICE);
        ReflectionTestUtils.setField(type, "name", "Duplicata mercantil");
        ReflectionTestUtils.setField(type, "monthlySpread", new BigDecimal("0.015"));
        ReflectionTestUtils.setField(type, "active", true);
        return type;
    }
}
