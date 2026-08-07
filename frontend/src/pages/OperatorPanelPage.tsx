import { BatchForm } from '../components/operator/BatchForm'
import { ErrorNotice } from '../components/ErrorNotice'
import { useAssignors, useCurrencies, useReceivableTypes } from '../hooks/useReferenceData'

export function OperatorPanelPage() {
  const currencies = useCurrencies()
  const receivableTypes = useReceivableTypes()
  const assignors = useAssignors()

  const error = currencies.error ?? receivableTypes.error ?? assignors.error
  const loading = currencies.isPending || receivableTypes.isPending || assignors.isPending
  const ready = currencies.data && receivableTypes.data && assignors.data

  return (
    <>
      <div className="card-title">
        <h1>Painel do operador</h1>
        <p>Simulacao em tempo real e registro da operacao</p>
      </div>

      <ErrorNotice error={error} />

      {loading && <div className="card empty-state">Carregando cadastro...</div>}

      {ready && (
        <BatchForm
          currencies={currencies.data}
          receivableTypes={receivableTypes.data}
          assignors={assignors.data}
        />
      )}
    </>
  )
}
