import { useCallback, useMemo } from 'react'
import { useSearchParams } from 'react-router-dom'
import type { ReportFilters, ReportSort, SettlementStatus } from '../api/types'

const DEFAULT_SORT: ReportSort = 'SETTLED_AT'
const DEFAULT_DIRECTION = 'DESC'
const DEFAULT_SIZE = 10

const SORTS: ReportSort[] = ['SETTLED_AT', 'REQUESTED_AT', 'NET_AMOUNT', 'FACE_VALUE', 'ASSIGNOR']
const STATUSES: SettlementStatus[] = ['PENDING', 'SETTLED', 'CANCELLED']

/** Os mesmos tamanhos oferecidos no seletor. Um valor fora da lista viria da URL e deixaria o
 * controle exibindo uma opção que não corresponde ao que a tabela esta mostrando. */
export const PAGE_SIZES = [10, 25, 50]

function readEnum<T extends string>(value: string | null, allowed: T[]): T | undefined {
  return value !== null && (allowed as string[]).includes(value) ? (value as T) : undefined
}

/**
 * O recorte vive na query string, não em estado local: o operador pode compartilhar o link de um
 * extrato filtrado, o botão voltar desfaz o filtro anterior e um F5 não perde o contexto.
 */
export function useStatementFilters() {
  const [params, setParams] = useSearchParams()

  const filters = useMemo<ReportFilters>(() => {
    const size = Number(params.get('size'))
    const page = Number(params.get('page'))

    return {
      from: params.get('from') ?? undefined,
      to: params.get('to') ?? undefined,
      assignorTaxId: params.get('assignorTaxId') ?? undefined,
      paymentCurrency: params.get('paymentCurrency') ?? undefined,
      status: readEnum(params.get('status'), STATUSES),
      sort: readEnum(params.get('sort'), SORTS) ?? DEFAULT_SORT,
      direction: params.get('direction') === 'ASC' ? 'ASC' : DEFAULT_DIRECTION,
      page: Number.isInteger(page) && page > 0 ? page : 0,
      size: PAGE_SIZES.includes(size) ? size : DEFAULT_SIZE,
    }
  }, [params])

  /**
   * Mudar qualquer filtro volta para a primeira página. Sem isso, restringir o recorte estando na
   * página 4 deixaria o operador olhando uma tabela vazia sem entender por que.
   */
  const update = useCallback(
    (patch: Partial<ReportFilters>) => {
      const next = { ...filters, ...patch }
      if (!('page' in patch)) {
        next.page = 0
      }

      const search = new URLSearchParams()
      for (const [key, value] of Object.entries(next)) {
        if (value !== undefined && value !== '' && value !== null) {
          search.set(key, String(value))
        }
      }
      setParams(search, { replace: true })
    },
    [filters, setParams],
  )

  const toggleSort = useCallback(
    (sort: ReportSort) => {
      const sameColumn = filters.sort === sort
      update({ sort, direction: sameColumn && filters.direction === 'DESC' ? 'ASC' : 'DESC' })
    },
    [filters, update],
  )

  const clear = useCallback(() => setParams(new URLSearchParams(), { replace: true }), [setParams])

  const hasActiveFilters =
    Boolean(filters.from) ||
    Boolean(filters.to) ||
    Boolean(filters.assignorTaxId) ||
    Boolean(filters.paymentCurrency) ||
    Boolean(filters.status)

  return { filters, update, toggleSort, clear, hasActiveFilters }
}
