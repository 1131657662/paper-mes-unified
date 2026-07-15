import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import { Button, Empty, Form, Input, InputNumber, Select, Space, Typography } from 'antd'
import MesTooltip from '../../../components/biz/MesTooltip'
import type { BackRecordFormValues } from './backRecordUtils'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'
import type { BackRecordSourceOption } from './BackRecordFinishFields'

interface Props {
  item: BackRecordWorkItem
  onFieldExhausted: () => void
  sourceOptions: BackRecordSourceOption[]
}

export default function BackRecordTrimEntryList({ item, onFieldExhausted, sourceOptions }: Props) {
  const options = itemSourceOptions(item, sourceOptions)
  if (item.kind !== 'roll' || options.length === 0) return null
  return (
    <section className="back-record-panel back-record-trim-panel">
      <Form.List name={['trims', item.key]}>
        {(fields, { add, remove }) => (
          <>
            <div className="back-record-panel__head">
              <Typography.Text strong>切边 / 余料</Typography.Text>
              <Button size="small" icon={<PlusOutlined />} onClick={() => add(defaultTrim(options))}>新增切边</Button>
            </div>
            {fields.length === 0 ? (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="本次没有保留切边" />
            ) : (
              <div className="back-record-trim-list">
                {fields.map((field, index) => (
                  <TrimRow
                    key={field.key}
                    fieldName={field.name}
                    index={index}
                    itemKey={item.key}
                    onFieldExhausted={onFieldExhausted}
                    options={options}
                    onRemove={() => remove(field.name)}
                  />
                ))}
              </div>
            )}
          </>
        )}
      </Form.List>
    </section>
  )
}

interface TrimRowProps {
  fieldName: number
  index: number
  itemKey: string
  onFieldExhausted: () => void
  onRemove: () => void
  options: BackRecordSourceOption[]
}

function TrimRow(props: TrimRowProps) {
  const form = Form.useFormInstance<BackRecordFormValues>()
  const sourcePath = ['trims', props.itemKey, props.fieldName, 'originalUuid'] as const
  const sourceUuid = Form.useWatch(sourcePath, form)
  const maxWidth = props.options.find((option) => option.value === sourceUuid)?.maxWidth
  return (
    <div className="back-record-trim-row">
      <div className="back-record-trim-row__identity">切边 {props.index + 1}</div>
      <Form.Item name={[props.fieldName, 'originalUuid']} label="来源母卷" rules={[{ required: true, message: '请选择来源母卷' }]}>
        <Select options={props.options} disabled={props.options.length === 1} />
      </Form.Item>
      <Form.Item name={[props.fieldName, 'finishWidth']} label="切边宽度" rules={widthRules(maxWidth)}>
        <InputNumber data-back-record-field="true" min={1} max={maxWidth} precision={0} suffix="mm" />
      </Form.Item>
      <Form.Item name={[props.fieldName, 'actualWeight']} label="实际重量" rules={weightRules}>
        <InputNumber data-back-record-field="true" min={0.001} precision={3} suffix="kg" />
      </Form.Item>
      <Form.Item name={[props.fieldName, 'actualRemark']} label="备注">
        <Input data-back-record-field="true" placeholder="余料状态或去向" onPressEnter={props.onFieldExhausted} />
      </Form.Item>
      <Space align="end" className="back-record-trim-row__action">
        <MesTooltip title="删除切边">
          <Button
            danger
            aria-label={`删除切边 ${props.index + 1}`}
            type="text"
            icon={<DeleteOutlined />}
            onClick={props.onRemove}
          />
        </MesTooltip>
      </Space>
    </div>
  )
}

function itemSourceOptions(item: BackRecordWorkItem, options: BackRecordSourceOption[]) {
  const sourceUuids = new Set(item.rollProductions.map((production) => production.originalUuid))
  if (item.roll) sourceUuids.add(item.roll.uuid)
  return options.filter((option) => option.processMode === 2 && sourceUuids.has(option.value))
}

function defaultTrim(options: BackRecordSourceOption[]) {
  return { originalUuid: options.length === 1 ? options[0]?.value : undefined }
}

function widthRules(maxWidth?: number) {
  return [
    { required: true, message: '请输入切边宽度' },
    { type: 'number' as const, min: 1, max: maxWidth, message: maxWidth ? `宽度范围为 1-${maxWidth}mm` : '切边宽度必须大于0' },
  ]
}

const weightRules = [
  { required: true, message: '请输入切边重量' },
  { type: 'number' as const, min: 0.001, message: '切边重量必须大于0' },
]
