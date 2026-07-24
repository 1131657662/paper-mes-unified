import { Button, Space, Tag, Typography } from 'antd'
import type { ProcessRoutePreviewVO } from '../../../types/processOrder'
import { formatKg } from '../../../utils/numberFormatters'
import type { RollDraft } from '../types'

interface Props {
  onOpen: () => void
  preview: ProcessRoutePreviewVO
  roll: RollDraft
}

export default function ConfigStepRoutePanel({ onOpen, preview, roll }: Props) {
  const finals = preview.outputs?.filter((item) => !item.consumedByNextStage) ?? []
  const finishWeight = finals.reduce((sum, item) => sum + Number(item.estimateWeight ?? 0), 0)
  return (
    <div className="route-configured-panel">
      <Tag color="blue">链式工艺</Tag>
      <Typography.Title level={5}>{roll.rollNo || roll.paperName || '母卷'} 已配置多序加工</Typography.Title>
      <Space wrap>
        <Tag>工序 {preview.stages?.length ?? 0} 道</Tag>
        <Tag color="green">最终成品 {finals.length} 件</Tag>
        <Tag color="cyan">预估 {formatKg(finishWeight)}</Tag>
      </Space>
      <Typography.Paragraph type="secondary">
        该母卷已进入链式路线模式，单道工艺编辑已锁定，避免覆盖多序产物关系。
      </Typography.Paragraph>
      <Button type="primary" onClick={onOpen}>进入链式工艺设计</Button>
    </div>
  )
}
