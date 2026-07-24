import { Button, InputNumber, Segmented, Space, Tag, Typography } from 'antd'
import { PlusOutlined } from '@ant-design/icons'
import { DEFAULT_WIDTH_DIFFERENCE_POLICY, WIDTH_DIFFERENCE_POLICY_OPTIONS } from '../../constants/processOrder'
import type { FinishConfigSaveDTO, FinishConfigSpecDTO, OriginalRoll, WidthDifferencePolicy } from '../../types/processOrder'
import SawSpecificationTable from './SawSpecificationTable'

interface Props {
  roll: OriginalRoll
  processMode: number
  config?: FinishConfigSaveDTO
  onChange: (config: FinishConfigSaveDTO) => void
}

export default function SawingConfigForm({ roll, processMode, config, onChange }: Props) {
  const policy = config?.widthDifferencePolicy ?? DEFAULT_WIDTH_DIFFERENCE_POLICY
  const specs = normalizedSpecs(config, processMode)
  const difference = widthDifference(specs, roll.originalWidth ?? 0)
  const hasRemainder = specs.some((spec) => spec.itemType === 'TRIM')
  const count = specs.filter(isFinish).reduce((sum, spec) => sum + spec.count, 0)
  const knifeCount = config?.knifeCount ?? derivedKnives(count, difference)

  const emit = (patch: Partial<FinishConfigSaveDTO>) => onChange({
    processMode,
    mainStepType: 1,
    spareCount: config?.spareCount ?? 0,
    unitPrice: config?.unitPrice,
    knifeCount,
    widthDifferencePolicy: policy,
    finishSpecs: specs,
    ...patch,
  })

  const updateSpecs = (next: FinishConfigSpecDTO[]) => {
    const compatible = policy === 'REMAINDER' ? next : next.filter(isFinish)
    emit({ finishSpecs: compatible, knifeCount: derivedKnives(
      compatible.filter(isFinish).reduce((sum, spec) => sum + spec.count, 0),
      widthDifference(compatible, roll.originalWidth ?? 0),
    ) })
  }

  const changePolicy = (value: string | number) => {
    if (!isPolicy(value)) return
    emit({ widthDifferencePolicy: value, finishSpecs: value === 'REMAINDER' ? specs : specs.filter(isFinish) })
  }

  if (processMode === 2) {
    return <OnSiteSawConfig count={count} config={config} emit={emit} />
  }

  return (
    <Space direction="vertical" size={12} style={{ width: '100%' }}>
      <Space wrap>
        <Segmented aria-label="门幅差额处理" value={policy} options={WIDTH_DIFFERENCE_POLICY_OPTIONS} onChange={changePolicy} />
        <Tag color="blue">刀数 {knifeCount}</Tag>
        <Tag color={difference > 0 ? 'orange' : 'default'}>门幅差额 {Math.max(0, difference)} mm</Tag>
      </Space>
      <SawSpecificationTable specs={specs} onChange={updateSpecs} />
      <Space wrap>
        <Button icon={<PlusOutlined />} onClick={() => updateSpecs([...specs, newSpec('FINISH')])}>添加成品</Button>
        {policy === 'REMAINDER' && difference > 0 && !hasRemainder && (
          <Button onClick={() => updateSpecs([...specs.filter(isFinish), newSpec('TRIM', difference)])}>剩余转余料</Button>
        )}
        <NumberField label="实际刀数" value={knifeCount} onChange={(value) => emit({ knifeCount: value })} />
        <NumberField label="锯纸单价" value={config?.unitPrice} precision={2} onChange={(value) => emit({ unitPrice: value })} />
        <NumberField label="备用卷号" value={config?.spareCount ?? 0} onChange={(value) => emit({ spareCount: value })} />
      </Space>
    </Space>
  )
}

function OnSiteSawConfig({ count, config, emit }: {
  count: number
  config?: FinishConfigSaveDTO
  emit: (patch: Partial<FinishConfigSaveDTO>) => void
}) {
  return <Space wrap>
    <NumberField label="预计成品件数" value={count || 1}
      onChange={(value) => emit({ finishSpecs: [{ itemType: 'FINISH', finishWidth: 0, count: Math.max(1, value) }] })} />
    <NumberField label="实际刀数" value={config?.knifeCount ?? 0} onChange={(value) => emit({ knifeCount: value })} />
    <NumberField label="锯纸单价" value={config?.unitPrice} precision={2} onChange={(value) => emit({ unitPrice: value })} />
  </Space>
}

function NumberField({ label, value, precision = 0, onChange }: {
  label: string
  value?: number
  precision?: number
  onChange: (value: number) => void
}) {
  return <Space size={6}>
    <Typography.Text>{label}</Typography.Text>
    <InputNumber aria-label={label} min={0} precision={precision} value={value}
      onChange={(next) => onChange(next ?? 0)} style={{ width: 120 }} />
  </Space>
}

function normalizedSpecs(config: FinishConfigSaveDTO | undefined, processMode: number) {
  const source = config?.finishSpecs?.length ? config.finishSpecs : [newSpec('FINISH', processMode === 2 ? 0 : 400)]
  return source.map((spec) => ({ ...spec, itemType: spec.itemType ?? 'FINISH', count: spec.count ?? 1 }))
}

function widthDifference(specs: FinishConfigSpecDTO[], sourceWidth: number) {
  const used = specs.filter(isFinish).reduce((sum, spec) => sum + (spec.finishWidth ?? 0) * spec.count, 0)
  return sourceWidth - used
}

function derivedKnives(count: number, difference: number) {
  return count <= 0 ? 0 : Math.max(0, count - 1) + (difference > 0 ? 1 : 0)
}

function newSpec(itemType: 'FINISH' | 'TRIM', width = 400): FinishConfigSpecDTO {
  return { itemType, finishWidth: width, count: 1, estimateWeight: 0 }
}

function isFinish(spec: FinishConfigSpecDTO) {
  return spec.itemType !== 'TRIM'
}

function isPolicy(value: string | number): value is WidthDifferencePolicy {
  return value === 'LOSS' || value === 'ALLOCATE' || value === 'REMAINDER'
}
