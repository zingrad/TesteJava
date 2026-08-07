import type { Assignor, Currency, ReportFilters, SettlementStatus } from '../../api/types'

interface Props {
  filters: ReportFilters
  currencies: Currency[]
  assignors: Assignor[]
  hasActiveFilters: boolean
  onChange: (patch: Partial<ReportFilters>) => void
  onClear: () => void
}

const STATUS_OPTIONS: { value: SettlementStatus; label: string }[] = [
  { value: 'SETTLED', label: 'Liquidada' },
  { value: 'PENDING', label: 'Pendente' },
  { value: 'CANCELLED', label: 'Cancelada' },
]

export function StatementFilters({
  filters,
  currencies,
  assignors,
  hasActiveFilters,
  onChange,
  onClear,
}: Props) {
  return (
    <div className="card">
      <div className="card-title">
        <h2>Filtros</h2>
        {hasActiveFilters && (
          <button type="button" onClick={onClear}>
            Limpar
          </button>
        )}
      </div>

      <div className="filter-fields">
        <div className="field">
          <label htmlFor="from">Liquidada a partir de</label>
          <input
            id="from"
            type="date"
            value={filters.from ?? ''}
            onChange={(event) => onChange({ from: event.target.value || undefined })}
          />
        </div>

        <div className="field">
          <label htmlFor="to">Até</label>
          <input
            id="to"
            type="date"
            value={filters.to ?? ''}
            onChange={(event) => onChange({ to: event.target.value || undefined })}
          />
        </div>

        <div className="field">
          <label htmlFor="assignor-filter">Cedente</label>
          <select
            id="assignor-filter"
            value={filters.assignorTaxId ?? ''}
            onChange={(event) => onChange({ assignorTaxId: event.target.value || undefined })}
          >
            <option value="">Todos</option>
            {assignors.map((assignor) => (
              <option key={assignor.id} value={assignor.taxId}>
                {assignor.legalName}
              </option>
            ))}
          </select>
        </div>

        <div className="field">
          <label htmlFor="currency-filter">Moeda de pagamento</label>
          <select
            id="currency-filter"
            value={filters.paymentCurrency ?? ''}
            onChange={(event) => onChange({ paymentCurrency: event.target.value || undefined })}
          >
            <option value="">Todas</option>
            {currencies.map((currency) => (
              <option key={currency.code} value={currency.code}>
                {currency.code}
              </option>
            ))}
          </select>
        </div>

        <div className="field">
          <label htmlFor="status-filter">Status</label>
          <select
            id="status-filter"
            value={filters.status ?? ''}
            onChange={(event) =>
              onChange({ status: (event.target.value || undefined) as SettlementStatus | undefined })
            }
          >
            <option value="">Todos</option>
            {STATUS_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {(filters.from || filters.to) && (
        <p className="muted filter-hint">
          O período filtra pela data de liquidação, então operações pendentes ficam de fora.
        </p>
      )}
    </div>
  )
}
