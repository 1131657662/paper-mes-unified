import { STANDARD_WEIGHT_FORMULA } from './customerSpecDraftModel'

export const CUSTOMER_WEIGHT_FORMULA_VARIABLES = [
  { key: 'physicalWeight', label: '实物重量', description: '现场实物重量，单位 kg' },
  { key: 'physicalGsm', label: '实物克重', description: '母卷实际克重，单位 g/m²' },
  { key: 'physicalWidth', label: '实物门幅', description: '现场实物门幅，单位 mm' },
  { key: 'customerGsm', label: '客户克重', description: '客户要求的克重，单位 g/m²' },
  { key: 'customerWidth', label: '客户门幅', description: '客户要求的门幅，单位 mm' },
] as const

const FORMULA_DISPLAY_VARIABLES = [
  ...CUSTOMER_WEIGHT_FORMULA_VARIABLES,
  { key: 'sourceWeight', label: '来源重量' },
  { key: 'sourceGsm', label: '来源克重' },
  { key: 'sourceWidth', label: '来源门幅' },
  { key: 'finishWeight', label: '成品重量' },
  { key: 'finishWidth', label: '成品门幅' },
  { key: 'lossWeight', label: '损耗重量' },
  { key: 'adjustment', label: '调整值' },
] as const

export const CUSTOMER_WEIGHT_FORMULA_PRESETS = [
  {
    value: 'STANDARD',
    label: '克重 + 门幅换算',
    formula: STANDARD_WEIGHT_FORMULA,
    summary: '按客户克重比例换算，再按客户门幅比例换算。',
  },
  {
    value: 'GSM_ONLY',
    label: '仅按克重换算',
    formula: 'physicalWeight * (customerGsm / physicalGsm)',
    summary: '只按客户克重变化换算，门幅不参与计算。',
  },
  {
    value: 'WIDTH_ONLY',
    label: '仅按门幅换算',
    formula: 'physicalWeight * (customerWidth / physicalWidth)',
    summary: '只按客户门幅变化换算，克重不参与计算。',
  },
] as const

export type CustomerWeightFormulaPreset = (typeof CUSTOMER_WEIGHT_FORMULA_PRESETS)[number]['value'] | 'CUSTOM'

export function formulaPresetValue(formula?: string): CustomerWeightFormulaPreset {
  return CUSTOMER_WEIGHT_FORMULA_PRESETS.find((preset) => preset.formula === formula)?.value ?? 'CUSTOM'
}

export function formulaPresetSummary(formula?: string) {
  return CUSTOMER_WEIGHT_FORMULA_PRESETS.find((preset) => preset.formula === formula)?.summary
    ?? '自定义公式按输入的运算顺序计算，结果作为每件客户重量。'
}

export function toDisplayFormula(formula?: string) {
  if (!formula) return ''
  let display = formula
  for (const variable of FORMULA_DISPLAY_VARIABLES) {
    display = display.replace(new RegExp(`\\b${variable.key}\\b`, 'g'), variable.label)
  }
  return display
    .replace(/\s*\*\s*/g, ' × ')
    .replace(/\s*\/\s*/g, ' ÷ ')
    .replace(/\s+/g, ' ')
    .trim()
}

export function toEngineFormula(displayFormula: string) {
  let formula = displayFormula.trim()
  for (const variable of FORMULA_DISPLAY_VARIABLES) {
    formula = formula.split(variable.label).join(variable.key)
  }
  return formula
    .replaceAll('×', '*')
    .replaceAll('÷', '/')
    .replaceAll('（', '(')
    .replaceAll('）', ')')
    .replace(/\s*([+*/-])\s*/g, ' $1 ')
    .replace(/\(\s+/g, '(')
    .replace(/\s+\)/g, ')')
    .replace(/\s+/g, ' ')
    .trim()
}

export function appendFormulaToken(formula: string | undefined, token: string) {
  const display = toDisplayFormula(formula)
  return toEngineFormula(`${display}${display ? ' ' : ''}${token}`)
}
