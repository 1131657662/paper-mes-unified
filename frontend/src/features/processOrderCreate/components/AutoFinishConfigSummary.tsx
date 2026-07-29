import { Descriptions, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { STEP_TYPE } from '../../../constants/processOrder'
import type { ProcessPlanDTO } from '../../../types/processOrder'
import type { RollDraft } from '../types'

export interface AutoFinishConfigItem {
  plan: ProcessPlanDTO
  roll: RollDraft
}

export default function AutoFinishConfigSummary({ items }: { items: AutoFinishConfigItem[] }) {
  return (
    <>
      <Descriptions size="small" column={2} bordered items={summaryItems(items.length)} />
      <Table
        size="small"
        rowKey={({ roll }) => roll.localId}
        pagination={false}
        columns={columns}
        dataSource={items}
        scroll={{ y: 360 }}
      />
    </>
  )
}

const columns: ColumnsType<AutoFinishConfigItem> = [
  { title: '母卷', width: 150, render: (_, item) => item.roll.rollNo || item.roll.paperName || '-' },
  { title: '主工艺', width: 100, render: (_, item) => STEP_TYPE[item.plan.mainStepType ?? 0] ?? '-' },
  { title: '成品规格', render: (_, item) => configSummary(item.plan) },
  { title: '备用号', width: 80, align: 'right', render: (_, item) => item.plan.spareCount ?? 0 },
]

function summaryItems(count: number) {
  return [
    { key: 'count', label: '待确认母卷', children: `${count} 卷` },
    { key: 'source', label: '保存内容', children: <Tag color="blue">当前工作台参数（含默认值）</Tag> },
  ]
}

function configSummary(plan: ProcessPlanDTO): string {
  const specs = plan.finishSpecs?.filter((item) => item.itemType !== 'TRIM') ?? []
  if (specs.length > 0) return specs.map((item) => `${item.finishWidth ?? '-'}mm x ${item.count}`).join('；')
  const items = plan.segments?.flatMap((segment) => segment.layoutItems ?? []) ?? []
  return items.map((item) => `${item.width}mm x ${item.quantity}`).join('；') || '无独立成品'
}
