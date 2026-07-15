import { useState } from 'react'
import { PlusOutlined } from '@ant-design/icons'
import { Button, Empty, Form, message, Space, Typography } from 'antd'
import type { OnSiteOutputRecordValues } from './backRecordUtils'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'
import type { BackRecordSourceOption } from './BackRecordFinishFields'
import BackRecordOnSiteOutputRow from './BackRecordOnSiteOutputRow'
import BackRecordBatchSpecModal, { type BatchSpecValues } from './BackRecordBatchSpecModal'

interface Props {
  item: BackRecordWorkItem
  onFieldExhausted: () => void
  sourceOptions: BackRecordSourceOption[]
}

export default function BackRecordOnSiteOutputList({ item, onFieldExhausted, sourceOptions }: Props) {
  const form = Form.useFormInstance()
  const [batchOpen, setBatchOpen] = useState(false)
  const options = itemSourceOptions(item, sourceOptions)
  const applyBatch = (values: BatchSpecValues) => {
    const path = ['onSiteOutputs', item.key] as const
    const rows = (form.getFieldValue(path) ?? []) as OnSiteOutputRecordValues[]
    const next = rows.map((row) => row.outputType === 'FINISH' && shouldFill(row, values)
      ? { ...row, finishWidth: values.finishWidth, finishDiameter: values.finishDiameter ?? row.finishDiameter, finishCoreDiameter: values.finishCoreDiameter ?? row.finishCoreDiameter }
      : row)
    form.setFieldValue(path, next)
    setBatchOpen(false)
    message.success(`已填写 ${next.filter((row) => row.outputType === 'FINISH').length} 件成品规格`)
  }
  return (
    <>
      <section className="back-record-panel back-record-output-panel">
        <Form.List name={['onSiteOutputs', item.key]}>
          {(fields, { add, remove }) => (
            <>
              <OutputHeader
                addFinish={() => add(defaultOutput('FINISH', options))}
                addTrim={() => add(defaultOutput('TRIM', options))}
                openBatch={() => setBatchOpen(true)}
              />
              {fields.length === 0 ? (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="尚未录入实际产出" />
              ) : (
                <div className="back-record-output-list">
                  {fields.map((field, index) => (
                    <BackRecordOnSiteOutputRow
                      key={field.key}
                      fieldName={field.name}
                      index={index}
                      itemKey={item.key}
                      onFieldExhausted={onFieldExhausted}
                      onRemove={() => remove(field.name)}
                      options={options}
                    />
                  ))}
                </div>
              )}
            </>
          )}
        </Form.List>
      </section>
      <BackRecordBatchSpecModal maxWidth={item.roll?.actualWidth ?? item.roll?.originalWidth} open={batchOpen} onCancel={() => setBatchOpen(false)} onApply={applyBatch} />
    </>
  )
}

function OutputHeader({ addFinish, addTrim, openBatch }: { addFinish: () => void; addTrim: () => void; openBatch: () => void }) {
  return (
    <div className="back-record-panel__head">
      <div>
        <Typography.Text strong>现场实际产出</Typography.Text>
        <Typography.Text type="secondary" className="back-record-output-panel__hint">
          成品和切边均按车间实际数量逐件录入
        </Typography.Text>
      </div>
      <Space wrap size={8}>
        <Button size="small" onClick={openBatch}>批量规格</Button>
        <Button size="small" type="primary" icon={<PlusOutlined />} onClick={addFinish}>新增成品</Button>
        <Button size="small" icon={<PlusOutlined />} onClick={addTrim}>新增切边/余料</Button>
      </Space>
    </div>
  )
}

function shouldFill(row: OnSiteOutputRecordValues, values: BatchSpecValues) {
  return values.target === 'all' || !row.finishWidth || row.finishWidth <= 0
}

function itemSourceOptions(item: BackRecordWorkItem, options: BackRecordSourceOption[]) {
  const sourceUuids = new Set(item.rollProductions.map((production) => production.originalUuid))
  if (item.roll) sourceUuids.add(item.roll.uuid)
  return options.filter((option) => option.processMode === 2 && sourceUuids.has(option.value))
}

function defaultOutput(outputType: 'FINISH' | 'TRIM', options: BackRecordSourceOption[]) {
  return { outputType, originalUuid: options.length === 1 ? options[0]?.value : undefined }
}
