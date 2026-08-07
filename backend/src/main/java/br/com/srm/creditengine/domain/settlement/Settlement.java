package br.com.srm.creditengine.domain.settlement;

import br.com.srm.creditengine.domain.assignor.Assignor;
import br.com.srm.creditengine.domain.currency.Currency;
import br.com.srm.creditengine.domain.currency.ExchangeRate;
import br.com.srm.creditengine.domain.exception.ConflictException;
import br.com.srm.creditengine.domain.pricing.PricedBatch;
import br.com.srm.creditengine.domain.pricing.ReceivableType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Raiz do agregado de liquidacao. O lote inteiro entra e sai junto: os itens sao gravados em cascata
 * dentro da mesma transacao do cabecalho, e a transicao de estado so acontece por estes metodos,
 * nunca por um setter solto.
 */
@Entity
@Table(name = "settlement")
public class Settlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assignor_id", nullable = false)
    private Assignor assignor;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "face_currency", nullable = false)
    private Currency faceCurrency;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_currency", nullable = false)
    private Currency paymentCurrency;

    @Column(name = "base_monthly_rate", nullable = false, precision = 9, scale = 6)
    private BigDecimal baseMonthlyRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exchange_rate_id")
    private ExchangeRate exchangeRate;

    @Column(name = "exchange_rate_value", precision = 18, scale = 8)
    private BigDecimal exchangeRateValue;

    @Column(name = "total_face_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalFaceValue;

    @Column(name = "total_present_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalPresentValue;

    @Column(name = "total_net_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal totalNetAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status;

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "settled_at")
    private OffsetDateTime settledAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "settlement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SettlementItem> items = new ArrayList<>();

    protected Settlement() {
    }

    public Settlement(String reference, Assignor assignor, Currency faceCurrency, Currency paymentCurrency,
            PricedBatch batch, Map<String, ReceivableType> receivableTypes, OffsetDateTime requestedAt) {

        this.reference = reference;
        this.assignor = assignor;
        this.faceCurrency = faceCurrency;
        this.paymentCurrency = paymentCurrency;
        this.baseMonthlyRate = batch.baseMonthlyRate();
        this.exchangeRate = batch.appliedExchangeRate();
        this.exchangeRateValue = batch.crossCurrency() ? batch.exchangeRateValue() : null;
        this.totalFaceValue = batch.totalFaceValue();
        this.totalPresentValue = batch.totalPresentValue();
        this.totalNetAmount = batch.totalNetAmount();
        this.status = SettlementStatus.PENDING;
        this.requestedAt = requestedAt;

        batch.items().forEach(priced -> items.add(
                new SettlementItem(this, receivableTypes.get(priced.receivableTypeCode()), priced)));
    }

    public void settle(OffsetDateTime moment) {
        requirePending("liquidada");
        this.status = SettlementStatus.SETTLED;
        this.settledAt = moment;
    }

    public void cancel() {
        requirePending("cancelada");
        this.status = SettlementStatus.CANCELLED;
    }

    private void requirePending(String action) {
        if (status != SettlementStatus.PENDING) {
            throw new ConflictException("SETTLEMENT_NOT_PENDING",
                    "A operacao %s esta %s e nao pode ser %s.".formatted(reference, statusLabel(), action));
        }
    }

    private String statusLabel() {
        return switch (status) {
            case PENDING -> "pendente";
            case SETTLED -> "liquidada";
            case CANCELLED -> "cancelada";
        };
    }

    public Long id() {
        return id;
    }

    public String reference() {
        return reference;
    }

    public Assignor assignor() {
        return assignor;
    }

    public Currency faceCurrency() {
        return faceCurrency;
    }

    public Currency paymentCurrency() {
        return paymentCurrency;
    }

    public BigDecimal baseMonthlyRate() {
        return baseMonthlyRate;
    }

    public BigDecimal exchangeRateValue() {
        return exchangeRateValue;
    }

    public BigDecimal totalFaceValue() {
        return totalFaceValue;
    }

    public BigDecimal totalPresentValue() {
        return totalPresentValue;
    }

    public BigDecimal totalNetAmount() {
        return totalNetAmount;
    }

    public SettlementStatus status() {
        return status;
    }

    public OffsetDateTime requestedAt() {
        return requestedAt;
    }

    public OffsetDateTime settledAt() {
        return settledAt;
    }

    public List<SettlementItem> items() {
        return List.copyOf(items);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Settlement settlement && id != null && Objects.equals(id, settlement.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
