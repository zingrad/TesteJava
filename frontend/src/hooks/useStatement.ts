import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { endpoints } from '../api/endpoints'
import type { ReportFilters } from '../api/types'

export function useStatement(filters: ReportFilters) {
  return useQuery({
    queryKey: ['statement', filters],
    queryFn: () => endpoints.statement(filters),
    // Trocar de pagina mantendo as linhas anteriores na tela evita o salto de altura da tabela.
    placeholderData: keepPreviousData,
  })
}

export function useStatementSummary(filters: ReportFilters) {
  // Os totais dependem do recorte, mas nao da pagina nem da ordenacao: incluir esses campos na
  // chave faria a mesma consulta rodar de novo a cada clique de paginacao.
  const scope = {
    from: filters.from,
    to: filters.to,
    assignorTaxId: filters.assignorTaxId,
    paymentCurrency: filters.paymentCurrency,
    status: filters.status,
  }

  return useQuery({
    queryKey: ['statement-summary', scope],
    queryFn: () => endpoints.statementSummary(scope),
    placeholderData: keepPreviousData,
  })
}
