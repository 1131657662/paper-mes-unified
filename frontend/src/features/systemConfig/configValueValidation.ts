import type { ConfigItemSaveDTO } from '../../types/systemConfig'
import { CONFIG_KEYS } from './configFallbacks'

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
  if (options.valueType !== 'number') return undefined
  const number = Number(text)
  return validateKnownNumber(options.configKey, text, number)
}

function validateKnownNumber(configKey: string | undefined, text: string, number: number) {
  if (configKey === CONFIG_KEYS.spareRollNoCount && !isIntegerInRange(number, 0, 100)) {
    return '备用卷号数量必须是 0 到 100 的整数'
  }
  if (configKey === CONFIG_KEYS.backupRetentionDays && !isIntegerInRange(number, 7, 3650)) {
    return '备份保留天数必须是 7 到 3650 天的整数'
  }
  if (configKey === CONFIG_KEYS.defaultPageSize && !isIntegerInRange(number, 10, 100)) {
    return '默认每页条数必须是 10 到 100 的整数'
  }
  if (configKey === CONFIG_KEYS.cashSettleBlockMode && !isIntegerInRange(number, 0, 2)) {
    return '现结出库拦截模式只能是 0、1 或 2'
  }
  if (PERCENT_KEYS.has(configKey ?? '') && !isInRange(number, 0, 100)) {
    return '百分比参数必须在 0 到 100 之间'
  }
  if (MONEY_KEYS.has(configKey ?? '') && !isValidMoney(text, number)) {
    return '金额参数必须在 0 到 999999999.99 之间，且最多保留两位小数'
  }
  return undefined
}

function isIntegerInRange(value: number, min: number, max: number): boolean {
  return Number.isInteger(value) && isInRange(value, min, max)
}

function isInRange(value: number, min: number, max: number): boolean {
  return Number.isFinite(value) && value >= min && value <= max
}

function isValidMoney(text: string, value: number): boolean {
  return MONEY_PATTERN.test(text) && isInRange(value, 0, 999_999_999.99)
}

const NUMBER_PATTERN = /^[+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?$/
const MONEY_PATTERN = /^\+?(?:\d+(?:\.\d{0,2})?|\.\d{1,2})$/
const BOOLEAN_VALUES = new Set(['true', 'false'])
const PERCENT_KEYS: ReadonlySet<string> = new Set([
  CONFIG_KEYS.discountMaxPercent,
  CONFIG_KEYS.weightBlockTolerancePercent,
  CONFIG_KEYS.weightTolerancePercent,
])
const MONEY_KEYS: ReadonlySet<string> = new Set([
  CONFIG_KEYS.discountAutoApproveLimit,
  CONFIG_KEYS.discountMaxAmount,
  CONFIG_KEYS.pricingAutoApproveLimit,
])
