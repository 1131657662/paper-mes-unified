import {
  createConfigItem,
  createDictItem,
  deleteConfigItem,
  deleteDictItem,
  listRuntimeConfigs,
  listRuntimeDictOptions,
  pageNoRules,
  pageConfigItems,
  pageDictItems,
  previewNoRule,
  updateConfigItem,
  updateDictItem,
  updateNoRule,
} from '../../../api/systemConfig'
import type {
  ConfigItemQuery,
  ConfigItemSaveDTO,
  DictItemQuery,
  DictItemSaveDTO,
  NoRuleQuery,
  NoRuleSaveDTO,
} from '../../../types/systemConfig'

export const systemConfigService = {
  configItems: (query: ConfigItemQuery) => pageConfigItems(query),
  createConfigItem: (data: ConfigItemSaveDTO) => createConfigItem(data),
  createDictItem: (data: DictItemSaveDTO) => createDictItem(data),
  deleteConfigItem: (uuid: string) => deleteConfigItem(uuid),
  deleteDictItem: (uuid: string) => deleteDictItem(uuid),
  dictItems: (query: DictItemQuery) => pageDictItems(query),
  noRules: (query: NoRuleQuery) => pageNoRules(query),
  previewNoRule: (bizType: string) => previewNoRule(bizType),
  runtimeConfigs: (keys: string[]) => listRuntimeConfigs(keys),
  runtimeDictOptions: (types: string[]) => listRuntimeDictOptions(types),
  updateConfigItem: (params: { uuid: string; data: ConfigItemSaveDTO }) => updateConfigItem(params.uuid, params.data),
  updateDictItem: (params: { uuid: string; data: DictItemSaveDTO }) => updateDictItem(params.uuid, params.data),
  updateNoRule: (params: { uuid: string; data: NoRuleSaveDTO }) => updateNoRule(params.uuid, params.data),
}
