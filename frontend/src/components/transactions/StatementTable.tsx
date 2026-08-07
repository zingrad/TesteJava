import type { ReportFilters, ReportSort, SettlementReportRow } from '../../api/types'
import { dateTime, money, taxId } from '../../lib/format'
import { StatusBadge } from '../StatusBadge'

interface Props {
  rows: SettlementReportRow[]
  filters: ReportFilters
  isFetching: boolean
  onSort: (sort: ReportSort) => void
}

const AMOUNT_SORTS: ReportSort[] = ['NET_AMOUNT', 'FACE_VALUE']

export function StatementTable({ rows, filters, isFetching, onSort }: Props) {
  if (rows.length === 0) {
    return <div className="empty-state">Nenhuma operação para este recorte.</div>
  }

  // Ordenar por valor compara o número cru, e o extrato mistura moedas. Sem um recorte de moeda,
  // um lote em USD aparece abaixo de outro em BRL de valor econômico menor.
  const mixedCurrencyRanking =
    AMOUNT_SORTS.includes(filters.sort ?? 'SETTLED_AT') &&
    new Set(rows.map((row) => row.paymentCurrency)).size > 1

  return (
    <>
      {mixedCurrencyRanking && (
        <p className="notice info ranking-warning">
          A ordenação por valor compara números de moedas diferentes. Filtre por moeda de pagamento
          para um ranking com significado econômico.
        </p>
      )}
      <div className={isFetching ? 'table-scroll stale' : 'table-scroll'}>
      <table>
        <thead>
          <tr>
            <th>Referência</th>
            <SortableHeader sort="ASSIGNOR" filters={filters} onSort={onSort}>
              Cedente
            </SortableHeader>
            <th>Status</th>
            <th className="numeric">Títulos</th>
            <SortableHeader sort="FACE_VALUE" filters={filters} onSort={onSort} numeric>
              Valor de face
            </SortableHeader>
            <th className="numeric">Valor presente</th>
            <SortableHeader sort="NET_AMOUNT" filters={filters} onSort={onSort} numeric>
              Liquido
            </SortableHeader>
            <SortableHeader sort="SETTLED_AT" filters={filters} onSort={onSort}>
              Liquidada em
            </SortableHeader>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.reference}>
              <td>
                <code>{row.reference}</code>
              </td>
              <td>
                {row.assignorLegalName}
                <br />
                <span className="muted tiny">{taxId(row.assignorTaxId)}</span>
              </td>
              <td>
                <StatusBadge status={row.status} />
              </td>
              <td className="numeric">{row.itemCount}</td>
              <td className="numeric">{money(row.totalFaceValue, row.faceCurrency)}</td>
              <td className="numeric">{money(row.totalPresentValue, row.faceCurrency)}</td>
              <td className="numeric">
                {money(row.totalNetAmount, row.paymentCurrency)}
                {row.exchangeRate && (
                  <>
                    <br />
                    <span className="muted tiny">câmbio {row.exchangeRate}</span>
                  </>
                )}
              </td>
              <td>{dateTime(row.settledAt)}</td>
            </tr>
          ))}
          </tbody>
        </table>
      </div>
    </>
  )
}

function SortableHeader({
  sort,
  filters,
  onSort,
  numeric,
  children,
}: {
  sort: ReportSort
  filters: ReportFilters
  onSort: (sort: ReportSort) => void
  numeric?: boolean
  children: React.ReactNode
}) {
  const active = filters.sort === sort
  const ascending = filters.direction === 'ASC'

  return (
    <th className={numeric ? 'numeric' : undefined} aria-sort={active ? (ascending ? 'ascending' : 'descending') : 'none'}>
      <button type="button" className="sort-header" onClick={() => onSort(sort)}>
        {children}
        <span className={active ? 'sort-arrow active' : 'sort-arrow'}>
          {active ? (ascending ? '↑' : '↓') : '↕'}
        </span>
      </button>
    </th>
  )
}
