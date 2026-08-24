import { Button, Input, InputNumber, Select, Space, Typography } from 'antd'
import { ReloadOutlined } from '@ant-design/icons'
import { useState } from 'react'
import type { ProcessAiCorrection, ProcessAiParseResult } from '../types'

interface Props {
  result: ProcessAiParseResult
  loading: boolean
  onSubmit: (corrections: ProcessAiCorrection[]) => Promise<void>
}

export default function ProcessAiCorrectionPanel({ result, loading, onSubmit }: Props) {
  const assignments = result.result?.assignments ?? []
  const [coreValues, setCoreValues] = useState<Record<string, number | undefined>>({})
  const [widthValues, setWidthValues] = useState<Record<string, number | undefined>>({})
  const [scopes, setScopes] = useState<Record<string, string | undefined>>({})
  const [customerValues, setCustomerValues] = useState<Record<string, CustomerDraft>>({})
  const corrections = assignments.flatMap((assignment) => {
    const rewind = recordValue(assignment.rewindIntent)
    const values: ProcessAiCorrection[] = []
    if (rewind) addRewindCorrections(values, assignment.ownerRollRef,
      coreValues[assignment.ownerRollRef], widthValues[assignment.ownerRollRef],
      scopes[assignment.ownerRollRef])
    for (const spec of assignment.customerSpecs ?? []) {
      const draft = customerValues[customerKey(assignment.ownerRollRef, spec.outputIndex)]
      if (draft?.paperName) values.push({ assignmentRef: assignment.ownerRollRef,
        field: 'customerPaperName', textValue: draft.paperName, outputIndex: spec.outputIndex })
      if (draft?.gramWeight != null) values.push({ assignmentRef: assignment.ownerRollRef,
        field: 'customerGramWeight', value: draft.gramWeight, outputIndex: spec.outputIndex })
      if (draft?.finishWidth != null) values.push({ assignmentRef: assignment.ownerRollRef,
        field: 'customerFinishWidth', value: draft.finishWidth, unit: 'mm', outputIndex: spec.outputIndex })
      if (draft?.overrideReason) values.push({ assignmentRef: assignment.ownerRollRef,
        field: 'customerSpecOverrideReason', textValue: draft.overrideReason, outputIndex: spec.outputIndex })
    }
    return values
  })
  if (assignments.length === 0) return null
  return <section className="process-ai-correction-panel">
    <div>
      <Typography.Text strong>需要调整时，可直接修改结构化字段</Typography.Text>
      <Typography.Paragraph type="secondary">修改后后端会重新绑定母卷、校验工艺并生成新的预览版本。</Typography.Paragraph>
    </div>
    {assignments.map((assignment) => <CorrectionRow key={assignment.ownerRollRef}
      assignment={assignment} core={coreValues[assignment.ownerRollRef]}
      width={widthValues[assignment.ownerRollRef]} scope={scopes[assignment.ownerRollRef]}
      onCore={(value) => setCoreValues((current) => ({ ...current, [assignment.ownerRollRef]: value }))}
      onWidth={(value) => setWidthValues((current) => ({ ...current, [assignment.ownerRollRef]: value }))}
      onScope={(value) => setScopes((current) => ({ ...current, [assignment.ownerRollRef]: value }))}
      customerValues={customerValues} onCustomer={(key, patch) => setCustomerValues((current) => ({
        ...current, [key]: { ...current[key], ...patch },
      }))} />)}
    <Button icon={<ReloadOutlined />} loading={loading} disabled={corrections.length === 0}
      onClick={() => void onSubmit(corrections)}>重新生成工艺预览</Button>
  </section>
}

