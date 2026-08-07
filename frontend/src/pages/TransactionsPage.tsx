import { useEffect } from 'react'
import { ErrorNotice } from '../components/ErrorNotice'
import { Pagination } from '../components/transactions/Pagination'
import { StatementFilters } from '../components/transactions/StatementFilters'
import { StatementTable } from '../components/transactions/StatementTable'
import { SummaryStrip } from '../components/transactions/SummaryStrip'
import { useStatement, useStatementSummary } from '../hooks/useStatement'
import { useStatementFilters } from '../hooks/useStatementFilters'
import { useAssignors, useCurrencies } from '../hooks/useReferenceData'

export function TransactionsPage() {
  const { filters, update, toggleSort, clear, hasActiveFilters } = useStatementFilters()
  const statement = useStatement(filters)
  const summary = useStatementSummary(filters)
  const currencies = useCurrencies()
  const assignors = useAssignors()

  const page = statement.data

  /**
   * Um link compartilhado pode apontar para uma pagina que nao existe mais, seja porque o recorte
   * mudou ou porque as linhas foram removidas. Em vez de exibir "nenhuma operacao" sobre um total
   * que claramente nao e zero, a grade volta sozinha para a ultima pagina valida.
   */
  useEffect(() => {
    if (page && page.content.length === 0 && page.totalElements > 0 && page.page > 0) {
      update({ page: page.totalPages - 1 })
    }
  }, [page, update])

  return (
    <>
      <div className="card-title">
        <h1>Extrato de liquidacao</h1>
        <p>Paginacao e filtros resolvidos no servidor</p>
      </div>

      <ErrorNotice error={statement.error ?? summary.error} />

      <StatementFilters
        filters={filters}
        currencies={currencies.data ?? []}
        assignors={assignors.data ?? []}
        hasActiveFilters={hasActiveFilters}
        onChange={update}
        onClear={clear}
      />

      <SummaryStrip summary={summary.data} />

      <div className="card">
        {statement.isPending ? (
          <div className="empty-state">Carregando extrato...</div>
        ) : (
          <>
            <StatementTable
              rows={page?.content ?? []}
              filters={filters}
              isFetching={statement.isFetching}
              onSort={toggleSort}
            />
            <Pagination
              page={page?.page ?? 0}
              size={page?.size ?? 10}
              totalElements={page?.totalElements ?? 0}
              totalPages={page?.totalPages ?? 0}
              onPageChange={(next) => update({ page: next })}
              onSizeChange={(size) => update({ size })}
            />
          </>
        )}
      </div>
    </>
  )
}
