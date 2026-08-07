import { api, queryString } from './client'
import type {
  Assignor,
  Currency,
  ExchangeRate,
  Page,
  PricingSimulation,
  PricingSimulationRequest,
  ReceivableType,
  RegisterSettlementRequest,
  ReportFilters,
  Settlement,
  SettlementReportRow,
  SettlementSummaryRow,
} from './types'

export const endpoints = {
  currencies: () => api.get<Currency[]>('/api/currencies'),

  receivableTypes: () => api.get<ReceivableType[]>('/api/receivable-types'),

  assignors: () => api.get<Assignor[]>('/api/assignors'),

  currentRate: (base: string, quote: string) =>
    api.get<ExchangeRate>(`/api/exchange-rates/current${queryString({ base, quote })}`),

  syncRates: () => api.post<ExchangeRate[]>('/api/exchange-rates/sync'),

  simulate: (request: PricingSimulationRequest) =>
    api.post<PricingSimulation>('/api/pricing/simulate', request),

  registerSettlement: (request: RegisterSettlementRequest) =>
    api.post<Settlement>('/api/settlements', request),

  settlement: (reference: string) =>
    api.get<Settlement>(`/api/settlements/${encodeURIComponent(reference)}`),

  settle: (reference: string) =>
    api.post<Settlement>(`/api/settlements/${encodeURIComponent(reference)}/settle`),

  cancelSettlement: (reference: string) =>
    api.post<Settlement>(`/api/settlements/${encodeURIComponent(reference)}/cancel`),

  statement: (filters: ReportFilters) =>
    api.get<Page<SettlementReportRow>>(`/api/reports/settlements${queryString({ ...filters })}`),

  statementSummary: (filters: ReportFilters) =>
    api.get<SettlementSummaryRow[]>(
      `/api/reports/settlements/summary${queryString({
        from: filters.from,
        to: filters.to,
        assignorTaxId: filters.assignorTaxId,
        paymentCurrency: filters.paymentCurrency,
        status: filters.status,
      })}`,
    ),
}
