import { useState } from 'react'
import { Input, InputNumber, Switch, Typography } from 'antd'
import type { RewindLayoutItemPlanDTO } from '../../../types/processOrder'
import { formatGram, formatMm } from '../../../utils/numberFormatters'
import type { RollDraft } from '../types'

interface Props {
  index: number
  item: RewindLayoutItemPlanDTO
  roll: RollDraft
  onChange: (patch: Partial<RewindLayoutItemPlanDTO>) => void
}

export default function RewindCustomerSpecificationFields({ index, item, roll, onChange }: Props) {
  const [opened, setOpened] = useState(false)
  const hasValues = hasCustomerValues(item)
  const enabled = opened || hasValues
  const reasonRequired = customerSpecificationDiffers(item, roll)

  return (
    <div className="rewind-customer-spec">
      <div className="rewind-customer-spec__header">
        <div>
          <Typography.Text strong>客户销售规格</Typography.Text>
          <Typography.Text type="secondary">
            {enabled ? '单独标注' : `与物理规格一致 · ${physicalSummary(item, roll)}`}
          </Typography.Text>
        </div>
        <Switch aria-label={`排布 ${index + 1} 使用客户销售规格`} checked={enabled}
          onChange={(checked) => toggleCustomerSpecification(checked, setOpened, onChange)} />
      </div>
      {enabled && <div className="rewind-customer-grid">
        <label className="rewind-field rewind-field--wide">
          <span className="rewind-field__label">客户品名</span>
          <Input value={item.customerPaperName} placeholder={roll.paperName || '请输入品名'}
            aria-label={`排布 ${index + 1} 客户品名`}
            onChange={(event) => onChange({ customerPaperName: valueOrUndefined(event.target.value) })} />
        </label>
        <label className="rewind-field">
          <span className="rewind-field__label">客户克重</span>
          <InputNumber min={1} suffix="g" value={item.customerGramWeight}
            placeholder={String(roll.gramWeight ?? '')} aria-label={`排布 ${index + 1} 客户克重`}
            onChange={(value) => onChange({ customerGramWeight: value ?? undefined })} />
        </label>
        <label className="rewind-field">
          <span className="rewind-field__label">客户门幅</span>
          <InputNumber min={1} suffix="mm" value={item.customerFinishWidth}
            placeholder={String(item.width)} aria-label={`排布 ${index + 1} 客户门幅`}
            onChange={(value) => onChange({ customerFinishWidth: value ?? undefined })} />
        </label>
        {(reasonRequired || item.customerSpecOverrideReason) && <label className="rewind-field rewind-field--reason">
          <span className="rewind-field__label">改写原因</span>
          <Input status={reasonRequired && !item.customerSpecOverrideReason?.trim() ? 'error' : undefined}
            value={item.customerSpecOverrideReason} placeholder="必填，例如：按客户合同标签标注"
            aria-label={`排布 ${index + 1} 客户规格改写原因`}
            onChange={(event) => onChange({ customerSpecOverrideReason: valueOrUndefined(event.target.value) })} />
          {reasonRequired && !item.customerSpecOverrideReason?.trim()
            && <Typography.Text type="danger">销售规格与物理规格不同时必须填写</Typography.Text>}
        </label>}
      </div>}
    </div>
  )
}

function toggleCustomerSpecification(
  checked: boolean,
  setOpened: (value: boolean) => void,
  onChange: Props['onChange'],
) {
  setOpened(checked)
  if (!checked) onChange({
    customerPaperName: undefined,
    customerGramWeight: undefined,
    customerFinishWidth: undefined,
    customerSpecOverrideReason: undefined,
  })
}

function hasCustomerValues(item: RewindLayoutItemPlanDTO) {
  return Boolean(item.customerPaperName || item.customerGramWeight
    || item.customerFinishWidth || item.customerSpecOverrideReason)
}

function customerSpecificationDiffers(item: RewindLayoutItemPlanDTO, roll: RollDraft) {
  return differs(item.customerPaperName?.trim(), roll.paperName)
    || differs(item.customerGramWeight, roll.gramWeight)
    || differs(item.customerFinishWidth, item.width)
}

function differs(customer: string | number | undefined, physical: string | number | undefined) {
  return customer != null && customer !== '' && customer !== physical
}

function physicalSummary(item: RewindLayoutItemPlanDTO, roll: RollDraft) {
  return `${roll.paperName || '-'} / ${formatGram(roll.gramWeight)} / ${formatMm(item.width)}`
}

function valueOrUndefined(value: string) {
  return value || undefined
}
