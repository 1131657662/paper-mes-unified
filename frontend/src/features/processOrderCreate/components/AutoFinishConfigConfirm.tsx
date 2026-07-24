import { Descriptions, Modal, Table, Tag } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import { STEP_TYPE } from '../../../constants/processOrder'
import type { ProcessPlanDTO } from '../../../types/processOrder'
import type { RollDraft } from '../types'

export interface AutoFinishConfigItem {
  plan: ProcessPlanDTO
  roll: RollDraft
}

export function confirmAutoFinishConfigs(items: AutoFinishConfigItem[]): Promise<boolean> {
  return new Promise((resolve) => {
    Modal.confirm({
      title: '确认自动生成的成品配置',
      content: <AutoFinishConfigSummary items={items} />,
      width: 760,
      okText: '确认无误并保存',
      cancelText: '返回修改',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}

function AutoFinishConfigSummary({ items }: { items: AutoFinishConfigItem[] }) {
  return (
    <>
      <Descriptions size="small" column={2} bordered items={[
        { key: 'count', label: '待确认母卷', children: `${items.length} 卷` },
        { key: 'source', label: '配置来源', children: <Tag color="blue">系统默认配置</Tag> },
      ]} />
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

function configSummary(plan: ProcessPlanDTO): string {
  const specs = plan.finishSpecs?.filter((item) => item.itemType !== 'TRIM') ?? []
  if (specs.length > 0) return specs.map((item) => `${item.finishWidth ?? '-'}mm x ${item.count}`).join('；')
  const items = plan.segments?.flatMap((segment) => segment.layoutItems ?? []) ?? []
  return items.map((item) => `${item.width}mm x ${item.quantity}`).join('；') || '无独立成品'
}
