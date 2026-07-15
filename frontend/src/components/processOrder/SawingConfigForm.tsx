import { useState } from 'react'
import { Button, InputNumber, Space, Table, Typography, message } from 'antd'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import type { FinishConfigSaveDTO, FinishConfigSpecDTO, OriginalRoll } from '../../types/processOrder'

interface Props {
  roll: OriginalRoll
  processMode: number
  config?: FinishConfigSaveDTO
  onChange: (config: FinishConfigSaveDTO) => void
}

interface FinishSpec {
  key: string
  finishWidth: number
  count: number
  estimateWeight: number
}

const toDtoSpecs = (specs: FinishSpec[]): FinishConfigSpecDTO[] =>
  specs.map((spec) => ({
    finishWidth: spec.finishWidth,
    count: spec.count,
    estimateWeight: spec.estimateWeight,
  }))

export default function SawingConfigForm({ roll, processMode, config, onChange }: Props) {
  const [finishSpecs, setFinishSpecs] = useState<FinishSpec[]>(initialFinishSpecs(config))
  const [knifeCount, setKnifeCount] = useState(config?.knifeCount ?? 0)
  const [unitPrice, setUnitPrice] = useState<number | undefined>(config?.unitPrice)
  const [spareCount, setSpareCount] = useState(config?.spareCount ?? 0)

  const isStandardMode = processMode === 1
  const isOnSiteMode = processMode === 2

  const emitConfig = (next: Partial<FinishConfigSaveDTO>) => {
    onChange({
      processMode,
      mainStepType: 1,
      knifeCount,
      unitPrice,
      spareCount,
      finishSpecs: toDtoSpecs(finishSpecs),
      ...next,
    })
  }

  const calculateTrimWidth = () => {
    const totalFinishWidth = finishSpecs.reduce(
      (sum, spec) => sum + spec.finishWidth * spec.count,
      0,
    )
    const originalWidth = roll.originalWidth ?? 0
    return Math.max(0, originalWidth - totalFinishWidth)
  }

  const calculateTheoreticalKnife = () => {
    const totalCount = finishSpecs.reduce((sum, spec) => sum + spec.count, 0)
    return Math.max(0, totalCount - 1) + (calculateTrimWidth() > 0 ? 1 : 0)
  }

  const calculateEstimateWeights = () => {
    const originalWidth = roll.originalWidth ?? 0
    const totalWeight = (roll.rollWeight ?? 0) * (roll.pieceNum ?? 1)
    const trimWidth = calculateTrimWidth()
    const trimLossWeight = trimWidth > 0 ? (trimWidth / originalWidth) * totalWeight : 0
    const availableWeight = totalWeight - trimLossWeight
    const totalFinishWidth = finishSpecs.reduce(
      (sum, spec) => sum + spec.finishWidth * spec.count,
      0,
    )

    return finishSpecs.map((spec) => {
      if (totalFinishWidth === 0) return 0
      const ratio = spec.finishWidth / totalFinishWidth
      return Number((availableWeight * ratio).toFixed(3))
    })
  }

  const updateSpecs = (nextSpecs: FinishSpec[]) => {
    setFinishSpecs(nextSpecs)
    emitConfig({ finishSpecs: toDtoSpecs(nextSpecs) })
  }

  const handleAddSpec = () => {
    updateSpecs([...finishSpecs, { key: String(Date.now()), finishWidth: 400, count: 1, estimateWeight: 0 }])
  }

  const handleRemoveSpec = (key: string) => {
    if (finishSpecs.length <= 1) {
      message.warning('至少保留一个成品规格')
      return
    }
    updateSpecs(finishSpecs.filter((spec) => spec.key !== key))
  }

  const handleWidthChange = (key: string, value: number | null) => {
    updateSpecs(
      finishSpecs.map((spec) => (spec.key === key ? { ...spec, finishWidth: value ?? 0 } : spec)),
    )
  }

  const handleCountChange = (key: string, value: number | null) => {
    updateSpecs(
      finishSpecs.map((spec) => (spec.key === key ? { ...spec, count: value ?? 1 } : spec)),
    )
  }

  const handleCalculate = () => {
    const weights = calculateEstimateWeights()
    updateSpecs(finishSpecs.map((spec, index) => ({
      ...spec,
      estimateWeight: weights[index] ?? spec.estimateWeight,
    })))
    message.success('已重新计算预估重量')
  }

  const columns: ColumnsType<FinishSpec> = [
    {
      title: '成品门幅(mm)',
      dataIndex: 'finishWidth',
      width: 150,
      render: (value, record) => (
        <InputNumber
          aria-label={`成品规格 ${record.finishWidth} mm 门幅`}
          min={1}
          value={value}
          onChange={(val) => handleWidthChange(record.key, val)}
          style={{ width: '100%' }}
          disabled={isOnSiteMode}
        />
      ),
    },
    {
      title: '数量',
      dataIndex: 'count',
      width: 100,
      render: (value, record) => (
        <InputNumber
          aria-label={`成品规格 ${record.finishWidth} mm 数量`}
          min={1}
          value={value}
          onChange={(val) => handleCountChange(record.key, val)}
          style={{ width: '100%' }}
          disabled={isOnSiteMode}
        />
      ),
    },
    {
      title: '预估重量(kg)',
      dataIndex: 'estimateWeight',
      width: 150,
      render: (value) => <Typography.Text>{value || '-'}</Typography.Text>,
    },
    {
      title: '操作',
      width: 80,
      render: (_, record) => (
        <Button
          type="link"
          danger
          size="small"
          aria-label={`删除成品规格 ${record.finishWidth} mm`}
          icon={<DeleteOutlined />}
          onClick={() => handleRemoveSpec(record.key)}
          disabled={finishSpecs.length <= 1}
        />
      ),
    },
  ]

  const trimWidth = calculateTrimWidth()
  const theoreticalKnife = calculateTheoreticalKnife()
  const totalFinishCount = finishSpecs.reduce((sum, spec) => sum + spec.count, 0)

  return (
    <div>
      {isStandardMode && (
        <>
          <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
            标准加工模式：请配置成品规格，系统将自动计算预估重量并生成成品卷号
          </Typography.Text>

          <Table
            size="small"
            columns={columns}
            dataSource={finishSpecs}
            pagination={false}
            style={{ marginBottom: 12 }}
          />

          <Space style={{ marginBottom: 16 }}>
            <Button type="dashed" icon={<PlusOutlined />} onClick={handleAddSpec}>
              添加成品规格
            </Button>
            <Button onClick={handleCalculate}>重新计算预估重量</Button>
          </Space>

          <div style={{ background: '#fafafa', padding: 12, borderRadius: 4, marginBottom: 16 }}>
            <Space direction="vertical" style={{ width: '100%' }}>
              <div>
                <Typography.Text strong>总修边宽度：</Typography.Text>
                <Typography.Text>{trimWidth} mm</Typography.Text>
              </div>
              <div>
                <Typography.Text strong>理论刀数：</Typography.Text>
                <Typography.Text>{theoreticalKnife} 刀</Typography.Text>
                <Typography.Text type="secondary" style={{ marginLeft: 8 }}>
                  (成品数量 - 1)
                </Typography.Text>
              </div>
            </Space>
          </div>

          <Space direction="vertical" style={{ width: '100%', marginBottom: 16 }}>
            <div>
              <Typography.Text strong>实际刀数：</Typography.Text>
              <InputNumber
                aria-label="实际刀数"
                min={0}
                precision={0}
                value={knifeCount}
                onChange={(val) => {
                  const nextKnifeCount = val ?? 0
                  setKnifeCount(nextKnifeCount)
                  emitConfig({ knifeCount: nextKnifeCount })
                }}
                style={{ width: 150, marginLeft: 8 }}
                placeholder="含切头切边损耗"
              />
              <Typography.Text type="secondary" style={{ marginLeft: 8 }}>
                刀 (含切头切边损耗)
              </Typography.Text>
            </div>
            <div>
              <Typography.Text strong>锯纸单价：</Typography.Text>
              <InputNumber
                aria-label="锯纸单价"
                min={0}
                precision={2}
                value={unitPrice}
                onChange={(val) => {
                  const nextUnitPrice = val ?? 0
                  setUnitPrice(nextUnitPrice)
                  emitConfig({ unitPrice: nextUnitPrice })
                }}
                style={{ width: 150, marginLeft: 8 }}
                placeholder="元/刀"
              />
              <Typography.Text type="secondary" style={{ marginLeft: 8 }}>
                元/刀
              </Typography.Text>
            </div>
            <div>
              <Typography.Text strong>预估加工费：</Typography.Text>
              <Typography.Text mark style={{ marginLeft: 8 }}>
                {(knifeCount * (unitPrice ?? 0)).toFixed(2)} 元
              </Typography.Text>
            </div>
          </Space>
        </>
      )}

      {isOnSiteMode && (
        <>
          <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
            现场定尺模式：请输入预计成品件数，保存后由后端生成对应数量的正式成品号
          </Typography.Text>

          <div style={{ marginBottom: 16 }}>
            <Typography.Text strong>预计成品件数：</Typography.Text>
            <InputNumber
              aria-label="预计成品件数"
              min={1}
              value={totalFinishCount}
              onChange={(val) => {
                const count = val ?? 1
                updateSpecs([{ key: '1', finishWidth: 0, count, estimateWeight: 0 }])
              }}
              style={{ width: 150, marginLeft: 8 }}
            />
            <Typography.Text type="secondary" style={{ marginLeft: 8 }}>
              件
            </Typography.Text>
          </div>

          <Typography.Paragraph type="secondary" style={{ fontSize: 12 }}>
            说明：现场定尺模式下，成品规格在车间现场确定，保存后生成正式成品号，便于车间抄写。
          </Typography.Paragraph>
        </>
      )}

      <div style={{ background: '#f0f7ff', padding: 12, borderRadius: 4, marginBottom: 16 }}>
        <Typography.Text strong>保存后生成正式成品号：</Typography.Text>
        <div style={{ marginTop: 8 }}>
          <Typography.Text code>{totalFinishCount} 个</Typography.Text>
        </div>
      </div>

      <Space>
        <Typography.Text>备用卷号数量：</Typography.Text>
        <InputNumber
          aria-label="备用卷号数量"
          min={0}
          max={10}
          value={spareCount}
          onChange={(val) => {
            const nextSpareCount = val ?? 0
            setSpareCount(nextSpareCount)
            emitConfig({ spareCount: nextSpareCount })
          }}
          style={{ width: 100 }}
        />
        <Typography.Text type="secondary">个 (用于现场异常备用)</Typography.Text>
      </Space>
      {spareCount > 0 && (
        <div style={{ marginTop: 8 }}>
          <Typography.Text type="secondary">保存后生成备用号：{spareCount} 个</Typography.Text>
        </div>
      )}
    </div>
  )
}

function initialFinishSpecs(config?: FinishConfigSaveDTO): FinishSpec[] {
  const specs = config?.finishSpecs?.length ? config.finishSpecs : undefined
  if (!specs) {
    return [{ key: '1', finishWidth: 400, count: 1, estimateWeight: 0 }]
  }
  return specs.map((spec, index) => ({
    key: String(index + 1),
    finishWidth: spec.finishWidth ?? 400,
    count: spec.count ?? 1,
    estimateWeight: spec.estimateWeight ?? 0,
  }))
}
