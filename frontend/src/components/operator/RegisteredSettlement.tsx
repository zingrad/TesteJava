import type { Settlement } from '../../api/types'
import { money } from '../../lib/format'
import { StatusBadge } from '../StatusBadge'

interface Props {
  settlement: Settlement
  onSettle: () => void
  onNewBatch: () => void
  isSettling: boolean
}

export function RegisteredSettlement({ settlement, onSettle, onNewBatch, isSettling }: Props) {
  const settled = settlement.status === 'SETTLED'

  return (
    <div className="card registered">
      <div className="card-title">
        <h2>
          Operacao {settlement.reference} <StatusBadge status={settlement.status} />
        </h2>
        <p>{settlement.assignorLegalName}</p>
      </div>

      <p>
        Liquido ao cedente:{' '}
        <strong>{money(settlement.totalNetAmount, settlement.paymentCurrency)}</strong> sobre{' '}
        {settlement.items.length} {settlement.items.length === 1 ? 'titulo' : 'titulos'}.
      </p>

      <div className="actions">
        <button type="button" className="primary" onClick={onSettle} disabled={settled || isSettling}>
          {settled ? 'Liquidada' : isSettling ? 'Liquidando...' : 'Liquidar agora'}
        </button>
        <button type="button" onClick={onNewBatch}>
          Novo lote
        </button>
      </div>
    </div>
  )
}
