import { ErrorNotice } from '../components/ErrorNotice'
import { useCurrencies, useReceivableTypes } from '../hooks/useReferenceData'
import { monthlyRate } from '../lib/format'

export function OperatorPanelPage() {
  const currencies = useCurrencies()
  const receivableTypes = useReceivableTypes()

  return (
    <>
      <div className="card-title">
        <h1>Painel do operador</h1>
      </div>

      <ErrorNotice error={currencies.error ?? receivableTypes.error} />

      <section className="card">
        <div className="card-title">
          <h2>Parametros da mesa</h2>
          <p>Cadastro carregado da API</p>
        </div>

        {currencies.isPending || receivableTypes.isPending ? (
          <p className="muted">Carregando cadastro...</p>
        ) : (
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Tipo de recebivel</th>
                  <th>Codigo</th>
                  <th className="numeric">Spread</th>
                </tr>
              </thead>
              <tbody>
                {receivableTypes.data?.map((type) => (
                  <tr key={type.code}>
                    <td>{type.name}</td>
                    <td>
                      <code>{type.code}</code>
                    </td>
                    <td className="numeric">{monthlyRate(type.monthlySpread)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        <p className="muted" style={{ marginBottom: 0 }}>
          Moedas suportadas: {currencies.data?.map((currency) => currency.code).join(', ') || '—'}
        </p>
      </section>

      <section className="card">
        <div className="empty-state">O formulario de simulacao entra no proximo passo.</div>
      </section>
    </>
  )
}
