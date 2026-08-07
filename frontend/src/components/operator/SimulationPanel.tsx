import type { PricingSimulation } from '../../api/types'
import { date, money, monthlyRate } from '../../lib/format'

interface Props {
  simulation: PricingSimulation | undefined
  isFetching: boolean
  isIncomplete: boolean
}

export function SimulationPanel({ simulation, isFetching, isIncomplete }: Props) {
  return (
    <div className="card">
      <div className="card-title">
        <h2>Simulacao</h2>
        <p>{isFetching ? 'Recalculando...' : simulation ? `Referencia ${date(simulation.valuationDate)}` : ''}</p>
      </div>

      {!simulation ? (
        <div className="empty-state">
          {isIncomplete
            ? 'Preencha valor e datas dos titulos para ver o calculo.'
            : 'Calculando...'}
        </div>
      ) : (
        <div className={isFetching ? 'simulation stale' : 'simulation'}>
          <div className="totals">
            <Total label="Valor de face" value={money(simulation.totalFaceValue, simulation.faceCurrency)} />
            <Total
              label="Valor presente"
              value={money(simulation.totalPresentValue, simulation.faceCurrency)}
              hint={`desagio de ${money(simulation.totalFaceValue - simulation.totalPresentValue, simulation.faceCurrency)}`}
            />
            <Total
              label="Liquido ao cedente"
              value={money(simulation.totalNetAmount, simulation.paymentCurrency)}
              hint={
                simulation.crossCurrency
                  ? `convertido a ${simulation.exchangeRate} ${simulation.faceCurrency}/${simulation.paymentCurrency}`
                  : 'mesma moeda dos titulos'
              }
              strong
            />
          </div>

          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Documento</th>
                  <th className="numeric">Prazo</th>
                  <th className="numeric">Taxa</th>
                  <th className="numeric">Valor de face</th>
                  <th className="numeric">Valor presente</th>
                  <th className="numeric">Liquido</th>
                </tr>
              </thead>
              <tbody>
                {simulation.items.map((item) => (
                  <tr key={item.documentNumber}>
                    <td>{item.documentNumber}</td>
                    <td className="numeric">{item.termDays} dias</td>
                    <td className="numeric">{monthlyRate(item.monthlyDiscountRate)}</td>
                    <td className="numeric">{money(item.faceValue, simulation.faceCurrency)}</td>
                    <td className="numeric">{money(item.presentValue, simulation.faceCurrency)}</td>
                    <td className="numeric">{money(item.netAmount, simulation.paymentCurrency)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  )
}

function Total({
  label,
  value,
  hint,
  strong,
}: {
  label: string
  value: string
  hint?: string
  strong?: boolean
}) {
  return (
    <div className={strong ? 'total strong' : 'total'}>
      <span className="total-label">{label}</span>
      <span className="total-value">{value}</span>
      {hint && <span className="total-hint">{hint}</span>}
    </div>
  )
}
