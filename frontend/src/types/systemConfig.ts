import type { PageQuery } from './common'

export type ConfigStatus = 0 | 1
export type ConfigValueType = 'string' | 'number' | 'boolean'

export interface DictItem {
  uuid: string
  dictType: string
  dictName: string
  itemCode: string
  itemName: string
  itemValue?: number
  sortNo: number
  status: ConfigStatus
  builtIn: ConfigStatus
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface DictItemQuery extends PageQuery {
  keyword?: string
  dictType?: string
  status?: ConfigStatus
}

export interface DictItemSaveDTO {
  dictType: string
  dictName: string
  itemCode: string
  itemName: string
  itemValue?: number
  sortNo?: number
  status: ConfigStatus
  remark?: string
}

export interface ConfigItem {
  uuid: string
  configGroup: string
  configKey: string
  configName: string
  configValue: string
  valueType: ConfigValueType
  unit?: string
  sortNo: number
  status: ConfigStatus
  builtIn: ConfigStatus
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface ConfigItemQuery extends PageQuery {
  keyword?: string
  configGroup?: string
  status?: ConfigStatus
}

export interface ConfigItemSaveDTO {
  configGroup: string
  configKey: string
  configName: string
  configValue: string
  valueType: ConfigValueType
  unit?: string
  sortNo?: number
  status: ConfigStatus
  remark?: string
}

export type NoRulePatternType = 1 | 2
export type NoRuleResetCycle = 0 | 1 | 2 | 3

export interface NoRule {
  uuid: string
  bizType: string
  ruleName: string
  prefix: string
  patternType: NoRulePatternType
  datePattern?: string
  serialLength: number
  resetCycle: NoRuleResetCycle
  status: ConfigStatus
  remark?: string
  createTime?: string
  updateTime?: string
}

export interface NoRuleQuery extends PageQuery {
  keyword?: string
  bizType?: string
  status?: ConfigStatus
}

export interface NoRuleSaveDTO {
  bizType: string
  ruleName: string
  prefix: string
  patternType: NoRulePatternType
  datePattern?: string
  serialLength: number
  resetCycle: NoRuleResetCycle
  status: ConfigStatus
  remark?: string
}

export interface NoRulePreview {
  bizType: string
  exampleNo: string
  sequenceKey: string
  currentValue: number
  nextValue: number
}

export interface RuntimeDictOption {
  dictType: string
  itemCode: string
  itemName: string
  itemValue?: number
  sortNo: number
  remark?: string
}

export interface RuntimeConfig {
  configKey: string
  configName: string
  configValue: string
  valueType: ConfigValueType
  unit?: string
}
