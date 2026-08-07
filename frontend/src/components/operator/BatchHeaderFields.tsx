import type { Assignor, Currency } from '../../api/types'
import type { DraftHeader } from '../../hooks/useSettlementDraft'

interface Props {
  header: DraftHeader
  currencies: Currency[]
  assignors: Assignor[]
  violationFor: (field: string) => string | undefined
  onChange: <K extends keyof DraftHeader>(field: K, value: DraftHeader[K]) => void
}

export function BatchHeaderFields({ header, currencies, assignors, violationFor, onChange }: Props) {
  const referenceError = violationFor('reference')
  const rateError = violationFor('baseMonthlyRate')

  return (
    <div className="card">
      <div className="card-title">
        <h2>Dados da operação</h2>
      </div>

      <div className="header-fields">
        <div className="field">
          <label htmlFor="reference">Referência</label>
          <input
            id="reference"
            value={header.reference}
            placeholder="OP-2026-0001"
            aria-invalid={referenceError ? 'true' : undefined}
            onChange={(event) => onChange('reference', event.target.value)}
          />
          {referenceError && <span className="error">{referenceError}</span>}
        </div>

        <div className="field">
          <label htmlFor="assignor">Cedente</label>
          <select
            id="assignor"
            value={header.assignorTaxId}
            onChange={(event) => onChange('assignorTaxId', event.target.value)}
          >
            {assignors.map((assignor) => (
              <option key={assignor.id} value={assignor.taxId}>
                {assignor.legalName}
              </option>
            ))}
          </select>
        </div>

        <div className="field">
          <label htmlFor="faceCurrency">Moeda dos títulos</label>
          <select
            id="faceCurrency"
            value={header.faceCurrency}
            onChange={(event) => onChange('faceCurrency', event.target.value)}
          >
            {currencies.map((currency) => (
              <option key={currency.code} value={currency.code}>
                {currency.code} — {currency.name}
              </option>
            ))}
          </select>
        </div>

        <div className="field">
          <label htmlFor="paymentCurrency">Moeda de pagamento</label>
          <select
            id="paymentCurrency"
            value={header.paymentCurrency}
            onChange={(event) => onChange('paymentCurrency', event.target.value)}
          >
            {currencies.map((currency) => (
              <option key={currency.code} value={currency.code}>
                {currency.code} — {currency.name}
              </option>
            ))}
          </select>
        </div>

        <div className="field">
          <label htmlFor="baseRate">Taxa base (% a.m.)</label>
          <input
            id="baseRate"
            type="number"
            min="0"
            max="100"
            step="0.01"
            value={header.baseMonthlyRatePercent}
            aria-invalid={rateError ? 'true' : undefined}
            onChange={(event) => onChange('baseMonthlyRatePercent', event.target.value)}
          />
          {rateError && <span className="error">{rateError}</span>}
        </div>
      </div>
    </div>
  )
}
