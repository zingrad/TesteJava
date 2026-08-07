package br.com.srm.creditengine.domain.currency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "currency")
public class Currency {

    @Id
    @Column(length = 3)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String code;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(name = "minor_unit", nullable = false)
    private short minorUnit;

    protected Currency() {
    }

    public String code() {
        return code;
    }

    public String name() {
        return name;
    }

    public short minorUnit() {
        return minorUnit;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Currency currency && Objects.equals(code, currency.code);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code);
    }
}
