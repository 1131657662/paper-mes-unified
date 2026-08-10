const CHUNK_RELOAD_KEY = 'paper-mes:chunk-reload'

let installed = false
let reloadAttempted = false

/** Reload once when Vite cannot resolve a stale preload after a deployment. */
export function installChunkRecovery(): void {
  if (installed) return
  installed = true
  window.addEventListener('vite:preloadError', handlePreloadError)
  window.addEventListener('load', clearReloadMarker, { once: true })
  window.setTimeout(clearReloadMarker, 10_000)
}

function handlePreloadError(event: Event): void {
  event.preventDefault()
  if (reloadAttempted || !shouldReloadAfterPreloadError(readReloadMarker())) return

  reloadAttempted = true
  markReloadAttempt()
  window.location.reload()
}

export function shouldReloadAfterPreloadError(marker: string | null): boolean {
  return marker !== '1'
}

function readReloadMarker(): string | null {
  try {
    return window.sessionStorage.getItem(CHUNK_RELOAD_KEY) === '1'
      ? '1'
      : null
  } catch {
    return null
  }
}

function markReloadAttempt(): void {
  try {
    window.sessionStorage.setItem(CHUNK_RELOAD_KEY, '1')
  } catch {
    // Private browsing or a blocked storage policy should not break recovery.
  }
}

function clearReloadMarker(): void {
  try {
    window.sessionStorage.removeItem(CHUNK_RELOAD_KEY)
  } catch {
    // Ignore storage failures; the listener still provides the best effort path.
  }
}
