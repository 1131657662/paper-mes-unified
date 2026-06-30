import { useEffect, useRef, useState } from 'react'
import { CONFIG_KEYS } from '../configFallbacks'
import { useNumberConfigValue } from './useSystemConfigValue'

export function useConfiguredPageSize(fallback = 20) {
  const { value: configuredPageSize } = useNumberConfigValue(CONFIG_KEYS.defaultPageSize, fallback)
  const [pageSize, setPageSizeState] = useState(fallback)
  const touchedRef = useRef(false)

  useEffect(() => {
    if (touchedRef.current || configuredPageSize <= 0) return
    setPageSizeState(configuredPageSize)
  }, [configuredPageSize])

  const setPageSize = (nextPageSize: number) => {
    touchedRef.current = true
    setPageSizeState(nextPageSize)
  }

  return [pageSize, setPageSize] as const
}
