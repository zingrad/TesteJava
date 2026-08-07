import type { SettlementStatus } from '../api/types'

const LABELS: Record<SettlementStatus, string> = {
  PENDING: 'Pendente',
  SETTLED: 'Liquidada',
  CANCELLED: 'Cancelada',
}

export function StatusBadge({ status }: { status: SettlementStatus }) {
  return <span className={`badge ${status.toLowerCase()}`}>{LABELS[status]}</span>
}
