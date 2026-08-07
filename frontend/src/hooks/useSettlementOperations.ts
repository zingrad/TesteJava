import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { endpoints } from '../api/endpoints'
import type { PricingSimulationRequest, RegisterSettlementRequest } from '../api/types'
import { useDebouncedValue } from './useDebouncedValue'

const SIMULATION_DEBOUNCE_MS = 400

export function usePricingSimulation(request: PricingSimulationRequest | null) {
  const debounced = useDebouncedValue(request, SIMULATION_DEBOUNCE_MS)

  return useQuery({
    queryKey: ['pricing-simulation', debounced],
    queryFn: () => endpoints.simulate(debounced as PricingSimulationRequest),
    enabled: debounced !== null,
    // Manter o resultado anterior enquanto a próxima simulação chega evita a tabela piscar vazia
    // a cada ajuste de valor.
    placeholderData: keepPreviousData,
  })
}

export function useRegisterSettlement() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (request: RegisterSettlementRequest) => endpoints.registerSettlement(request),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['statement'] })
    },
  })
}

export function useSettleSettlement() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (reference: string) => endpoints.settle(reference),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['statement'] })
    },
  })
}
