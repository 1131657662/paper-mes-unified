import { CONFIG_KEYS } from '../../systemConfig/configFallbacks'
import { useRuntimeConfigs } from '../../systemConfig/hooks/useRuntimeConfigs'

const SETTING_KEYS = [CONFIG_KEYS.autoFinishConfig, CONFIG_KEYS.spareRollNoCount]

export function useProcessOrderCreateSettings() {
  const query = useRuntimeConfigs(SETTING_KEYS)
  const configMap = new Map(query.data?.map((item) => [item.configKey, item.configValue]))
  const spareCount = Number(configMap.get(CONFIG_KEYS.spareRollNoCount) ?? 0)
  return {
    ...query,
    autoFinishConfigEnabled: configMap.get(CONFIG_KEYS.autoFinishConfig)?.toLowerCase() === 'true',
    defaultSpareCount: Number.isFinite(spareCount) ? spareCount : 0,
  }
}
