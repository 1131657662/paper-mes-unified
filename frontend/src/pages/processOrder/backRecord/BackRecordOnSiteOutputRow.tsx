import { DeleteOutlined } from '@ant-design/icons'
import { Button, Form, Input, InputNumber, Popconfirm, Select, Tag } from 'antd'
import MesTooltip from '../../../components/biz/MesTooltip'
import type { BackRecordFormValues, OnSiteOutputRecordValues } from './backRecordUtils'
import type { BackRecordSourceOption } from './BackRecordFinishFields'

interface Props {
  fieldName: number
  index: number
  itemKey: string
  onFieldExhausted: () => void
  onRemove: () => void
  options: BackRecordSourceOption[]
}

export default function BackRecordOnSiteOutputRow(props: Props) {
  const form = Form.useFormInstance<BackRecordFormValues>()
  const path = ['onSiteOutputs', props.itemKey, props.fieldName] as const
  const output = Form.useWatch(path, form) as OnSiteOutputRecordValues | undefined
  const source = props.options.find((option) => option.value === output?.originalUuid)
  const isTrim = output?.outputType === 'TRIM'
  return (
    <div className={`back-record-output-row${isTrim ? ' is-trim' : ''}`}>
      <OutputIdentity index={props.index} output={output} />
      <HiddenFields fieldName={props.fieldName} />
      <Form.Item name={[props.fieldName, 'originalUuid']} label="来源母卷" rules={[{ required: true, message: '请选择来源母卷' }]}>
        <Select options={props.options} disabled={props.options.length === 1} />
      </Form.Item>
      <Form.Item name={[props.fieldName, 'finishWidth']} label={isTrim ? '切边宽度' : '成品门幅'} rules={widthRules(source?.maxWidth)}>
        <InputNumber data-back-record-field="true" min={1} max={source?.maxWidth} precision={0} suffix="mm" />
      </Form.Item>
      {!isTrim && <SpecFields fieldName={props.fieldName} />}
      <Form.Item name={[props.fieldName, 'actualWeight']} label="实际重量" rules={weightRules}>
        <InputNumber data-back-record-field="true" min={0.001} precision={3} suffix="kg" />
      </Form.Item>
      <Form.Item name={[props.fieldName, 'actualRemark']} label="备注">
        <Input data-back-record-field="true" placeholder={isTrim ? '余料状态或去向' : '实际工艺或异常说明'} onPressEnter={props.onFieldExhausted} />
      </Form.Item>
      <OutputDeleteButton output={output} onRemove={props.onRemove} />
    </div>
  )
}

function OutputDeleteButton({
  output,
  onRemove,
}: {
  output?: OnSiteOutputRecordValues
  onRemove: () => void
}) {
  const accessibleName = output?.uuid
    ? `移除实际产出 ${output.finishRollNo || '预占卷号'}`
    : '删除本条实际产出'
  const button = (
    <Button
      danger
      aria-label={accessibleName}
      type="text"
      icon={<DeleteOutlined />}
      onClick={output?.uuid ? undefined : onRemove}
    />
  )
  if (!output?.uuid) return <MesTooltip title="删除本条实际产出">{button}</MesTooltip>
  return (
    <Popconfirm
      title={`确认移除预占卷号 ${output.finishRollNo || ''}？`}
      description="提交回录后，该预占卷号将自动作废。"
      okText="确认移除"
      cancelText="取消"
      onConfirm={onRemove}
    >
      <MesTooltip title="移除并在提交时作废预占卷号">{button}</MesTooltip>
    </Popconfirm>
  )
}

function HiddenFields({ fieldName }: { fieldName: number }) {
  return (
    <>
      <Form.Item name={[fieldName, 'uuid']} hidden><Input /></Form.Item>
      <Form.Item name={[fieldName, 'finishRollNo']} hidden><Input /></Form.Item>
      <Form.Item name={[fieldName, 'outputType']} hidden><Input /></Form.Item>
    </>
  )
}

function OutputIdentity({ index, output }: { index: number; output?: OnSiteOutputRecordValues }) {
  const isTrim = output?.outputType === 'TRIM'
  return (
    <div className="back-record-output-row__identity">
      <Tag color={isTrim ? 'orange' : 'blue'}>{isTrim ? '切边/余料' : '成品'}</Tag>
      <strong>{output?.finishRollNo || `${isTrim ? '余料' : '新成品'} ${index + 1}`}</strong>
      {output?.uuid && <span>原预占号</span>}
    </div>
  )
}

function SpecFields({ fieldName }: { fieldName: number }) {
  return (
    <>
      <Form.Item name={[fieldName, 'finishDiameter']} label="实际直径">
        <InputNumber data-back-record-field="true" min={1} precision={0} suffix="in" />
      </Form.Item>
      <Form.Item name={[fieldName, 'finishCoreDiameter']} label="实际纸芯">
        <InputNumber data-back-record-field="true" min={1} precision={0} suffix="in" />
      </Form.Item>
    </>
  )
}

function widthRules(maxWidth?: number) {
  return [
    { required: true, message: '请输入实际宽度' },
    { type: 'number' as const, min: 1, max: maxWidth, message: maxWidth ? `宽度范围为 1-${maxWidth}mm` : '宽度必须大于0' },
  ]
}

const weightRules = [
  { required: true, message: '请输入实际重量' },
  { type: 'number' as const, min: 0.001, message: '实际重量必须大于0' },
]
