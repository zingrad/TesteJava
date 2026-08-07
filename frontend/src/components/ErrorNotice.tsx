import { ApiError } from '../api/ApiError'

interface Props {
  error: unknown
}

/**
 * Erro de validação já aparece campo a campo no formulário; repetir aqui só faria ruido. O que sobe
 * para o topo é o que o operador não consegue resolver no input.
 */
export function ErrorNotice({ error }: Props) {
  if (!error) {
    return null
  }

  if (error instanceof ApiError) {
    if (error.isValidation && error.violations.length > 0) {
      return null
    }
    return (
      <div className="notice error" role="alert">
        {error.detail}
        {error.incident && <> Código do incidente: <code>{error.incident}</code>.</>}
      </div>
    )
  }

  return (
    <div className="notice error" role="alert">
      Falha inesperada na interface. Recarregue a página e tente novamente.
    </div>
  )
}
