import { useEffect, useRef, useState } from 'react'
import { Alert, Button, Form, message, Space, Tag, Typography } from 'antd'
import { EditOutlined } from '@ant-design/icons'
import { DICT_TYPES, abnormalFallbackOptions } from '../../../features/systemConfig/configFallbacks'
import { useDictOptions } from '../../../features/systemConfig/hooks/useRuntimeDictOptions'
import type { BackRecordFormValues } from './backRecordUtils'
import { formatKg, formatMm } from '../../../utils/numberFormatters'
import type { BackRecordWorkItem, WorkbenchFinish } from './backRecordWorkbenchTypes'
import { autoTrimWeights } from './backRecordAutoTrim'
import BackRecordBatchSpecModal, { type BatchSpecValues } from './BackRecordBatchSpecModal'
import BackRecordFinishFields, { type BackRecordSourceOption } from './BackRecordFinishFields'

interface Props {
  item: BackRecordWorkItem
  onFieldExhausted: () => void
  sourceOptions: BackRecordSourceOption[]
}

export default function BackRecordFinishEntryList({ item, onFieldExhausted, sourceOptions }: Props) {
  const form = Form.useFormInstance<BackRecordFormValues>()
  const finishes = Form.useWatch('finishes', form)
  const rolls = Form.useWatch('rolls', form)
  const steps = Form.useWatch('steps', form)
  const autoTrimUuids = useRef(new Set<string>())
  const manualTrimUuids = useRef(new Set<string>())
  const [batchOpen, setBatchOpen] = useState(false)
  const { options: abnormalTypeOptions } = useDictOptions(DICT_TYPES.abnormalType, abnormalFallbackOptions)
  const onSite = item.roll?.processMode === 2

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

  const applyBatchSpecs = (values: BatchSpecValues) => {
    const targets = item.finishes.filter(({ finish }) => finish.isSpare !== 1 && finish.isRemain !== 1 && finish.rollNoStatus !== 3)
    let applied = 0
    for (const { finish } of targets) {
      const path: ['finishes', string] = ['finishes', finish.uuid]
      const current = form.getFieldValue(path)
      if (values.target === 'missing' && current?.finishWidth && current.finishWidth > 0) continue
      form.setFieldValue(path, { ...current, finishWidth: values.finishWidth, finishDiameter: values.finishDiameter ?? current?.finishDiameter, finishCoreDiameter: values.finishCoreDiameter ?? current?.finishCoreDiameter })
      applied += 1
    }
    setBatchOpen(false)
    message.success(`已填写 ${applied} 件正式成品的现场规格`)
  }

  return (
    <section className="back-record-panel">
      <div className="back-record-panel__head">
        <Typography.Text strong>成品 / 已配置余料</Typography.Text>
        <Space wrap size={8}>
          {onSite && <Button size="small" icon={<EditOutlined />} onClick={() => setBatchOpen(true)}>批量填写门幅</Button>}
        </Space>
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
              maxWidth={item.roll?.actualWidth ?? item.roll?.originalWidth}
              onSite={onSite}
              sourceOptions={sourceOptions}
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
      <BackRecordBatchSpecModal maxWidth={item.roll?.actualWidth ?? item.roll?.originalWidth} open={batchOpen} onCancel={() => setBatchOpen(false)} onApply={applyBatchSpecs} />
    </section>
  )
}

function FinishEntryRow({
  abnormalTypeOptions,
  entry,
  maxWidth,
  onSite,
  sourceOptions,
  onActualWeightChange,
  onFieldExhausted,
}: {
  abnormalTypeOptions: Array<{ label: string; value: number | string }>
  entry: WorkbenchFinish
  maxWidth?: number
  onSite: boolean
  sourceOptions: BackRecordSourceOption[]
  onActualWeightChange: (value: number | null) => void
  onFieldExhausted: () => void
}) {
  const finish = entry.finish

  return (
    <div className="back-record-finish-row">
      <div className="back-record-finish-row__identity">
        <Typography.Text strong>{finish.finishRollNo || '-'}</Typography.Text>
        <span>{finishSpec(finish, onSite)}</span>
        <div>
          <Tag color={finish.isRemain === 1 ? 'orange' : finish.isSpare === 1 ? 'gold' : 'blue'}>
            {finish.isRemain === 1 ? '余料' : finish.isSpare === 1 ? '备用' : '正式'}
          </Tag>
          {entry.bindMode === 'inferred' && <Tag color="warning">辅助匹配</Tag>}
          {entry.bindMode === 'pool' && <Tag color="warning">待核对</Tag>}
        </div>
      </div>
      <BackRecordFinishFields
        abnormalTypeOptions={abnormalTypeOptions}
        context={{ maxWidth, needsSource: entry.bindMode === 'pool', onSite, sourceOptions }}
        finish={finish}
        onActualWeightChange={onActualWeightChange}
        onFieldExhausted={onFieldExhausted}
      />
    </div>
  )
}

function finishSpec(finish: WorkbenchFinish['finish'], onSite: boolean) {
  const width = onSite && !finish.finishWidth ? '待现场确认' : formatMm(finish.finishWidth)
  const weight = finish.estimateWeight ? formatKg(finish.estimateWeight) : '无预估'
  return `${finish.paperName || '-'} / ${width} / ${weight}`
}
