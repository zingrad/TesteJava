import { ApiError } from '../api/ApiError'

interface Props {
  error: unknown
}

/**
 * Erro de validacao ja aparece campo a campo no formulario; repetir aqui so faria ruido. O que sobe
 * para o topo e o que o operador nao consegue resolver no input.
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
        {error.incident && <> Codigo do incidente: <code>{error.incident}</code>.</>}
      </div>
    )
  }

  return (
    <div className="notice error" role="alert">
      Falha inesperada na interface. Recarregue a pagina e tente novamente.
    </div>
  )
}
