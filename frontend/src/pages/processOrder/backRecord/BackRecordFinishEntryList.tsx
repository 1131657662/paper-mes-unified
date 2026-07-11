import { useEffect, useRef } from 'react'
import { Alert, Button, Form, Input, InputNumber, Select, Tag, Typography } from 'antd'
import { CopyOutlined } from '@ant-design/icons'
import { IS_REMAIN } from '../../../constants/processOrder'
import { DICT_TYPES, abnormalFallbackOptions } from '../../../features/systemConfig/configFallbacks'
import { useDictOptions } from '../../../features/systemConfig/hooks/useRuntimeDictOptions'
import type { FinishRoll } from '../../../types/processOrder'
import type { BackRecordFormValues } from './backRecordUtils'
import { formatKg, formatMm } from '../../../utils/numberFormatters'
import { focusNextBackRecordField } from './backRecordKeyboard'
import type { BackRecordWorkItem, WorkbenchFinish } from './backRecordWorkbenchTypes'
import { autoTrimWeights } from './backRecordAutoTrim'
import { theoreticalItemFinishValues } from './backRecordTheoryFill'

interface Props {
  item: BackRecordWorkItem
  onFieldExhausted: () => void
}

export default function BackRecordFinishEntryList({ item, onFieldExhausted }: Props) {
  const form = Form.useFormInstance<BackRecordFormValues>()
  const finishes = Form.useWatch('finishes', form)
  const rolls = Form.useWatch('rolls', form)
  const steps = Form.useWatch('steps', form)
  const autoTrimUuids = useRef(new Set<string>())
  const manualTrimUuids = useRef(new Set<string>())
  const { options: abnormalTypeOptions } = useDictOptions(DICT_TYPES.abnormalType, abnormalFallbackOptions)

  useEffect(() => {
    const patches = autoTrimWeights(item, { finishes, rolls, steps }, {
      autoTrimUuids: autoTrimUuids.current,
      manualTrimUuids: manualTrimUuids.current,
    })
    for (const patch of patches) {
      const path: ['finishes', string, 'actualWeight'] = ['finishes', patch.uuid, 'actualWeight']
      const current = form.getFieldValue(path)
      if (current === patch.actualWeight) {
        autoTrimUuids.current.add(patch.uuid)
        continue
      }
      form.setFieldValue(path, patch.actualWeight)
      autoTrimUuids.current.add(patch.uuid)
    }
  }, [finishes, form, item, rolls, steps])

  const fillFinishes = () => {
    const values = theoreticalItemFinishValues(item)
    for (const [uuid, value] of Object.entries(values)) {
      form.setFieldValue(['finishes', uuid], value)
    }
  }

  return (
    <section className="back-record-panel">
      <div className="back-record-panel__head">
        <Typography.Text strong>成品实重</Typography.Text>
        <Button size="small" icon={<CopyOutlined />} onClick={fillFinishes} disabled={item.finishes.length === 0}>
          带入预估
        </Button>
      </div>
      {item.finishes.length === 0 ? (
        <Alert showIcon type="info" message="当前母卷没有已绑定成品。直发卷会在提交回录时由后端生成出库用记录。" />
      ) : (
        <div className="back-record-finish-list">
          {item.finishes.map((entry) => (
            <FinishEntryRow
              key={entry.finish.uuid}
              abnormalTypeOptions={abnormalTypeOptions}
              entry={entry}
              onActualWeightChange={(value) => {
                if (entry.finish.isRemain !== 1) return
                if (value == null) {
                  manualTrimUuids.current.delete(entry.finish.uuid)
                  autoTrimUuids.current.delete(entry.finish.uuid)
                  return
                }
                manualTrimUuids.current.add(entry.finish.uuid)
                autoTrimUuids.current.delete(entry.finish.uuid)
              }}
              onFieldExhausted={onFieldExhausted}
            />
          ))}
        </div>
      )}
    </section>
  )
}

function FinishEntryRow({
  abnormalTypeOptions,
  entry,
  onActualWeightChange,
  onFieldExhausted,
}: {
  abnormalTypeOptions: Array<{ label: string; value: number | string }>
  entry: WorkbenchFinish
  onActualWeightChange: (value: number | null) => void
  onFieldExhausted: () => void
}) {
  const finish = entry.finish

  return (
    <div className="back-record-finish-row">
      <div className="back-record-finish-row__identity">
        <Typography.Text strong>{finish.finishRollNo || '-'}</Typography.Text>
        <span>{finishSpec(finish)}</span>
        <div>
          <Tag color={finish.isSpare === 1 ? 'orange' : 'blue'}>{finish.isSpare === 1 ? '备用' : '正式'}</Tag>
          {entry.bindMode === 'inferred' && <Tag color="warning">辅助匹配</Tag>}
          {entry.bindMode === 'pool' && <Tag color="warning">待核对</Tag>}
        </div>
      </div>
      <div className="back-record-finish-row__fields">
        <Form.Item
          name={['finishes', finish.uuid, 'actualWeight']}
          label="实际重量"
          rules={finish.isSpare === 1 ? undefined : [
            { required: true, message: '必填' },
            { type: 'number', min: 0.001, message: '需大于0' },
          ]}
        >
          <InputNumber data-back-record-field="true" min={0} placeholder={finish.isSpare === 1 ? '未用留空' : 'kg'} addonAfter="kg" onChange={onActualWeightChange} onPressEnter={(event) => focusNextBackRecordField(event, onFieldExhausted)} />
        </Form.Item>
        <Form.Item name={['finishes', finish.uuid, 'scrapWeight']} label="报废/损耗">
          <InputNumber data-back-record-field="true" min={0} placeholder="kg" addonAfter="kg" onPressEnter={(event) => focusNextBackRecordField(event, onFieldExhausted)} />
        </Form.Item>
        <Form.Item name={['finishes', finish.uuid, 'isRemain']} label="属性">
          <Select options={remainOptions} />
        </Form.Item>
        <Form.Item name={['finishes', finish.uuid, 'isAbnormal']} label="异常">
          <Select options={abnormalOptions} />
        </Form.Item>
        <Form.Item name={['finishes', finish.uuid, 'abnormalType']} label="异常类型">
          <Select allowClear options={abnormalTypeOptions} placeholder="请选择" />
        </Form.Item>
        <Form.Item name={['finishes', finish.uuid, 'actualRemark']} label="备注">
          <Input data-back-record-field="true" placeholder="车间说明" onPressEnter={(event) => focusNextBackRecordField(event, onFieldExhausted)} />
        </Form.Item>
      </div>
    </div>
  )
}

function finishSpec(finish: FinishRoll) {
  const width = formatMm(finish.finishWidth)
  const weight = finish.estimateWeight ? formatKg(finish.estimateWeight) : '无预估'
  return `${finish.paperName || '-'} / ${width} / ${weight}`
}

const remainOptions = Object.entries(IS_REMAIN).map(([value, label]) => ({ value: Number(value), label }))
const abnormalOptions = [
  { value: 0, label: '否' },
  { value: 1, label: '是' },
]
