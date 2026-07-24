import { CONFIG_KEYS } from './configFallbacks'
import type { ConfigItemSaveDTO } from '../../types/systemConfig'

interface ConfigValueValidationOptions {
  configKey?: string
  value: unknown
  valueType?: ConfigItemSaveDTO['valueType']
}

export function getConfigValueError(options: ConfigValueValidationOptions): string | undefined {
  const text = typeof options.value === 'string' ? options.value.trim() : ''
  if (!text) return undefined
  if (options.valueType === 'number' && !NUMBER_PATTERN.test(text)) return '请输入有效数字'
  if (options.valueType === 'boolean' && !BOOLEAN_VALUES.has(text.toLowerCase())) {
    return '布尔参数值只能填写 true 或 false'
  }
  if (options.configKey === CONFIG_KEYS.spareRollNoCount && !isValidSpareCount(text)) {
    return '备用卷号数量必须是 0 到 100 的整数'
  }
  return undefined
}

function isValidSpareCount(value: string): boolean {
  const parsed = Number(value)
  return Number.isInteger(parsed) && parsed >= 0 && parsed <= 100
}

const NUMBER_PATTERN = /^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?$/
const BOOLEAN_VALUES = new Set(['true', 'false'])
