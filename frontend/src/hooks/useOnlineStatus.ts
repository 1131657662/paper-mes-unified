import { useSyncExternalStore } from 'react'

export function useOnlineStatus() {
  return useSyncExternalStore(subscribeOnlineStatus, getOnlineStatus, getServerOnlineStatus)
}

function subscribeOnlineStatus(onStoreChange: () => void) {
  window.addEventListener('online', onStoreChange)
  window.addEventListener('offline', onStoreChange)

  return () => {
    window.removeEventListener('online', onStoreChange)
    window.removeEventListener('offline', onStoreChange)
  }
}

function getOnlineStatus() {
  return navigator.onLine
}

function getServerOnlineStatus() {
  return true
}
