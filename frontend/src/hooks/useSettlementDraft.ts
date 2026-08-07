import { useCallback, useMemo, useState } from 'react'
import type { PricingSimulationRequest, RegisterSettlementRequest } from '../api/types'
import { addDays, today } from '../lib/format'

export interface ReceivableDraft {
  key: string
  documentNumber: string
  receivableTypeCode: string
  faceValue: string
  issueDate: string
  dueDate: string
}

export interface DraftHeader {
  reference: string
  assignorTaxId: string
  faceCurrency: string
  paymentCurrency: string
  baseMonthlyRatePercent: string
}

interface Defaults {
  receivableTypeCode: string
  faceCurrency: string
  paymentCurrency: string
  assignorTaxId: string
}

const DEFAULT_TERM_DAYS = 60

function newReceivable(receivableTypeCode: string): ReceivableDraft {
  const issueDate = today()
  return {
    key: crypto.randomUUID(),
    documentNumber: '',
    receivableTypeCode,
    faceValue: '',
    issueDate,
    dueDate: addDays(issueDate, DEFAULT_TERM_DAYS),
  }
}

function isPositiveNumber(value: string): boolean {
  const parsed = Number(value)
  return value.trim() !== '' && Number.isFinite(parsed) && parsed > 0
}

/**
 * Toda a logica de estado e de montagem do payload vive aqui; os componentes so renderizam o que
 * este hook devolve. A simulacao so recebe um pedido quando o rascunho esta completo — mandar lote
 * pela metade so renderia 400 a cada tecla digitada.
 */
export function useSettlementDraft(defaults: Defaults) {
  const [header, setHeader] = useState<DraftHeader>({
    reference: '',
    assignorTaxId: defaults.assignorTaxId,
    faceCurrency: defaults.faceCurrency,
    paymentCurrency: defaults.paymentCurrency,
    baseMonthlyRatePercent: '1',
  })
  const [items, setItems] = useState<ReceivableDraft[]>([
    newReceivable(defaults.receivableTypeCode),
  ])

  const updateHeader = useCallback(<K extends keyof DraftHeader>(field: K, value: DraftHeader[K]) => {
    setHeader((current) => ({ ...current, [field]: value }))
  }, [])

  const updateItem = useCallback(
    <K extends keyof ReceivableDraft>(key: string, field: K, value: ReceivableDraft[K]) => {
      setItems((current) =>
        current.map((item) => (item.key === key ? { ...item, [field]: value } : item)),
      )
    },
    [],
  )

  const addItem = useCallback(() => {
    setItems((current) => [...current, newReceivable(defaults.receivableTypeCode)])
  }, [defaults.receivableTypeCode])

  const removeItem = useCallback((key: string) => {
    setItems((current) => (current.length === 1 ? current : current.filter((i) => i.key !== key)))
  }, [])

  const reset = useCallback(() => {
    setHeader((current) => ({ ...current, reference: '' }))
    setItems([newReceivable(defaults.receivableTypeCode)])
  }, [defaults.receivableTypeCode])

  const pricingRequest = useMemo<PricingSimulationRequest | null>(() => {
    const complete = items.every(
      (item) =>
        item.receivableTypeCode !== '' &&
        isPositiveNumber(item.faceValue) &&
        item.issueDate !== '' &&
        item.dueDate !== '' &&
        item.dueDate > item.issueDate,
    )
    if (!complete || items.length === 0 || header.baseMonthlyRatePercent.trim() === '') {
      return null
    }

    return {
      faceCurrency: header.faceCurrency,
      paymentCurrency: header.paymentCurrency,
      // A mesa pensa em percentual ao mes; a API trabalha em fracao decimal.
      baseMonthlyRate: Number(header.baseMonthlyRatePercent) / 100,
      items: items.map((item) => ({
        documentNumber: item.documentNumber.trim() || 'SEM-NUMERO',
        receivableTypeCode: item.receivableTypeCode,
        faceValue: Number(item.faceValue),
        issueDate: item.issueDate,
        dueDate: item.dueDate,
      })),
    }
  }, [header, items])

  const settlementRequest = useMemo<RegisterSettlementRequest | null>(() => {
    const documentsFilled = items.every((item) => item.documentNumber.trim() !== '')
    if (!pricingRequest || !documentsFilled || header.reference.trim() === '') {
      return null
    }
    return {
      ...pricingRequest,
      reference: header.reference.trim(),
      assignorTaxId: header.assignorTaxId,
      items: items.map((item) => ({
        documentNumber: item.documentNumber.trim(),
        receivableTypeCode: item.receivableTypeCode,
        faceValue: Number(item.faceValue),
        issueDate: item.issueDate,
        dueDate: item.dueDate,
      })),
    }
  }, [header, items, pricingRequest])

  return {
    header,
    items,
    updateHeader,
    updateItem,
    addItem,
    removeItem,
    reset,
    pricingRequest,
    settlementRequest,
  }
}
