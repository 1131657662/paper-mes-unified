import { Alert, Form, Input, InputNumber, Table, Tag, Typography } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { formatKg } from '../../../features/processOrderDetail/orderDetailUtils'
import { formatGram, formatMm, formatOptionalKg } from '../../../utils/numberFormatters'
import { focusNextBackRecordField } from './backRecordKeyboard'
import {
  sourceActualWeight,
  sourceEstimatedWeight,
  sourceIsMeasured,
  sourceWeightSummary,
  type BackRecordSourceRoll,
} from './backRecordSourceRolls'
import type { BackRecordFormValues } from './backRecordUtils'
import { requiresMeasuredSourceWeights } from './backRecordWeightPolicy'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

interface Props {
  item: BackRecordWorkItem
  onFieldExhausted: () => void
}

export default function BackRecordMergeSummary({ item, onFieldExhausted }: Props) {
  const form = Form.useFormInstance<BackRecordFormValues>()
  const values = Form.useWatch('rolls', { form, preserve: true }) ?? {}
  if (!item.isMergeGroup) return null
  const formValues: BackRecordFormValues = { rolls: values }
  const summary = sourceWeightSummary(item, formValues)
  const required = requiresMeasuredSourceWeights(item)
  const missingCount = required ? summary.measuredMissingCount : summary.missingCount
  return (
    <section className="back-record-panel back-record-merge-summary">
      <div className="back-record-panel__head">
        <Typography.Text strong>{item.title}: 逐卷复称录入</Typography.Text>
        <Typography.Text type="secondary">
          已录入 {summary.sources.length - missingCount}/{summary.sources.length}
          {summary.recordedTotal > 0 ? `，当前合计 ${formatKg(summary.recordedTotal)}` : ''}
        </Typography.Text>
      </div>
      <WeightNotice required={required} missingCount={missingCount} />
      <Table
        size="small"
        pagination={false}
        rowKey="uuid"
        dataSource={summary.sources}
        columns={columns({ formValues, onFieldExhausted, required })}
        scroll={{ x: 1120 }}
      />
    </section>
  )
}

interface ColumnOptions {
  formValues: BackRecordFormValues
  onFieldExhausted: () => void
  required: boolean
}

function columns(options: ColumnOptions): ColumnsType<BackRecordSourceRoll> {
  return [
    { title: '来源母卷', width: 190, fixed: 'left', render: (_, source, index) => <SourceIdentity source={source} index={index} values={options.formValues} /> },
    { title: '标称/参考', width: 110, render: (_, source) => formatOptionalKg(sourceEstimatedWeight(source)) },
    { title: '实测克重', width: 150, render: (_, source) => <NumberField source={source} field="actualGramWeight" min={1} suffix="g" onFieldExhausted={options.onFieldExhausted} /> },
    { title: '实测门幅', width: 150, render: (_, source) => <NumberField source={source} field="actualWidth" min={1} suffix="mm" onFieldExhausted={options.onFieldExhausted} /> },
    { title: options.required ? '复称重量（必填）' : '复称重量（选填）', width: 175, render: (_, source) => <NumberField source={source} field="actualWeight" min={0.001} suffix="kg" required={options.required} onFieldExhausted={options.onFieldExhausted} /> },
    { title: '复核说明', width: 230, render: (_, source) => <RemarkField source={source} onFieldExhausted={options.onFieldExhausted} /> },
  ]
}

function SourceIdentity({ source, index, values }: { source: BackRecordSourceRoll; index: number; values: BackRecordFormValues }) {
  const weight = sourceActualWeight(source, values)
  const measured = sourceIsMeasured(source, values)
  const label = measured ? '已实测' : weight != null && weight > 0 ? '已带入标称' : '待称重'
  const color = measured ? 'success' : weight != null && weight > 0 ? 'processing' : 'warning'
  return (
    <div className="back-record-merge-source">
      <Typography.Text strong>{source.rollNo || source.extraNo || `来源母卷 ${index + 1}`}</Typography.Text>
      <Typography.Text type="secondary">{source.paperName || '-'} / {formatGram(source.gramWeight)} / {formatMm(source.originalWidth)}</Typography.Text>
      <Tag color={color}>{label}</Tag>
    </div>
  )
}

interface NumberFieldProps {
  field: 'actualGramWeight' | 'actualWidth' | 'actualWeight'
  min: number
  onFieldExhausted: () => void
  required?: boolean
  source: BackRecordSourceRoll
  suffix: string
}

function NumberField({ field, min, onFieldExhausted, required, source, suffix }: NumberFieldProps) {
  const form = Form.useFormInstance<BackRecordFormValues>()
  return (
    <>
      {field === 'actualWeight' && <Form.Item name={['rolls', source.uuid, 'weightEntryMode']} hidden><Input /></Form.Item>}
      <Form.Item name={['rolls', source.uuid, field]} rules={required ? [{ required: true, message: '必填' }] : undefined}>
      <InputNumber
        aria-label={`${source.rollNo || source.extraNo || source.uuid} ${fieldLabel(field)}`}
        data-back-record-field="true"
        min={min}
        placeholder={suffix}
        suffix={suffix}
        onChange={field === 'actualWeight'
          ? (value) => queueMicrotask(() => form.setFieldValue(
              ['rolls', source.uuid, 'weightEntryMode'], value != null && value > 0 ? 'MEASURED' : undefined,
            ))
          : undefined}
        onPressEnter={(event) => focusNextBackRecordField(event, onFieldExhausted)}
      />
      </Form.Item>
    </>
  )
}

function RemarkField({ source, onFieldExhausted }: { source: BackRecordSourceRoll; onFieldExhausted: () => void }) {
  return (
    <Form.Item name={['rolls', source.uuid, 'remark']}>
      <Input
        aria-label={`${source.rollNo || source.extraNo || source.uuid} 复核说明`}
        data-back-record-field="true"
        placeholder="破损、水湿、复称差异"
        onPressEnter={(event) => focusNextBackRecordField(event, onFieldExhausted)}
      />
    </Form.Item>
  )
}

function WeightNotice({ required, missingCount }: { required: boolean; missingCount: number }) {
  if (missingCount === 0) return null
  return (
    <Alert
      showIcon
      type={required ? 'warning' : 'info'}
      message={required
        ? `还有 ${missingCount} 卷未完成实测；标准吨位复卷完成和正式计费前必须逐卷补齐。`
        : `还有 ${missingCount} 卷未录入重量；当前计价模式不依赖重量，提交后的重量闭合会标记为未核验。`}
    />
  )
}

function fieldLabel(field: NumberFieldProps['field']): string {
  if (field === 'actualWeight') return '复称重量'
  return field === 'actualWidth' ? '实测门幅' : '实测克重'
}
