const currencyFormatters = new Map<string, Intl.NumberFormat>()

function currencyFormatter(currency: string): Intl.NumberFormat {
  let formatter = currencyFormatters.get(currency)
  if (!formatter) {
    formatter = new Intl.NumberFormat('pt-BR', { style: 'currency', currency })
    currencyFormatters.set(currency, formatter)
  }
  return formatter
}

export function money(value: number, currency: string): string {
  return currencyFormatter(currency).format(value)
}

/**
 * Taxas trafegam como fração decimal (0,015) e são lidas pelo operador em percentual (1,5% a.m.).
 */
export function monthlyRate(value: number): string {
  return `${new Intl.NumberFormat('pt-BR', {
    minimumFractionDigits: 2,
    maximumFractionDigits: 4,
  }).format(value * 100)}% a.m.`
}

export function date(iso: string | undefined): string {
  if (!iso) {
    return '—'
  }
  const [year, month, day] = iso.slice(0, 10).split('-')
  return `${day}/${month}/${year}`
}

export function dateTime(iso: string | undefined): string {
  if (!iso) {
    return '—'
  }
  return new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(
    new Date(iso),
  )
}

export function taxId(digits: string): string {
  if (digits.length !== 14) {
    return digits
  }
  return `${digits.slice(0, 2)}.${digits.slice(2, 5)}.${digits.slice(5, 8)}/${digits.slice(8, 12)}-${digits.slice(12)}`
}

export function today(): string {
  return new Date().toISOString().slice(0, 10)
}

export function addDays(isoDate: string, days: number): string {
  const value = new Date(`${isoDate}T00:00:00Z`)
  value.setUTCDate(value.getUTCDate() + days)
  return value.toISOString().slice(0, 10)
}
