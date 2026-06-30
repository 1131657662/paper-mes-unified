import { createQueryKeys } from '@lukemorales/query-key-factory'
import type { ConfigItemQuery, DictItemQuery } from '../../../types/systemConfig'
import { systemConfigService } from '../services/systemConfigService'

export const systemConfigKeys = createQueryKeys('systemConfig', {
  configItems: (query: ConfigItemQuery) => ({
    queryKey: [query],
    queryFn: () => systemConfigService.configItems(query),
  }),
  dictItems: (query: DictItemQuery) => ({
    queryKey: [query],
    queryFn: () => systemConfigService.dictItems(query),
  }),
  runtimeConfigs: (keys: string[]) => ({
    queryKey: [keys],
    queryFn: () => systemConfigService.runtimeConfigs(keys),
  }),
  runtimeDictOptions: (types: string[]) => ({
    queryKey: [types],
    queryFn: () => systemConfigService.runtimeDictOptions(types),
  }),
})
