import { PAGE_SIZES } from '../../hooks/useStatementFilters'

interface Props {
  page: number
  size: number
  totalElements: number
  totalPages: number
  onPageChange: (page: number) => void
  onSizeChange: (size: number) => void
}

export function Pagination({
  page,
  size,
  totalElements,
  totalPages,
  onPageChange,
  onSizeChange,
}: Props) {
  const first = Math.min(page * size + 1, totalElements)
  const last = Math.min((page + 1) * size, totalElements)

  return (
    <div className="pagination">
      <span className="muted">
        {totalElements === 0
          ? 'Nenhum resultado'
          : `${first}–${last} de ${totalElements} ${totalElements === 1 ? 'operacao' : 'operacoes'}`}
      </span>

      <div className="pagination-controls">
        <label className="muted tiny" htmlFor="page-size">
          Por pagina
        </label>
        <select
          id="page-size"
          value={size}
          onChange={(event) => onSizeChange(Number(event.target.value))}
        >
          {PAGE_SIZES.map((option) => (
            <option key={option} value={option}>
              {option}
            </option>
          ))}
        </select>

        <button type="button" onClick={() => onPageChange(page - 1)} disabled={page === 0}>
          Anterior
        </button>
        <span className="muted tiny">
          {totalPages === 0 ? 0 : page + 1} / {totalPages}
        </span>
        <button
          type="button"
          onClick={() => onPageChange(page + 1)}
          disabled={page + 1 >= totalPages}
        >
          Proxima
        </button>
      </div>
    </div>
  )
}
