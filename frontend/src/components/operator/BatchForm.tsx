import { useState } from 'react'
import { ApiError } from '../../api/ApiError'
import type { Assignor, Currency, ReceivableType, Settlement } from '../../api/types'
import { useSettlementDraft } from '../../hooks/useSettlementDraft'
import {
  usePricingSimulation,
  useRegisterSettlement,
  useSettleSettlement,
} from '../../hooks/useSettlementOperations'
import { ErrorNotice } from '../ErrorNotice'
import { BatchHeaderFields } from './BatchHeaderFields'
import { ReceivableRows } from './ReceivableRows'
import { RegisteredSettlement } from './RegisteredSettlement'
import { SimulationPanel } from './SimulationPanel'

interface Props {
  currencies: Currency[]
  receivableTypes: ReceivableType[]
  assignors: Assignor[]
}

const noViolation = () => undefined

export function BatchForm({ currencies, receivableTypes, assignors }: Props) {
  const draft = useSettlementDraft({
    receivableTypeCode: receivableTypes[0]?.code ?? '',
    faceCurrency: currencies[0]?.code ?? '',
    paymentCurrency: currencies[0]?.code ?? '',
    assignorTaxId: assignors[0]?.taxId ?? '',
  })

  const [registered, setRegistered] = useState<Settlement | null>(null)

  const simulation = usePricingSimulation(registered ? null : draft.pricingRequest)
  const register = useRegisterSettlement()
  const settle = useSettleSettlement()

  const submissionError = register.error ?? settle.error
  const violationFor =
    submissionError instanceof ApiError
      ? (field: string) => submissionError.violationFor(field)
      : noViolation

  const handleRegister = () => {
    if (!draft.settlementRequest) {
      return
    }
    register.mutate(draft.settlementRequest, { onSuccess: setRegistered })
  }

  const handleSettle = () => {
    if (!registered) {
      return
    }
    settle.mutate(registered.reference, { onSuccess: setRegistered })
  }

  const handleNewBatch = () => {
    setRegistered(null)
    register.reset()
    settle.reset()
    draft.reset()
  }

  if (registered) {
    return (
      <>
        <ErrorNotice error={settle.error} />
        <RegisteredSettlement
          settlement={registered}
          onSettle={handleSettle}
          onNewBatch={handleNewBatch}
          isSettling={settle.isPending}
        />
      </>
    )
  }

  return (
    <>
      <ErrorNotice error={submissionError ?? simulation.error} />

      <BatchHeaderFields
        header={draft.header}
        currencies={currencies}
        assignors={assignors}
        violationFor={violationFor}
        onChange={draft.updateHeader}
      />

      <ReceivableRows
        items={draft.items}
        receivableTypes={receivableTypes}
        violationFor={violationFor}
        onChange={draft.updateItem}
        onAdd={draft.addItem}
        onRemove={draft.removeItem}
      />

      <SimulationPanel
        simulation={simulation.data}
        isFetching={simulation.isFetching}
        isIncomplete={draft.pricingRequest === null}
      />

      <div className="card">
        <div className="actions">
          <button
            type="button"
            className="primary"
            onClick={handleRegister}
            disabled={!draft.settlementRequest || register.isPending}
          >
            {register.isPending ? 'Registrando...' : 'Registrar operação'}
          </button>
          {!draft.settlementRequest && (
            <span className="muted">
              Informe a referência e o número de todos os documentos para registrar.
            </span>
          )}
        </div>
      </div>
    </>
  )
}
