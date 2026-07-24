import { CalculatorOutlined } from '@ant-design/icons'
import { Button, Input, Segmented, Select, Space, Switch, Tag, Tooltip, Typography } from 'antd'
import { STANDARD_WEIGHT_FORMULA, isWeightRuleReady, type WeightRule } from './customerSpecDraftModel'
import type { CustomerWeightMode } from './customerSpecTypes'
import {
  appendFormulaToken,
  CUSTOMER_WEIGHT_FORMULA_PRESETS,
  CUSTOMER_WEIGHT_FORMULA_VARIABLES,
  formulaPresetSummary,
  formulaPresetValue,
  toDisplayFormula,
  toEngineFormula,
  type CustomerWeightFormulaPreset,
} from './customerWeightFormulaModel'
import CustomerSpecNumberInput from './CustomerSpecNumberInput'

interface Props { disabled: boolean; pending?: boolean; value: WeightRule; onChange: (value: WeightRule) => void; onApply: () => void }

const modes = [
  { label: '保持', value: 'KEEP' }, { label: '固定值', value: 'FIXED' },
  { label: '加减', value: 'DELTA' }, { label: '比例', value: 'RATIO' },
  { label: '公式', value: 'FORMULA' }, { label: '逐件录入', value: 'MANUAL' },
] satisfies Array<{ label: string; value: CustomerWeightMode }>

const formulaPresetOptions = [
  ...CUSTOMER_WEIGHT_FORMULA_PRESETS.map(({ label, value }) => ({ label, value })),
  { label: '自定义公式', value: 'CUSTOM' },
]
const formulaOperators = ['+', '-', '×', '÷', '(', ')']

export default function CustomerWeightRulePanel({ disabled, pending = false, value, onChange, onApply }: Props) {
  const change = (next: Partial<WeightRule>) => onChange({ ...value, ...next })
  const changeMode = (mode: CustomerWeightMode) => change({
    mode,
    formula: mode === 'FORMULA' && !value.formula ? STANDARD_WEIGHT_FORMULA : value.formula,
  })
  return (
    <div className="customer-weight-rule-panel">
      <div className="customer-weight-rule-panel__head">
        <Typography.Text strong>客户重量规则</Typography.Text>
        <Segmented<CustomerWeightMode> aria-label="客户重量计算方式" options={modes} value={value.mode} onChange={changeMode} />
        {pending && <Tag color="gold">规则待应用</Tag>}
      </div>
      <div className="customer-weight-rule-panel__editor">
        {value.mode === 'FORMULA'
          ? <FormulaRuleInput value={value} onChange={change} />
          : <RuleInput value={value} onChange={change} />}
        <Button className="customer-weight-rule-panel__apply" icon={<CalculatorOutlined />} disabled={disabled || !isWeightRuleReady(value)} onClick={onApply}>应用当前规则</Button>
      </div>
    </div>
  )
}

function RuleInput({ value, onChange }: { value: WeightRule; onChange: (value: Partial<WeightRule>) => void }) {
  if (value.mode === 'FIXED') return <CustomerSpecNumberInput min={1} max={1e12} precision={0} unit="kg/件" placeholder="统一重量" value={value.operand} onChange={(operand) => onChange({ operand })} />
  if (value.mode === 'DELTA') return <CustomerSpecNumberInput min={-1e12} max={1e12} precision={0} unit="kg/件" placeholder="加减重量" value={value.operand} onChange={(operand) => onChange({ operand })} />
  if (value.mode === 'RATIO') return <CustomerSpecNumberInput min={0.000001} max={1e6} unit="倍" placeholder="重量比例" value={value.operand} onChange={(operand) => onChange({ operand })} />
  return <Typography.Text type="secondary">{value.mode === 'MANUAL' ? '在下表逐件填写客户重量' : '保留当前客户单据重量'}</Typography.Text>
}

function FormulaRuleInput({ value, onChange }: {
  value: WeightRule
  onChange: (value: Partial<WeightRule>) => void
}) {
  const preset = formulaPresetValue(value.formula)
  const selectPreset = (next: CustomerWeightFormulaPreset) => {
    const matched = CUSTOMER_WEIGHT_FORMULA_PRESETS.find((item) => item.value === next)
    onChange({ formula: matched?.formula ?? '' })
  }
  const append = (token: string) => onChange({ formula: appendFormulaToken(value.formula, token) })

  return (
    <div className="customer-formula-editor">
      <div className="customer-formula-editor__head">
        <div>
          <Typography.Text strong>客户重量计算表达式</Typography.Text>
          <Typography.Text type="secondary">结果单位：kg/件</Typography.Text>
        </div>
        <Select aria-label="常用公式模板" value={preset} options={formulaPresetOptions} onChange={selectPreset} />
      </div>
      <Input.TextArea
        className="customer-formula-editor__input"
        aria-label="客户重量公式"
        autoSize={{ minRows: 2, maxRows: 4 }}
        placeholder="选择常用模板，或使用下方变量和运算符编辑"
        value={toDisplayFormula(value.formula)}
        onChange={(event) => onChange({ formula: toEngineFormula(event.target.value) })}
      />
      <FormulaTokens onAppend={append} />
      <div className="customer-formula-editor__foot">
        <Typography.Text type="secondary">{formulaPresetSummary(value.formula)}</Typography.Text>
        <span className="customer-zero-policy">
          <Switch aria-label="遇到零值时保留原重量" size="small" checked={value.skipZero} onChange={(skipZero) => onChange({ skipZero })} />
          遇到零值时保留原重量
        </span>
      </div>
    </div>
  )
}

function FormulaTokens({ onAppend }: { onAppend: (token: string) => void }) {
  return (
    <div className="customer-formula-editor__tokens">
      <Typography.Text type="secondary">可用变量</Typography.Text>
      <Space size={5} wrap>
        {CUSTOMER_WEIGHT_FORMULA_VARIABLES.map((variable) => (
          <Tooltip key={variable.key} title={variable.description}>
            <Button size="small" onClick={() => onAppend(variable.label)}>{variable.label}</Button>
          </Tooltip>
        ))}
      </Space>
      <Typography.Text type="secondary">运算符</Typography.Text>
      <Space.Compact>
        {formulaOperators.map((operator) => <Button key={operator} size="small" onClick={() => onAppend(operator)}>{operator}</Button>)}
      </Space.Compact>
    </div>
  )
}
