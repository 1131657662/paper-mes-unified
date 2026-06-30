import request from './request'
import type { PageResult } from '../types/common'
import type {
  ConfigItem,
  ConfigItemQuery,
  ConfigItemSaveDTO,
  DictItem,
  DictItemQuery,
  DictItemSaveDTO,
  NoRule,
  NoRulePreview,
  NoRuleQuery,
  NoRuleSaveDTO,
  RuntimeConfig,
  RuntimeDictOption,
} from '../types/systemConfig'

export function pageDictItems(query: DictItemQuery) {
  return request<PageResult<DictItem>>({
    url: '/api/system/dict-items',
    method: 'get',
    params: query,
  })
}

export function createDictItem(data: DictItemSaveDTO) {
  return request<string>({ url: '/api/system/dict-items', method: 'post', data })
}

export function updateDictItem(uuid: string, data: DictItemSaveDTO) {
  return request<void>({ url: `/api/system/dict-items/${uuid}`, method: 'put', data })
}

export function deleteDictItem(uuid: string) {
  return request<void>({ url: `/api/system/dict-items/${uuid}`, method: 'delete' })
}

export function pageConfigItems(query: ConfigItemQuery) {
  return request<PageResult<ConfigItem>>({
    url: '/api/system/config-items',
    method: 'get',
    params: query,
  })
}

export function createConfigItem(data: ConfigItemSaveDTO) {
  return request<string>({ url: '/api/system/config-items', method: 'post', data })
}

export function updateConfigItem(uuid: string, data: ConfigItemSaveDTO) {
  return request<void>({ url: `/api/system/config-items/${uuid}`, method: 'put', data })
}

export function deleteConfigItem(uuid: string) {
  return request<void>({ url: `/api/system/config-items/${uuid}`, method: 'delete' })
}

export function pageNoRules(query: NoRuleQuery) {
  return request<PageResult<NoRule>>({
    url: '/api/system/no-rules',
    method: 'get',
    params: query,
  })
}

export function updateNoRule(uuid: string, data: NoRuleSaveDTO) {
  return request<void>({ url: `/api/system/no-rules/${uuid}`, method: 'put', data })
}

export function previewNoRule(bizType: string, bizDate?: string) {
  return request<NoRulePreview>({
    url: `/api/system/no-rules/${bizType}/preview`,
    method: 'get',
    params: bizDate ? { bizDate } : undefined,
  })
}

export function listRuntimeDictOptions(types: string[]) {
  return request<RuntimeDictOption[]>({
    url: '/api/system/runtime/dict-options',
    method: 'get',
    params: { types: types.join(',') },
  })
}

export function listRuntimeConfigs(keys: string[]) {
  return request<RuntimeConfig[]>({
    url: '/api/system/runtime/configs',
    method: 'get',
    params: { keys: keys.join(',') },
  })
}
