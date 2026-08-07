package br.com.srm.creditengine.api.pricing;

import br.com.srm.creditengine.domain.pricing.PricedBatch;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Lote precificado, sem qualquer efeito de persistencia")
public record PricingSimulationResponse(

        @Schema(description = "Data usada como referencia para o prazo de cada titulo")
        LocalDate valuationDate,

        String faceCurrency,
        String paymentCurrency,
        BigDecimal baseMonthlyRate,

        @Schema(description = "Cotacao aplicada; 1 quando a operacao nao e cross-currency")
        BigDecimal exchangeRate,

        boolean crossCurrency,
        BigDecimal totalFaceValue,

        @Schema(description = "Soma dos valores presentes, na moeda dos titulos")
        BigDecimal totalPresentValue,

        @Schema(description = "Soma dos valores liquidos, na moeda de pagamento")
        BigDecimal totalNetAmount,

        List<PricedReceivableResponse> items) {

    static PricingSimulationResponse from(PricedBatch batch) {
        return new PricingSimulationResponse(
                batch.valuationDate(),
                batch.faceCurrency(),
                batch.paymentCurrency(),
                batch.baseMonthlyRate(),
                batch.exchangeRateValue(),
                batch.crossCurrency(),
                batch.totalFaceValue(),
                batch.totalPresentValue(),
                batch.totalNetAmount(),
                batch.items().stream().map(PricedReceivableResponse::from).toList());
    }
}
