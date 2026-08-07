/**
 * Os valores monetarios chegam como numero JSON, que e o que o Jackson produz a partir de BigDecimal.
 * O front nao faz conta com eles: toda aritmetica de dinheiro acontece no servidor — e por isso que
 * existe o endpoint de simulacao em vez de replicar a formula no navegador. Aqui esses campos so
 * sao formatados para exibicao.
 */

export type SettlementStatus = 'PENDING' | 'SETTLED' | 'CANCELLED'

export type RateSource = 'MANUAL' | 'PROVIDER'

export interface Currency {
  code: string
  name: string
  minorUnit: number
}

export interface ReceivableType {
  code: string
  name: string
  monthlySpread: number
}

export interface Assignor {
  id: number
  taxId: string
  legalName: string
}

export interface ExchangeRate {
  id: number
  baseCurrency: string
  quoteCurrency: string
  rate: number
  effectiveAt: string
  source: RateSource
}

export interface ReceivableInput {
  documentNumber: string
  receivableTypeCode: string
  faceValue: number
  issueDate: string
  dueDate: string
}

export interface PricingSimulationRequest {
  faceCurrency: string
  paymentCurrency: string
  baseMonthlyRate: number
  items: ReceivableInput[]
}

export interface PricedReceivable {
  documentNumber: string
  receivableTypeCode: string
  faceValue: number
  issueDate: string
  dueDate: string
  termDays: number
  appliedSpread: number
  monthlyDiscountRate: number
  presentValue: number
  netAmount: number
}

export interface PricingSimulation {
  valuationDate: string
  faceCurrency: string
  paymentCurrency: string
  baseMonthlyRate: number
  exchangeRate: number
  crossCurrency: boolean
  totalFaceValue: number
  totalPresentValue: number
  totalNetAmount: number
  items: PricedReceivable[]
}

export interface RegisterSettlementRequest extends PricingSimulationRequest {
  reference: string
  assignorTaxId: string
}

export interface SettlementItem {
  documentNumber: string
  receivableTypeCode: string
  faceValue: number
  issueDate: string
  dueDate: string
  termDays: number
  appliedSpread: number
  presentValue: number
  netAmount: number
}

export interface Settlement {
  reference: string
  assignorTaxId: string
  assignorLegalName: string
  faceCurrency: string
  paymentCurrency: string
  baseMonthlyRate: number
  exchangeRate?: number
  totalFaceValue: number
  totalPresentValue: number
  totalNetAmount: number
  status: SettlementStatus
  requestedAt: string
  settledAt?: string
  items: SettlementItem[]
}

export interface SettlementReportRow {
  reference: string
  assignorTaxId: string
  assignorLegalName: string
  faceCurrency: string
  paymentCurrency: string
  status: SettlementStatus
  baseMonthlyRate: number
  exchangeRate?: number
  totalFaceValue: number
  totalPresentValue: number
  totalNetAmount: number
  itemCount: number
  requestedAt: string
  settledAt?: string
}

export interface SettlementSummaryRow {
  faceCurrency: string
  paymentCurrency: string
  operations: number
  receivables: number
  totalFaceValue: number
  totalPresentValue: number
  totalNetAmount: number
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type ReportSort = 'SETTLED_AT' | 'REQUESTED_AT' | 'NET_AMOUNT' | 'FACE_VALUE' | 'ASSIGNOR'

export interface ReportFilters {
  from?: string
  to?: string
  assignorTaxId?: string
  paymentCurrency?: string
  status?: SettlementStatus
  sort?: ReportSort
  direction?: 'ASC' | 'DESC'
  page?: number
  size?: number
}
