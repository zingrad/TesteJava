import type { ReceivableType } from '../../api/types'
import type { ReceivableDraft } from '../../hooks/useSettlementDraft'

interface Props {
  items: ReceivableDraft[]
  receivableTypes: ReceivableType[]
  violationFor: (field: string) => string | undefined
  onChange: <K extends keyof ReceivableDraft>(
    key: string,
    field: K,
    value: ReceivableDraft[K],
  ) => void
  onAdd: () => void
  onRemove: (key: string) => void
}

export function ReceivableRows({
  items,
  receivableTypes,
  violationFor,
  onChange,
  onAdd,
  onRemove,
}: Props) {
  return (
    <div className="card">
      <div className="card-title">
        <h2>Titulos do lote</h2>
        <p>
          {items.length} {items.length === 1 ? 'titulo' : 'titulos'}
        </p>
      </div>

      <div className="receivable-rows">
        {items.map((item, index) => {
          const faceValueError = violationFor(`items[${index}].faceValue`)
          const documentError = violationFor(`items[${index}].documentNumber`)
          const datesInverted = item.dueDate !== '' && item.dueDate <= item.issueDate

          return (
            <div className="receivable-row" key={item.key}>
              <div className="field">
                <label htmlFor={`doc-${item.key}`}>Documento</label>
                <input
                  id={`doc-${item.key}`}
                  value={item.documentNumber}
                  placeholder="DUP-2026-0001"
                  aria-invalid={documentError ? 'true' : undefined}
                  onChange={(event) => onChange(item.key, 'documentNumber', event.target.value)}
                />
                {documentError && <span className="error">{documentError}</span>}
              </div>

              <div className="field">
                <label htmlFor={`type-${item.key}`}>Tipo</label>
                <select
                  id={`type-${item.key}`}
                  value={item.receivableTypeCode}
                  onChange={(event) => onChange(item.key, 'receivableTypeCode', event.target.value)}
                >
                  {receivableTypes.map((type) => (
                    <option key={type.code} value={type.code}>
                      {type.name}
                    </option>
                  ))}
                </select>
              </div>

              <div className="field">
                <label htmlFor={`value-${item.key}`}>Valor de face</label>
                <input
                  id={`value-${item.key}`}
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={item.faceValue}
                  placeholder="10000.00"
                  aria-invalid={faceValueError ? 'true' : undefined}
                  onChange={(event) => onChange(item.key, 'faceValue', event.target.value)}
                />
                {faceValueError && <span className="error">{faceValueError}</span>}
              </div>

              <div className="field">
                <label htmlFor={`issue-${item.key}`}>Emissao</label>
                <input
                  id={`issue-${item.key}`}
                  type="date"
                  value={item.issueDate}
                  onChange={(event) => onChange(item.key, 'issueDate', event.target.value)}
                />
              </div>

              <div className="field">
                <label htmlFor={`due-${item.key}`}>Vencimento</label>
                <input
                  id={`due-${item.key}`}
                  type="date"
                  value={item.dueDate}
                  aria-invalid={datesInverted ? 'true' : undefined}
                  onChange={(event) => onChange(item.key, 'dueDate', event.target.value)}
                />
                {datesInverted && <span className="error">deve ser posterior a emissao</span>}
              </div>

              <button
                type="button"
                className="row-remove"
                onClick={() => onRemove(item.key)}
                disabled={items.length === 1}
                aria-label={`Remover titulo ${index + 1}`}
              >
                Remover
              </button>
            </div>
          )
        })}
      </div>

      <button type="button" onClick={onAdd} style={{ marginTop: 12 }}>
        Adicionar titulo
      </button>
    </div>
  )
}
