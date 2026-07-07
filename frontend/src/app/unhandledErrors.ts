import { BizError, notifyErrorOnce } from '../api/request'

let installed = false

export function installUnhandledErrorHandlers(): void {
  if (installed) return
  window.addEventListener('unhandledrejection', handleUnhandledRejection)
  installed = true
}

function handleUnhandledRejection(event: PromiseRejectionEvent): void {
  if (!(event.reason instanceof BizError)) return
  notifyErrorOnce(event.reason)
  event.preventDefault()
}
