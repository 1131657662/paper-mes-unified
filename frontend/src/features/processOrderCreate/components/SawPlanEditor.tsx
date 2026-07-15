import { Button, InputNumber, Progress, Select, Space, Table, Tag, Typography } from 'antd'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import MesTooltip from '../../../components/biz/MesTooltip'
import type { FinishConfigSpecDTO, ProcessPlanDTO } from '../../../types/processOrder'
import { formatMm } from '../../../utils/numberFormatters'
import type { RollDraft } from '../types'
import { calcSawPlanStats, isTrimSpec, normalizeSawSpecs } from '../sawPlanUtils'
import './CreateOrderEditors.css'

interface Props {
  plan: ProcessPlanDTO
  roll: RollDraft
  onChange: (plan: ProcessPlanDTO) => void
}

const itemTypeOptions = [
  { label: '成品', value: 'FINISH' },
  { label: '切边', value: 'TRIM' },
]

export default function SawPlanEditor({ plan, roll, onChange }: Props) {
  const specs = normalizeSawSpecs(plan.finishSpecs ?? [])
  const stats = calcSawPlanStats(specs, roll.originalWidth ?? 0)
  const updateSpecs = (next: FinishConfigSpecDTO[]) => {
    const nextStats = calcSawPlanStats(next, roll.originalWidth ?? 0)
    onChange({ ...plan, finishSpecs: next, knifeCount: nextStats.knifeCount })
  }

  return (
    <Space className="create-editor-stack" direction="vertical" size={12}>
      <SawSummary originalWidth={roll.originalWidth ?? 0} stats={stats} />
      <Space wrap>
        <Button icon={<PlusOutlined />} onClick={() => updateSpecs([...specs, newSpec('FINISH')])}>
          添加成品
        </Button>
        <Button icon={<PlusOutlined />} onClick={() => updateSpecs([...specs, newSpec('TRIM')])}>
          添加切边
        </Button>
        <Button disabled={stats.remainingWidth <= 0} onClick={() => updateSpecs([...specs, newSpec('TRIM', stats.remainingWidth)])}>
          剩余转切边
        </Button>
      </Space>
      <Table
        size="small"
        rowKey={(_, index) => String(index)}
        pagination={false}
        columns={columns(specs, updateSpecs)}
        dataSource={specs}
        scroll={{ x: 760 }}
      />
    </Space>
  )
}

function SawSummary({ originalWidth, stats }: { originalWidth: number; stats: ReturnType<typeof calcSawPlanStats> }) {
  const overflow = stats.remainingWidth < 0
  return (
    <Space className="create-editor-stack" direction="vertical" size={6}>
      <Space wrap>
        <Tag color="blue">自动刀数：{stats.knifeCount}</Tag>
        <Tag color="green">成品：{stats.finishCount} 件</Tag>
        <Tag color={stats.trimWidth > 0 ? 'orange' : 'default'}>切边：{formatMm(stats.trimWidth)}</Tag>
        {stats.implicitTrimWidth > 0 && <Tag color="gold">可转切边：{formatMm(stats.implicitTrimWidth)}</Tag>}
        <Typography.Text type={overflow ? 'danger' : 'secondary'}>
          门幅 {stats.usedWidth}/{originalWidth || '-'} mm{overflow ? `，超出 ${formatMm(Math.abs(stats.remainingWidth))}` : `，剩余 ${formatMm(stats.remainingWidth)}`}
        </Typography.Text>
      </Space>
      {originalWidth > 0 && <Progress percent={stats.usedPercent} size="small" status={overflow ? 'exception' : 'active'} />}
    </Space>
  )
}

function columns(
  specs: FinishConfigSpecDTO[],
  updateSpecs: (specs: FinishConfigSpecDTO[]) => void,
): ColumnsType<FinishConfigSpecDTO> {
  return [
    {
      title: '类型',
      width: 96,
      render: (_, spec, index) => (
        <Select
          aria-label={`规格 ${index + 1} 类型`}
          value={spec.itemType ?? 'FINISH'}
          options={itemTypeOptions}
          className="create-editor-kind-select--compact"
          onChange={(value) => updateSpecs(patchSpec(specs, index, typePatch(spec, value)))}
        />
      ),
    },
    {
      title: '门幅',
      dataIndex: 'finishWidth',
      width: 130,
      render: (_, spec, index) => (
        <InputNumber aria-label={`规格 ${index + 1} 门幅`} min={1} suffix="mm" value={spec.finishWidth} onChange={(value) => updateSpecs(patchSpec(specs, index, { finishWidth: value ?? 1 }))} />
      ),
    },
    {
      title: '直径',
      dataIndex: 'finishDiameter',
      width: 120,
      render: (_, spec, index) => (
        <InputNumber aria-label={`规格 ${index + 1} 直径`} disabled={isTrimSpec(spec)} min={0} value={spec.finishDiameter} onChange={(value) => updateSpecs(patchSpec(specs, index, { finishDiameter: value ?? undefined }))} />
      ),
    },
    {
      title: '纸芯',
      dataIndex: 'finishCoreDiameter',
      width: 120,
      render: (_, spec, index) => (
        <InputNumber aria-label={`规格 ${index + 1} 纸芯`} disabled={isTrimSpec(spec)} min={0} value={spec.finishCoreDiameter} onChange={(value) => updateSpecs(patchSpec(specs, index, { finishCoreDiameter: value ?? undefined }))} />
      ),
    },
    {
      title: '数量',
      dataIndex: 'count',
      width: 110,
      render: (_, spec, index) => (
        <InputNumber aria-label={`规格 ${index + 1} 数量`} min={1} value={spec.count} onChange={(value) => updateSpecs(patchSpec(specs, index, { count: value ?? 1 }))} />
      ),
    },
    {
      title: '操作',
      width: 72,
      render: (_, __, index) => (
        <MesTooltip title="删除规格">
          <Button
            danger
            aria-label="删除锯纸规格"
            size="small"
            icon={<DeleteOutlined />}
            onClick={() => updateSpecs(specs.filter((_, itemIndex) => itemIndex !== index))}
          />
        </MesTooltip>
      ),
    },
  ]
}

function newSpec(itemType: 'FINISH' | 'TRIM', finishWidth = itemType === 'FINISH' ? 1000 : 50): FinishConfigSpecDTO {
  return { itemType, finishWidth: Math.max(1, finishWidth), count: 1 }
}

function typePatch(spec: FinishConfigSpecDTO, itemType: 'FINISH' | 'TRIM'): Partial<FinishConfigSpecDTO> {
  return itemType === 'TRIM' ? { itemType, finishDiameter: undefined, finishCoreDiameter: undefined } : { ...spec, itemType }
}

function patchSpec(specs: FinishConfigSpecDTO[], index: number, patch: Partial<FinishConfigSpecDTO>) {
  return specs.map((spec, itemIndex) => (itemIndex === index ? { ...spec, ...patch } : spec))
}