function CorrectionRow({ assignment, core, width, scope, onCore, onWidth, onScope,
  customerValues, onCustomer }: {
  assignment: ProcessAiParseResult['result']['assignments'][number]
  core?: number
  width?: number
  scope?: string
  onCore: (value: number | undefined) => void
  onWidth: (value: number | undefined) => void
  onScope: (value: string | undefined) => void
  customerValues: Record<string, CustomerDraft>
  onCustomer: (key: string, patch: CustomerDraft) => void
}) {
  const rewind = recordValue(assignment.rewindIntent)
  const quantity = rewind ? recordValue(rewind.quantityIntent) : undefined
  return <div className="process-ai-correction-row">
      <Typography.Text strong>{assignment.ownerRollRef}</Typography.Text>
    <Space wrap>
      {rewind && <label>成品纸芯（英寸）<InputNumber min={0.1} step={0.5} value={core}
        placeholder={measurementValue(rewind.core)} onChange={(value) => onCore(value ?? undefined)} /></label>}
      {rewind && <label>成品门幅（mm）<InputNumber min={1} step={1} value={width}
        placeholder={widthValue(rewind.widthRule)} onChange={(value) => onWidth(value ?? undefined)} /></label>}
      {quantity && <label>数量范围<Select allowClear value={scope} placeholder={textValue(quantity.scope)}
        options={[{ value: 'PER_SOURCE', label: '每条母卷' }, { value: 'TOTAL', label: '全单合计' }]}
        onChange={onScope} /></label>}
    </Space>
    {(assignment.customerSpecs ?? []).map((spec) => <CustomerSpecRow key={spec.outputIndex}
      assignmentRef={assignment.ownerRollRef} spec={spec}
      value={customerValues[customerKey(assignment.ownerRollRef, spec.outputIndex)]}
      onChange={(patch) => onCustomer(customerKey(assignment.ownerRollRef, spec.outputIndex), patch)} />)}
  </div>
}

function CustomerSpecRow({ assignmentRef, spec, value, onChange }: {
  assignmentRef: string
  spec: NonNullable<ProcessAiParseResult['result']['assignments'][number]['customerSpecs']>[number]
  value?: CustomerDraft
  onChange: (patch: CustomerDraft) => void
}) {
  return <Space wrap className="process-ai-customer-spec-correction">
    <Typography.Text>第{spec.outputIndex + 1}件客户规格</Typography.Text>
    <Input placeholder={spec.paperName ?? '客户品名'} value={value?.paperName}
      onChange={(event) => onChange({ paperName: event.target.value })} />
    <InputNumber min={1} placeholder={spec.gramWeight == null ? '客户克重' : String(spec.gramWeight)}
      value={value?.gramWeight} onChange={(next) => onChange({ gramWeight: next ?? undefined })} />
    <InputNumber min={1} placeholder={spec.finishWidth == null ? '客户门幅' : String(spec.finishWidth)}
      value={value?.finishWidth} onChange={(next) => onChange({ finishWidth: next ?? undefined })} />
    <Input placeholder={spec.overrideReason ?? '规格改写原因'} value={value?.overrideReason}
      onChange={(event) => onChange({ overrideReason: event.target.value })}
      aria-label={`${assignmentRef} 第${spec.outputIndex + 1}件客户规格改写原因`} />
  </Space>
}

function addRewindCorrections(values: ProcessAiCorrection[], assignmentRef: string,
  core?: number, width?: number, scope?: string) {
  if (core != null) values.push({ assignmentRef, field: 'finishCoreDiameter', value: core, unit: 'inch' })
  if (width != null) values.push({ assignmentRef, field: 'widthMm', value: width, unit: 'mm' })
  if (scope) values.push({ assignmentRef, field: 'quantityScope', textValue: scope })
}

function customerKey(assignmentRef: string, outputIndex: number) {
  return `${assignmentRef}:${outputIndex}`
}

interface CustomerDraft {
  paperName?: string
  gramWeight?: number
  finishWidth?: number
  overrideReason?: string
}

function recordValue(value: unknown): Record<string, unknown> | undefined {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
    ? value as Record<string, unknown> : undefined
}

function measurementValue(value: unknown) {
  const measurement = recordValue(value)
  return typeof measurement?.value === 'number' ? String(measurement.value) : undefined
}

function widthValue(value: unknown) {
  const width = recordValue(value)
  const values = width?.values
  return Array.isArray(values) && typeof values[0] === 'number' ? String(values[0]) : undefined
}

function textValue(value: unknown) {
  return typeof value === 'string' ? value : undefined
}
