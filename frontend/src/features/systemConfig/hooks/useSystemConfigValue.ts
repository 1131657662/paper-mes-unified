import { useRuntimeConfigs } from './useRuntimeConfigs'

export function useSystemConfigValue(configKey: string, fallback = '') {
  const query = useRuntimeConfigs([configKey])
  const value = query.data?.find((item) => item.configKey === configKey)?.configValue ?? fallback
  return { ...query, value }
}

export function useNumberConfigValue(configKey: string, fallback: number) {
  const query = useSystemConfigValue(configKey, String(fallback))
  const numericValue = Number(query.value)
  return { ...query, value: Number.isFinite(numericValue) ? numericValue : fallback }
}
