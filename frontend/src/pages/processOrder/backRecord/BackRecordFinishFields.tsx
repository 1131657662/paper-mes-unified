import { SettingOutlined } from '@ant-design/icons'
import { Button, Form, Input, InputNumber, Popover, Select } from 'antd'
import { IS_REMAIN } from '../../../constants/processOrder'
import type { FinishRoll } from '../../../types/processOrder'
import { focusNextBackRecordField } from './backRecordKeyboard'
import type { BackRecordFormValues } from './backRecordUtils'

interface Props {
  abnormalTypeOptions: Array<{ label: string; value: number | string }>
  context: FinishFieldContext
  finish: FinishRoll
  onActualWeightChange: (value: number | null) => void
  onFieldExhausted: () => void
}

export interface BackRecordSourceOption {
  label: string
  maxWidth?: number
  processMode: number
  value: string
}

interface FinishFieldContext {
  needsSource: boolean
  maxWidth?: number
  onSite: boolean
  sourceOptions: BackRecordSourceOption[]
}

export default function BackRecordFinishFields(props: Props) {
  const form = Form.useFormInstance<BackRecordFormValues>()
  const selectedSource = Form.useWatch(['finishes', props.finish.uuid, 'originalUuid'], form)
  const selectedMode = props.context.sourceOptions.find((option) => option.value === selectedSource)?.processMode
  const selectedWidth = props.context.sourceOptions.find((option) => option.value === selectedSource)?.maxWidth
  const onSite = props.context.onSite || selectedMode === 2
  return (
    <div className="back-record-finish-row__fields">
      {props.context.needsSource && <SourceField finish={props.finish} options={props.context.sourceOptions} />}
      {onSite && <FinishWidthField finish={props.finish} maxWidth={selectedWidth ?? props.context.maxWidth} onFieldExhausted={props.onFieldExhausted} />}
      <ActualWeightField {...props} />
      <Form.Item name={['finishes', props.finish.uuid, 'scrapWeight']} label="报废/损耗">
        <InputNumber data-back-record-field="true" min={0} placeholder="kg" suffix="kg" onPressEnter={(event) => focusNextBackRecordField(event, props.onFieldExhausted)} />
      </Form.Item>
      <Form.Item name={['finishes', props.finish.uuid, 'isRemain']} label="属性">
        <Select disabled options={remainOptions} />
      </Form.Item>
      <Form.Item name={['finishes', props.finish.uuid, 'isAbnormal']} label="异常">
        <Select options={abnormalOptions} />
      </Form.Item>
      <Form.Item name={['finishes', props.finish.uuid, 'abnormalType']} label="异常类型">
        <Select allowClear options={props.abnormalTypeOptions} placeholder="请选择" />
      </Form.Item>
      {onSite && props.finish.isRemain !== 1 && <OptionalSpecField finish={props.finish} />}
      <Form.Item name={['finishes', props.finish.uuid, 'actualRemark']} label="备注">
        <Input data-back-record-field="true" placeholder="车间说明" onPressEnter={(event) => focusNextBackRecordField(event, props.onFieldExhausted)} />
      </Form.Item>
    </div>
  )
}

function SourceField({ finish, options }: { finish: FinishRoll; options: BackRecordSourceOption[] }) {
  return (
    <Form.Item name={['finishes', finish.uuid, 'originalUuid']} label="来源母卷" rules={[{ required: true, message: '必填' }]}>
      <Select options={options} placeholder="请选择" />
    </Form.Item>
  )
}

function FinishWidthField({ finish, maxWidth, onFieldExhausted }: { finish: FinishRoll; maxWidth?: number; onFieldExhausted: () => void }) {
  const form = Form.useFormInstance<BackRecordFormValues>()
  const validateWidth = (_: unknown, value?: number) => {
    const weight = form.getFieldValue(['finishes', finish.uuid, 'actualWeight'])
    if (finish.isSpare === 1 && !weight && !value) return Promise.resolve()
    if (!value || value <= 0) return Promise.reject(new Error('必填'))
    return maxWidth && value > maxWidth ? Promise.reject(new Error(`不得超过 ${maxWidth}mm`)) : Promise.resolve()
  }
  return (
    <Form.Item name={['finishes', finish.uuid, 'finishWidth']} label={finish.isRemain === 1 ? '切边宽度' : '成品门幅'} rules={[{ validator: validateWidth }]}>
      <InputNumber data-back-record-field="true" min={1} max={maxWidth} precision={0} placeholder="现场确认" suffix="mm" onPressEnter={(event) => focusNextBackRecordField(event, onFieldExhausted)} />
    </Form.Item>
  )
}

function ActualWeightField({ finish, onActualWeightChange, onFieldExhausted }: Props) {
  const rules = finish.isSpare === 1 ? undefined : [
    { required: true, message: '必填' },
    { type: 'number' as const, min: 0.001, message: '需大于0' },
  ]
  return (
    <Form.Item name={['finishes', finish.uuid, 'actualWeight']} label={finish.isRemain === 1 ? '余料重量' : '实际重量'} rules={rules}>
      <InputNumber data-back-record-field="true" min={0} placeholder={finish.isSpare === 1 ? '未用留空' : 'kg'} suffix="kg" onChange={onActualWeightChange} onPressEnter={(event) => focusNextBackRecordField(event, onFieldExhausted)} />
    </Form.Item>
  )
}

function OptionalSpecField({ finish }: { finish: FinishRoll }) {
  const content = (
    <div className="back-record-optional-specs">
      <Form.Item name={['finishes', finish.uuid, 'finishDiameter']} label="直径">
        <InputNumber min={1} precision={0} suffix="英寸" />
      </Form.Item>
      <Form.Item name={['finishes', finish.uuid, 'finishCoreDiameter']} label="纸芯">
        <InputNumber min={1} precision={0} suffix="英寸" />
      </Form.Item>
    </div>
  )
  return (
    <Form.Item label="更多规格">
      <Popover trigger="click" placement="bottomRight" title="现场可选规格" content={content}>
        <Button icon={<SettingOutlined />}>直径 / 纸芯</Button>
      </Popover>
    </Form.Item>
  )
}

const remainOptions = Object.entries(IS_REMAIN).map(([value, label]) => ({ value: Number(value), label }))
const abnormalOptions = [{ value: 0, label: '否' }, { value: 1, label: '是' }]
