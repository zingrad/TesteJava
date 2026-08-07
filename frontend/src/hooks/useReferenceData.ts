import { useQuery } from '@tanstack/react-query'
import { endpoints } from '../api/endpoints'

/**
 * Moedas, tipos de recebível e cedentes mudam em ritmo de cadastro, não de operação. Ficam em cache
 * por bastante tempo para o painel não rebuscar a cada tecla digitada na simulação.
 */
const REFERENCE_DATA_STALE_TIME = 15 * 60 * 1000

export function useCurrencies() {
  return useQuery({
    queryKey: ['currencies'],
    queryFn: endpoints.currencies,
    staleTime: REFERENCE_DATA_STALE_TIME,
  })
}

export function useReceivableTypes() {
  return useQuery({
    queryKey: ['receivable-types'],
    queryFn: endpoints.receivableTypes,
    staleTime: REFERENCE_DATA_STALE_TIME,
  })
}

export function useAssignors() {
  return useQuery({
    queryKey: ['assignors'],
    queryFn: endpoints.assignors,
    staleTime: REFERENCE_DATA_STALE_TIME,
  })
}
