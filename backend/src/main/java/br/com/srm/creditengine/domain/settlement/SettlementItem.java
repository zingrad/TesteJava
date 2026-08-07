package br.com.srm.creditengine.domain.settlement;

import br.com.srm.creditengine.domain.pricing.PricedReceivable;
import br.com.srm.creditengine.domain.pricing.ReceivableType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "settlement_item")
public class SettlementItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "settlement_id", nullable = false)
    private Settlement settlement;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receivable_type_code", nullable = false)
    private ReceivableType receivableType;

    @Column(name = "document_number", nullable = false, length = 40)
    private String documentNumber;

    @Column(name = "face_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal faceValue;

    @Column(name = "issue_date", nullable = false)
    private LocalDate issueDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "term_days", nullable = false)
    private int termDays;

    @Column(name = "applied_spread", nullable = false, precision = 9, scale = 6)
    private BigDecimal appliedSpread;

    @Column(name = "present_value", nullable = false, precision = 18, scale = 2)
    private BigDecimal presentValue;

    @Column(name = "net_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal netAmount;

    protected SettlementItem() {
    }

    SettlementItem(Settlement settlement, ReceivableType receivableType, PricedReceivable priced) {
        this.settlement = settlement;
        this.receivableType = receivableType;
        this.documentNumber = priced.documentNumber();
        this.faceValue = priced.faceValue();
        this.issueDate = priced.issueDate();
        this.dueDate = priced.dueDate();
        this.termDays = priced.termDays();
        this.appliedSpread = priced.appliedSpread();
        this.presentValue = priced.presentValue();
        this.netAmount = priced.netAmount();
    }

    public Long id() {
        return id;
    }

    public ReceivableType receivableType() {
        return receivableType;
    }

    public String documentNumber() {
        return documentNumber;
    }

    public BigDecimal faceValue() {
        return faceValue;
    }

    public LocalDate issueDate() {
        return issueDate;
    }

    public LocalDate dueDate() {
        return dueDate;
    }

    public int termDays() {
        return termDays;
    }

    public BigDecimal appliedSpread() {
        return appliedSpread;
    }

    public BigDecimal presentValue() {
        return presentValue;
    }

    public BigDecimal netAmount() {
        return netAmount;
    }
}
