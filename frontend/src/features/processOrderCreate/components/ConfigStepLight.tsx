import { Card, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { ReactNode } from 'react'
import { formatGram, formatMm, formatOptionalKg } from '../../../utils/numberFormatters'
import { mergedSourceLocks } from '../rewindConsumptionUtils'
import { configStepProgress } from '../configStepProgress'
import type { RollDraft } from '../types'
import { isRollWeightKnown, rollTotalWeight } from '../../processOrderDetail/routeConfigSource'
import ConfigStepFooter from './ConfigStepFooter'

interface Props {
  assistantEntry?: ReactNode
  lockedRolls: ReturnType<typeof mergedSourceLocks>
  onNext: () => void
  onPrev: () => void
  rolls: RollDraft[]
}

export default function ConfigStepLight({ assistantEntry, lockedRolls, onNext, onPrev, rolls }: Props) {
  const progress = configStepProgress({
    configuredPlanIds: [],
    lockedRolls,
    previews: {},
    routePreviews: {},
    rolls,
    serviceConfigured: {},
  })
  return (
    <Card title="无需单独配置的母卷" extra={assistantEntry} className="config-light-step">
      <Table
        size="small"
        rowKey="localId"
        pagination={false}
        columns={lightColumns(lockedRolls)}
        dataSource={rolls}
        scroll={{ x: 760 }}
      />
      <ConfigStepFooter hasUnsavedServiceChanges={false} onNext={onNext}
        onPrev={onPrev} progress={progress} saving={false} serviceWritePending={false} />
    </Card>
  )
}

function lightColumns(lockedRolls: ReturnType<typeof mergedSourceLocks>): ColumnsType<RollDraft> {
  return [
    { title: '母卷', width: 170, render: (_, roll) => rollNoText(roll) },
    { title: '品名', dataIndex: 'paperName', width: 130 },
    { title: '规格', width: 150, render: (_, roll) => `${formatGram(roll.gramWeight)} / ${formatMm(roll.originalWidth)}` },
    { title: '重量', width: 120, align: 'right', render: (_, roll) => !isRollWeightKnown(roll) ? '待称重' : formatOptionalKg(rollTotalWeight(roll)) },
    { title: '处理方式', width: 140, render: (_, roll) => lightRollStatus(roll, lockedRolls) },
    { title: '说明', width: 220, render: (_, roll) => lightRollHint(roll, lockedRolls) },
  ]
}

function lightRollStatus(roll: RollDraft, lockedRolls: ReturnType<typeof mergedSourceLocks>) {
  if (lockedRolls[roll.localId]) return <Tag color="blue">合并来源</Tag>
  if (roll.processMode === 3) return <Tag color="green">直发</Tag>
  return <Tag>无需配置</Tag>
}

function lightRollHint(roll: RollDraft, lockedRolls: ReturnType<typeof mergedSourceLocks>) {
  const lock = lockedRolls[roll.localId]
  if (lock) return `已由 ${lock.ownerLabel} 合并配置`
  if (roll.processMode === 3) return '回录阶段沿用母卷信息生成直发成品'
  return '-'
}

function rollNoText(roll: RollDraft) {
  return [roll.rollNo, roll.extraNo].filter(Boolean).join(' / ') || roll.paperName || '未编号母卷'
}
