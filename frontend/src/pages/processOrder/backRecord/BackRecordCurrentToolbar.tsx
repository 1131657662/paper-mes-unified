import { ArrowLeftOutlined, ArrowRightOutlined, CopyOutlined } from '@ant-design/icons'
import { Button, Form, Space, Tag, Typography } from 'antd'
import type { BackRecordFormValues } from './backRecordUtils'
import { theoreticalItemFinishValues } from './backRecordTheoryFill'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

interface Props {
  item: BackRecordWorkItem
  onNext: () => void
  onPrevious: () => void
}

export default function BackRecordCurrentToolbar({ item, onNext, onPrevious }: Props) {
  const form = Form.useFormInstance<BackRecordFormValues>()
  const canFillFinishes = item.roll?.processMode !== 2 && item.finishes.length > 0

  return (
    <div className="back-record-active__head">
      <div className="back-record-active__title">
        <Typography.Title level={5}>{item.title}</Typography.Title>
        {item.subtitle && <Typography.Text type="secondary">{item.subtitle}</Typography.Text>}
      </div>
      <Space wrap size={8} className="back-record-active__toolbar-actions">
        {item.roll && (
          <Button size="small" icon={<CopyOutlined />} onClick={() => fillRoll(form, item)}>
            带入标称
          </Button>
        )}
        {canFillFinishes && (
          <Button size="small" icon={<CopyOutlined />} onClick={() => fillFinishes(form, item)}>
            带入预估
          </Button>
        )}
        {item.isMergeGroup && <Tag color="geekblue">多母卷</Tag>}
        <Tag color={item.sourceMode === 'linked' ? 'success' : 'warning'}>{sourceLabel(item.sourceMode)}</Tag>
        <Button size="small" icon={<ArrowLeftOutlined />} onClick={onPrevious}>上一项</Button>
        <Button size="small" icon={<ArrowRightOutlined />} onClick={onNext}>下一项</Button>
      </Space>
    </div>
  )
}

function fillRoll(form: ReturnType<typeof Form.useForm<BackRecordFormValues>>[0], item: BackRecordWorkItem) {
  const roll = item.roll
  if (!roll) return
  form.setFieldValue(['rolls', roll.uuid], {
    actualGramWeight: roll.actualGramWeight ?? roll.gramWeight,
    actualWidth: roll.actualWidth ?? roll.originalWidth,
    actualWeight: roll.actualWeight ?? (roll.rollWeight ?? 0) * (roll.pieceNum ?? 1),
    remark: roll.remark,
  })
}

function fillFinishes(form: ReturnType<typeof Form.useForm<BackRecordFormValues>>[0], item: BackRecordWorkItem) {
  for (const [uuid, value] of Object.entries(theoreticalItemFinishValues(item))) {
    form.setFieldValue(['finishes', uuid], value)
  }
}

function sourceLabel(mode: BackRecordWorkItem['sourceMode']) {
  if (mode === 'linked') return '真实来源'
  if (mode === 'inferred') return '辅助匹配'
  if (mode === 'pool') return '待核对'
  return '无成品'
}
