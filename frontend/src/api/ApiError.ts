export interface FieldViolation {
  field: string
  message: string
}

interface ProblemDetail {
  title?: string
  status?: number
  detail?: string
  code?: string
  incident?: string
  violations?: FieldViolation[]
}

/**
 * Traduz o corpo RFC 7807 devolvido pela API. O `code` e o contrato estavel entre back e front —
 * a mensagem pode mudar de redacao, o codigo nao — entao e nele que a UI decide comportamento.
 */
export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly detail: string
  readonly violations: FieldViolation[]
  readonly incident?: string

  constructor(status: number, problem: ProblemDetail) {
    const detail = problem.detail ?? problem.title ?? 'Falha ao comunicar com o servidor.'
    super(detail)
    this.name = 'ApiError'
    this.status = status
    this.code = problem.code ?? 'UNKNOWN'
    this.detail = detail
    this.violations = problem.violations ?? []
    this.incident = problem.incident
  }

  static async fromResponse(response: Response): Promise<ApiError> {
    try {
      const problem = (await response.json()) as ProblemDetail
      return new ApiError(response.status, problem)
    } catch {
      return new ApiError(response.status, { detail: `Erro ${response.status} ao chamar a API.` })
    }
  }

  static network(): ApiError {
    return new ApiError(0, {
      code: 'NETWORK_UNAVAILABLE',
      detail: 'Nao foi possivel alcancar a API. Verifique se o backend esta no ar.',
    })
  }

  violationFor(field: string): string | undefined {
    return this.violations.find((violation) => violation.field === field)?.message
  }

  get isValidation(): boolean {
    return this.status === 400
  }

  get isConflict(): boolean {
    return this.status === 409
  }
}
