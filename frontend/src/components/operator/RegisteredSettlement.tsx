import type { Settlement } from '../../api/types'
import { money } from '../../lib/format'
import { StatusBadge } from '../StatusBadge'

interface Props {
  settlement: Settlement
  onSettle: () => void
  onCancel: () => void
  onNewBatch: () => void
  isSettling: boolean
  isCancelling: boolean
}

const CLOSED_MESSAGE: Record<string, string> = {
  SETTLED: 'Operação liquidada. O valor já foi movimentado e o registro não aceita mais alteração.',
  CANCELLED:
    'Operação cancelada. Nenhum valor foi movimentado, e o registro permanece no extrato para auditoria.',
}

export function RegisteredSettlement({
  settlement,
  onSettle,
  onCancel,
  onNewBatch,
  isSettling,
  isCancelling,
}: Props) {
  const pending = settlement.status === 'PENDING'
  const busy = isSettling || isCancelling

  return (
    <div className="card registered">
      <div className="card-title">
        <h2>
          Operação {settlement.reference} <StatusBadge status={settlement.status} />
        </h2>
        <p>{settlement.assignorLegalName}</p>
      </div>

      <p>
        Líquido ao cedente:{' '}
        <strong>{money(settlement.totalNetAmount, settlement.paymentCurrency)}</strong> sobre{' '}
        {settlement.items.length} {settlement.items.length === 1 ? 'título' : 'títulos'}.
      </p>

      {!pending && <p className="muted">{CLOSED_MESSAGE[settlement.status]}</p>}

      <div className="actions">
        {pending && (
          <>
            <button type="button" className="primary" onClick={onSettle} disabled={busy}>
              {isSettling ? 'Liquidando...' : 'Liquidar agora'}
            </button>
            {/* Cancelar é a saída para um lote registrado por engano. Só existe enquanto a operação
                está pendente: depois de liquidada, o dinheiro já se moveu. */}
            <button type="button" className="danger" onClick={onCancel} disabled={busy}>
              {isCancelling ? 'Cancelando...' : 'Cancelar operação'}
            </button>
          </>
        )}
        <button type="button" onClick={onNewBatch} disabled={busy}>
          Novo lote
        </button>
      </div>
    </div>
  )
}
