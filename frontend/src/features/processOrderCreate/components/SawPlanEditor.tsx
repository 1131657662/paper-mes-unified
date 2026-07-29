import { Alert, Button, Progress, Segmented, Space, Tag, Typography } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { DEFAULT_WIDTH_DIFFERENCE_POLICY, WIDTH_DIFFERENCE_POLICY_OPTIONS } from '../../../constants/processOrder'
import type { FinishConfigSpecDTO, ProcessPlanDTO, WidthDifferencePolicy } from '../../../types/processOrder'
import { formatMm } from '../../../utils/numberFormatters'
import type { RollDraft } from '../types'
import { calcSawPlanStats, normalizeSawSpecs } from '../sawPlanUtils'
import SawSpecificationTable from '../../../components/processOrder/SawSpecificationTable'
import './CreateOrderEditors.css'

interface Props {
  plan: ProcessPlanDTO
  roll: RollDraft
  onChange: (plan: ProcessPlanDTO) => void
}

export default function SawPlanEditor({ plan, roll, onChange }: Props) {
  const policy = plan.widthDifferencePolicy ?? DEFAULT_WIDTH_DIFFERENCE_POLICY
  const specs = normalizeSawSpecs(plan.finishSpecs ?? [])
  const stats = calcSawPlanStats(specs, roll.originalWidth ?? 0)

  const updateSpecs = (next: FinishConfigSpecDTO[]) => {
    const nextStats = calcSawPlanStats(next, roll.originalWidth ?? 0)
    onChange({ ...plan, finishSpecs: next, knifeCount: nextStats.knifeCount })
  }

  const changePolicy = (value: string | number) => {
    if (!isWidthDifferencePolicy(value)) return
    onChange({ ...plan, widthDifferencePolicy: value, finishSpecs: specs })
  }

  return (
    <Space className="create-editor-stack" direction="vertical" size={12}>
      <SawSummary originalWidth={roll.originalWidth ?? 0} stats={stats} policy={policy} />
      <Segmented aria-label="门幅差额处理" value={policy} options={WIDTH_DIFFERENCE_POLICY_OPTIONS} onChange={changePolicy} />
      <Space wrap>
        <Button icon={<PlusOutlined />} onClick={() => updateSpecs([...specs, newSpec('FINISH')])}>添加成品</Button>
        {policy === 'REMAINDER' && (
          <Button disabled={stats.remainingWidth <= 0}
            onClick={() => updateSpecs([...withoutTrim(specs), newSpec('TRIM', stats.remainingWidth)])}>
            剩余转余料
          </Button>
        )}
      </Space>
      {policy === 'REMAINDER' && stats.remainingWidth > 0 && (
        <Alert showIcon type="warning"
          message={`还有 ${formatMm(stats.remainingWidth)} 未分配`}
          description="留余料要求门幅完整闭合。可点击“剩余转余料”自动补齐，也可以手工调整余料门幅。" />
      )}
      <SawSpecificationTable specs={specs} onChange={updateSpecs} />
    </Space>
  )
}

function SawSummary({ originalWidth, stats, policy }: {
  originalWidth: number
  stats: ReturnType<typeof calcSawPlanStats>
  policy: WidthDifferencePolicy
}) {
  const overflow = stats.remainingWidth < 0
  return (
    <Space className="create-editor-stack" direction="vertical" size={6}>
      <Space wrap>
        <Tag color="blue">刀数 {stats.knifeCount}</Tag>
        <Tag color="green">成品 {stats.finishCount} 件</Tag>
        <Tag color={stats.remainingWidth > 0 ? 'orange' : 'default'}>
          未分配 {formatMm(Math.max(0, stats.remainingWidth))} · {policyLabel(policy)}
        </Tag>
        {stats.trimWidth > 0 && <Tag color="gold">切边 {formatMm(stats.trimWidth)}</Tag>}
        <Typography.Text type={overflow ? 'danger' : 'secondary'}>
          已排 {stats.usedWidth}/{originalWidth || '-'} mm
          {overflow ? `，超出 ${formatMm(Math.abs(stats.remainingWidth))}` : ''}
        </Typography.Text>
      </Space>
      {originalWidth > 0 && <Progress percent={stats.usedPercent} size="small" status={overflow ? 'exception' : 'active'} />}
    </Space>
  )
}

function newSpec(itemType: 'FINISH' | 'TRIM', finishWidth = itemType === 'FINISH' ? 1000 : 50): FinishConfigSpecDTO {
  return { itemType, finishWidth: Math.max(1, finishWidth), count: 1 }
}

function withoutTrim(specs: FinishConfigSpecDTO[]) {
  return specs.filter((spec) => spec.itemType !== 'TRIM')
}

function policyLabel(policy: WidthDifferencePolicy) {
  if (policy === 'LOSS') return '非库存损耗'
  if (policy === 'ALLOCATE') return '均匀分摊'
  return '实体余料'
}

function isWidthDifferencePolicy(value: string | number): value is WidthDifferencePolicy {
  return value === 'LOSS' || value === 'ALLOCATE' || value === 'REMAINDER'
}
