export interface RequestErrorDetails {
  code: number
  httpStatus?: number
  requestId?: string
}

export function formatRequestErrorText(text: string, error: RequestErrorDetails): string {
  if (!shouldShowRequestId(error) || !error.requestId) return text
  return `${text}（请求编号：${error.requestId}）`
}

export function shouldShowRequestId(error: RequestErrorDetails): boolean {
  return (error.httpStatus ?? error.code) >= 500
}
