import { useState } from 'react'
import { Button, Form, Input, List, Modal, Space, Switch, Tag, Typography, message } from 'antd'
import { EditOutlined, MinusCircleOutlined, PlusOutlined } from '@ant-design/icons'
import type { BackRecordFormValues } from './backRecordUtils'
import type { BackRecordFinishAdjustmentValues } from '../../../types/processOrder'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'
import {
  createAddedFinish,
  defaultFinishAdjustment,
  normalizeFinishAdjustment,
  nextAddedFinishIndex,
  officialFinishUuids,
} from './backRecordFinishAdjustment'

interface Props {
  item: BackRecordWorkItem
  onDirty?: () => void
}

export default function BackRecordFinishAdjustmentPanel({ item, onDirty }: Props) {
  const form = Form.useFormInstance<BackRecordFormValues>()
  const watched = Form.useWatch(['finishAdjustments', item.key], { form, preserve: true }) as BackRecordFinishAdjustmentValues | undefined
  const adjustment = normalizeFinishAdjustment(item, watched)
  const [open, setOpen] = useState(false)
  const [draft, setDraft] = useState<BackRecordFinishAdjustmentValues>(defaultFinishAdjustment(item))
  const planned = officialFinishUuids(item)
  const changed = adjustment.producedFinishUuids.length !== planned.length || adjustment.added.length > 0
  const notProducedCount = planned.length - adjustment.producedFinishUuids.length
  if (item.kind !== 'roll' || item.roll?.processMode !== 1) return null

  const openEditor = () => {
    setDraft({
      plannedFinishUuids: [...adjustment.plannedFinishUuids],
      producedFinishUuids: [...adjustment.producedFinishUuids],
      reason: adjustment.reason,
      added: adjustment.added.map((value) => ({ ...value })),
    })
    setOpen(true)
  }

  const apply = () => {
    const hasChange = draft.producedFinishUuids.length !== planned.length || draft.added.length > 0
    if (hasChange && !draft.reason.trim()) {
      message.warning('请填写产出数量调整原因')
      return
    }
    form.setFieldValue(['finishAdjustments', item.key], draft)
    const existing: NonNullable<BackRecordFormValues['finishes']> = form.getFieldValue('finishes') ?? {}
    const currentAddedKeys = new Set(draft.added.map(({ uuid }) => uuid))
    const removedAddedKeys = new Set(
      adjustment.added
        .map(({ uuid }) => uuid)
        .filter((uuid) => !currentAddedKeys.has(uuid)),
    )
    const retained = Object.entries(existing).reduce<NonNullable<BackRecordFormValues['finishes']>>(
      (result, [uuid, value]) => {
        if (!removedAddedKeys.has(uuid)) result[uuid] = value
        return result
      },
      {},
    )
    const addedValues = Object.fromEntries(draft.added.map((value) => [
      value.uuid,
      { ...(retained[value.uuid] ?? {}), originalUuid: value.originalUuid },
    ]))
    form.setFieldsValue({ finishes: { ...retained, ...addedValues } })
    onDirty?.()
    setOpen(false)
  }

  const setProduced = (uuid: string, produced: boolean) => {
    const next = new Set(draft.producedFinishUuids)
    if (produced) next.add(uuid)
    else next.delete(uuid)
    setDraft({ ...draft, producedFinishUuids: planned.filter((id) => next.has(id)) })
  }

  const addActual = () => setDraft({
    ...draft,
    added: [...draft.added, createAddedFinish(item, nextAddedFinishIndex(item.key, draft.added))],
  })

  const removeAdded = (index: number) => setDraft({ ...draft, added: draft.added.filter((_, i) => i !== index) })

  return (
    <>
      <div className="back-record-finish-adjustment">
        <div>
          <Typography.Text strong>实际产出</Typography.Text>
          <Typography.Text type="secondary">计划 {planned.length} 件 · 实际 {adjustment.producedFinishUuids.length + adjustment.added.length} 件</Typography.Text>
        </div>
        {changed && (
          <Space size={4}>
            <Tag color="warning">已调整</Tag>
            {notProducedCount > 0 && <Tag color="error">回录中已隐藏 {notProducedCount} 件</Tag>}
          </Space>
        )}
        <Button size="small" icon={<EditOutlined />} onClick={openEditor}>调整实际产出</Button>
      </div>
      <Modal title="调整实际产出" open={open} onCancel={() => setOpen(false)} onOk={apply} okText="应用调整" width={620} destroyOnHidden>
        <Typography.Paragraph type="secondary">现场实际数量与计划不一致时，在这里标记未产出或新增成品。原计划卷号会保留，便于追溯。</Typography.Paragraph>
        <List
          size="small"
          header={<Typography.Text strong>计划成品（{planned.length} 件）</Typography.Text>}
          dataSource={item.finishes.filter(({ finish }) => planned.includes(finish.uuid))}
          renderItem={({ finish }) => (
            <List.Item>
              <Space>
                <Switch aria-label={`${finish.finishRollNo || '计划成品'}是否实际产出`} checked={draft.producedFinishUuids.includes(finish.uuid)} onChange={(checked) => setProduced(finish.uuid, checked)} />
                <span>{finish.finishRollNo || '待生成卷号'}</span>
                <Typography.Text type="secondary">{finish.paperName || '-'} / {finish.finishWidth || '-'}mm</Typography.Text>
              </Space>
              {!draft.producedFinishUuids.includes(finish.uuid) && <Tag color="error">未产出</Tag>}
            </List.Item>
          )}
        />
        <div className="back-record-finish-adjustment__added-head">
          <Typography.Text strong>实际新增成品（{draft.added.length} 件）</Typography.Text>
          <Button type="link" size="small" icon={<PlusOutlined />} onClick={addActual}>新增一件</Button>
        </div>
        {draft.added.length > 0 && (
          <List
            size="small"
            dataSource={draft.added}
            renderItem={(_value, index) => (
              <List.Item actions={[<Button key="remove" aria-label={`移除新增成品 ${index + 1}`} type="text" danger icon={<MinusCircleOutlined />} onClick={() => removeAdded(index)} />]}>
                <Space>
                  <Tag color="blue">新增 {index + 1}</Tag>
                  <Typography.Text type="secondary">提交后自动生成正式卷号</Typography.Text>
                </Space>
              </List.Item>
            )}
          />
        )}
        <Form.Item label="调整原因" required>
          <Input.TextArea value={draft.reason} maxLength={255} showCount rows={2} placeholder="例如：现场实际切出2卷，另外2卷未产出" onChange={(event) => setDraft({ ...draft, reason: event.target.value })} />
        </Form.Item>
      </Modal>
    </>
  )
}
