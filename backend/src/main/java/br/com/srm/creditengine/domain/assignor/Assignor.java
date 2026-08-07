package br.com.srm.creditengine.domain.assignor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Table(name = "assignor")
public class Assignor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tax_id", nullable = false, length = 14)
    private String taxId;

    @Column(name = "legal_name", nullable = false, length = 120)
    private String legalName;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Assignor() {
    }

    public Long id() {
        return id;
    }

    public String taxId() {
        return taxId;
    }

    public String legalName() {
        return legalName;
    }

    public OffsetDateTime createdAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Assignor assignor && id != null && Objects.equals(id, assignor.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
