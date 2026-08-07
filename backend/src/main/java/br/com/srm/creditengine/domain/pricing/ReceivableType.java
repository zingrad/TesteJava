package br.com.srm.creditengine.domain.pricing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "receivable_type")
public class ReceivableType {

    @Id
    @Column(length = 40)
    private String code;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(name = "monthly_spread", nullable = false, precision = 9, scale = 6)
    private BigDecimal monthlySpread;

    @Column(nullable = false)
    private boolean active;

    protected ReceivableType() {
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public BigDecimal monthlySpread() {
        return monthlySpread;
    }

    public boolean active() {
        return active;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof ReceivableType type && Objects.equals(code, type.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }
}
