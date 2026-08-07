import type { SettlementSummaryRow } from '../../api/types'
import { money } from '../../lib/format'

interface Props {
  summary: SettlementSummaryRow[] | undefined
}

/**
 * Uma linha por par de moedas. Consolidar tudo num número só exigiria converter valores de moedas
 * diferentes num câmbio arbitrário — o extrato não inventa cotação que a operação não usou.
 */
export function SummaryStrip({ summary }: Props) {
  if (!summary || summary.length === 0) {
    return null
  }

  return (
    <div className="summary-strip">
      {summary.map((row) => (
        <div className="summary-card" key={`${row.faceCurrency}-${row.paymentCurrency}`}>
          <span className="summary-pair">
            {row.faceCurrency} &rarr; {row.paymentCurrency}
          </span>
          <span className="summary-main">{money(row.totalNetAmount, row.paymentCurrency)}</span>
          <span className="muted tiny">
            {row.operations} {row.operations === 1 ? 'operação' : 'operações'} &middot;{' '}
            {row.receivables} {row.receivables === 1 ? 'título' : 'títulos'} &middot; face{' '}
            {money(row.totalFaceValue, row.faceCurrency)}
          </span>
        </div>
      ))}
    </div>
  )
}
