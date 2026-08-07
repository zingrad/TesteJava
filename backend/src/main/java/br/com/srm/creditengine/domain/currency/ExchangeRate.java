package br.com.srm.creditengine.domain.currency;

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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;

@Entity
@Table(name = "exchange_rate")
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "base_currency", nullable = false)
    private Currency baseCurrency;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_currency", nullable = false)
    private Currency quoteCurrency;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal rate;

    @Column(name = "effective_at", nullable = false)
    private OffsetDateTime effectiveAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private RateSource source;

    protected ExchangeRate() {
    }

    public ExchangeRate(Currency baseCurrency, Currency quoteCurrency, BigDecimal rate,
            OffsetDateTime effectiveAt, RateSource source) {
        this.baseCurrency = baseCurrency;
        this.quoteCurrency = quoteCurrency;
        this.rate = rate;
        this.effectiveAt = effectiveAt;
        this.source = source;
    }

    public Long id() {
        return id;
    }

    public Currency baseCurrency() {
        return baseCurrency;
    }

    public Currency quoteCurrency() {
        return quoteCurrency;
    }

    public BigDecimal rate() {
        return rate;
    }

    public OffsetDateTime effectiveAt() {
        return effectiveAt;
    }

    public RateSource source() {
        return source;
    }

    /**
     * Converte na escala da moeda de cotação, com arredondamento bancário para não introduzir
     * viés sistemático ao longo de um grande volume de conversoes.
     */
    public BigDecimal convert(BigDecimal amount) {
        return amount.multiply(rate).setScale(quoteCurrency.minorUnit(), RoundingMode.HALF_EVEN);
    }
}
