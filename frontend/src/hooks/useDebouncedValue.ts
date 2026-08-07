import { useEffect, useState } from 'react'

/**
 * A simulação acompanha a digitação, mas não pode disparar uma chamada por tecla. O atraso curto
 * segura a rajada sem que o operador perceba espera.
 */
export function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const timer = window.setTimeout(() => setDebounced(value), delayMs)
    return () => window.clearTimeout(timer)
  }, [value, delayMs])

  return debounced
}
