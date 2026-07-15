import { useEffect, useRef, useState } from 'react'
import { Button, Col, Divider, InputNumber, Row, Select, Space, Tag, Typography, message } from 'antd'
import { DeleteOutlined, PlusOutlined } from '@ant-design/icons'
import { previewRewindPlan } from '../../api/processOrder'
import MesTooltip from '../biz/MesTooltip'
import TooltipText from '../biz/TooltipText'
import LayoutBar from './LayoutBar'
import FinishConfigPlanPreviewPanel from './FinishConfigPlanPreviewPanel'
import type {
  FinishConfigSaveDTO,
  FinishConfigSpecDTO,
  FinishPreviewVO,
  OriginalRoll,
  RewindLayoutItemDTO,
  RewindPlanPreviewDTO,
  RewindSegmentDTO,
} from '../../types/processOrder'
import { formatTon } from '../../utils/numberFormatters'

interface Props {
  orderUuid: string
  roll: OriginalRoll
  originalRolls: OriginalRoll[]
  processMode: number
  config?: FinishConfigSaveDTO
  onChange: (config: FinishConfigSaveDTO) => void
}

interface SegmentForm extends RewindSegmentDTO {
  key: string
  sources: SourceForm[]
  layoutItems: LayoutItemForm[]
}

interface SourceForm {
  originalUuid: string
  shareRatio: number
}

interface LayoutItemForm extends RewindLayoutItemDTO {
  key: string
  itemType: 'FINISH' | 'TRIM'
}

const REWIND_MODES = {
  1: '改门幅不变直径',
  2: '改直径不变门幅',
  3: '改门幅+改直径',
  4: '内外层分层',
  5: '多母卷合并复卷',
}

const newKey = () => String(Date.now() + Math.random())

const defaultLayoutItem = (width = 500): LayoutItemForm => ({
  key: newKey(),
  width,
  quantity: 1,
  itemType: 'FINISH',
})

const CORE_DIAMETER_OPTIONS = [3, 4, 6, 12]

const defaultSegment = (
  sort = 1,
  originalWidth = 1000,
  sourceUuid?: string,
  rewindMode = 1,
): SegmentForm => ({
  key: newKey(),
  segmentSort: sort,
  segmentRatio: 100,
  targetDiameter: undefined,
  finishCoreDiameter: 3,
  repeatCount: 1,
  sources: sourceUuid ? [{ originalUuid: sourceUuid, shareRatio: 100 }] : [],
  layoutItems: [defaultLayoutItem(rewindMode === 2 ? originalWidth : Math.floor(originalWidth / 2) || 500)],
})

const buildDefaultSegments = (originalWidth: number, sourceUuid: string, rewindMode: number) => [
  defaultSegment(1, originalWidth, sourceUuid, rewindMode),
]

const toCm = (inch?: number) => (inch != null ? Math.round(inch * 2.54) : undefined)
const toInch = (cm?: number) => (cm != null ? Math.round(cm / 2.54) : undefined)

const buildSegmentFromDto = (
  segment: RewindSegmentDTO,
  index: number,
  originalWidth: number,
  sourceUuid?: string,
  rewindMode = 1,
): SegmentForm => ({
  key: newKey(),
  segmentSort: segment.segmentSort ?? index + 1,
  segmentRatio: (segment.segmentRatio ?? 0) * 100,
  targetDiameter: toCm(segment.targetDiameter),
  finishCoreDiameter: segment.finishCoreDiameter ?? 3,
  repeatCount: segment.repeatCount ?? 1,
  sources: segment.sources?.length
    ? segment.sources.map((source) => ({ originalUuid: source.originalUuid ?? '', shareRatio: source.shareRatio ?? 0 }))
    : sourceUuid ? [{ originalUuid: sourceUuid, shareRatio: 100 }] : [],
  layoutItems: segment.layoutItems?.length
    ? segment.layoutItems.map((item) => ({
      key: newKey(),
      width: item.width,
      quantity: item.quantity ?? 1,
      itemType: item.itemType ?? 'FINISH',
      layers: item.layers?.map((layer) => ({ ...layer })),
    }))
    : [defaultLayoutItem(rewindMode === 2 ? originalWidth : Math.floor(originalWidth / 2) || 500)],
})

const buildInitialSegments = (roll: OriginalRoll, config?: FinishConfigSaveDTO) => {
  const originalWidth = roll.originalWidth ?? 1000
  const initialRewindMode = config?.rewindMode ?? 2
  if (config?.rewindSegments?.length) {
    return config.rewindSegments.map((segment, index) =>
      buildSegmentFromDto(segment, index, originalWidth, roll.uuid, initialRewindMode),
    )
  }
  return buildDefaultSegments(originalWidth, roll.uuid, initialRewindMode)
}

const toPreviewDto = (rewindMode: number, spareCount: number, segments: SegmentForm[]): RewindPlanPreviewDTO => ({
  rewindMode,
  spareCount,
  segments: segments.map(({ key: _key, layoutItems, sources, ...segment }) => ({
    ...segment,
    // 单分段时固定为 1.0，多分段时转换为小数（50% → 0.5）
    segmentRatio: segments.length === 1 ? 1 : (segment.segmentRatio ?? 0) / 100,
    targetDiameter: toInch(segment.targetDiameter),
    sources: sources.map(({ originalUuid, shareRatio }) => ({ originalUuid, shareRatio })),
    layoutItems: layoutItems.map(({ key: _itemKey, ...item }) => ({
      ...item,
      layers: rewindMode === 4 && item.itemType !== 'TRIM'
        ? item.layers?.length ? item.layers : [{ outDiameter: toInch(segment.targetDiameter), coreDiameter: segment.finishCoreDiameter }]
        : item.layers,
    })),
  })),
})

const toFinishSpecs = (preview: FinishPreviewVO | null, segments: SegmentForm[]): FinishConfigSpecDTO[] => {
  if (preview?.finishes?.length) {
    return preview.finishes.map((finish) => ({
      count: 1,
      finishWidth: finish.finishWidth,
      finishDiameter: finish.finishDiameter,
      finishCoreDiameter: finish.finishCoreDiameter,
      estimateWeight: finish.estimateWeight,
      layers: finish.layers,
    }))
  }

  return segments.flatMap((segment) => {
    const repeatCount = segment.repeatCount ?? 1
    return segment.layoutItems.flatMap((item) => {
      if (item.itemType !== 'FINISH') return []
      const quantity = item.quantity ?? 1
      return Array.from({ length: repeatCount * quantity }, () => ({
        count: 1,
        finishWidth: item.width,
        finishDiameter: toInch(segment.targetDiameter),
        finishCoreDiameter: segment.finishCoreDiameter,
        estimateWeight: 0,
      }))
    })
  })
}

export default function RewindingConfigForm({ orderUuid, roll, originalRolls, processMode, config, onChange }: Props) {
  const [rewindMode, setRewindMode] = useState(config?.rewindMode ?? 2)
  const [segments, setSegments] = useState<SegmentForm[]>(buildInitialSegments(roll, config))
  const [unitPrice, setUnitPrice] = useState<number | undefined>(config?.unitPrice)
  const [spareCount, setSpareCount] = useState(config?.spareCount ?? 0)
  const [preview, setPreview] = useState<FinishPreviewVO | null>(null)
  const [previewing, setPreviewing] = useState(false)
  const previewRequestRef = useRef(0)

  const isStandardMode = processMode === 1
  const isOnSiteMode = processMode === 2
  const totalFinishCount = preview?.finishCount ?? toFinishSpecs(null, segments).length
  const tonnage = formatTon(((roll.rollWeight ?? 0) * (roll.pieceNum ?? 1)) / 1000)
  const sourceRollOptions = originalRolls.map((sourceRoll, index) => ({
    label: `原纸${index + 1} ${sourceRoll.rollNo || sourceRoll.paperName || ''}`,
    value: sourceRoll.uuid,
  }))

  const buildConfig = ({
    nextPreview = preview,
    nextRewindMode = rewindMode,
    nextUnitPrice = unitPrice,
    nextSpareCount = spareCount,
    nextSegments = segments,
  } = {}): FinishConfigSaveDTO => ({
    processMode,
    mainStepType: 2,
    rewindMode: nextRewindMode,
    unitPrice: nextUnitPrice,
    spareCount: nextSpareCount,
    finishSpecs: toFinishSpecs(nextPreview, nextSegments),
    rewindSegments: toPreviewDto(nextRewindMode, nextSpareCount, nextSegments).segments,
  })

  const emitConfig = (options?: Parameters<typeof buildConfig>[0]) => {
    onChange(buildConfig(options))
  }

  const updateSegments = (nextSegments: SegmentForm[]) => {
    setSegments(nextSegments)
    emitConfig({ nextSegments })
  }

  useEffect(() => {
    if (!isStandardMode) return undefined
    const requestId = ++previewRequestRef.current
    setPreviewing(true)
    const timer = window.setTimeout(async () => {
      try {
        const result = await previewRewindPlan(
          orderUuid,
          roll.uuid,
          toPreviewDto(rewindMode, spareCount, segments),
        )
        if (previewRequestRef.current !== requestId) return
        setPreview(result)
        emitConfig({ nextPreview: result, nextSegments: segments })
      } catch {
        if (previewRequestRef.current === requestId) setPreview(null)
      } finally {
        if (previewRequestRef.current === requestId) setPreviewing(false)
      }
    }, 450)
    return () => window.clearTimeout(timer)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [orderUuid, roll.uuid, rewindMode, spareCount, segments, isStandardMode])

  const updateSegment = (key: string, patch: Partial<SegmentForm>) => {
    updateSegments(segments.map((segment) => (segment.key === key ? { ...segment, ...patch } : segment)))
  }

  const updateLayoutItem = (segmentKey: string, itemKey: string, patch: Partial<LayoutItemForm>) => {
    updateSegments(
      segments.map((segment) => {
        if (segment.key !== segmentKey) return segment
        return {
          ...segment,
          layoutItems: segment.layoutItems.map((item) => (item.key === itemKey ? { ...item, ...patch } : item)),
        }
      }),
    )
  }

  const updateSegmentSources = (segmentKey: string, sourceUuids: string[]) => {
    updateSegments(
      segments.map((segment) => {
        if (segment.key !== segmentKey) return segment
        const keptSources = segment.sources.filter((source) => sourceUuids.includes(source.originalUuid))
        const addedSources = sourceUuids
          .filter((sourceUuid) => !keptSources.some((source) => source.originalUuid === sourceUuid))
          .map((sourceUuid) => ({ originalUuid: sourceUuid, shareRatio: 0 }))
        return { ...segment, sources: [...keptSources, ...addedSources] }
      }),
    )
  }

  const updateSourceRatio = (segmentKey: string, originalUuid: string, shareRatio: number) => {
    updateSegments(
      segments.map((segment) => {
        if (segment.key !== segmentKey) return segment
        return {
          ...segment,
          sources: segment.sources.map((source) =>
            source.originalUuid === originalUuid ? { ...source, shareRatio } : source,
          ),
        }
      }),
    )
  }

  const addSegment = () => {
    updateSegments([...segments, defaultSegment(segments.length + 1, roll.originalWidth ?? 1000, roll.uuid, rewindMode)])
  }

  const removeSegment = (key: string) => {
    if (segments.length <= 1) {
      message.warning('至少保留一个分段')
      return
    }
    updateSegments(segments.filter((segment) => segment.key !== key))
  }

  const addLayoutItem = (segmentKey: string, itemType: 'FINISH' | 'TRIM') => {
    updateSegments(
      segments.map((segment) =>
        segment.key === segmentKey
          ? { ...segment, layoutItems: [...segment.layoutItems, { ...defaultLayoutItem(100), itemType }] }
          : segment,
      ),
    )
  }

  const removeLayoutItem = (segmentKey: string, itemKey: string) => {
    updateSegments(
      segments.map((segment) => {
        if (segment.key !== segmentKey) return segment
        if (segment.layoutItems.length <= 1) {
          message.warning('每个分段至少保留一个门幅项')
          return segment
        }
        return { ...segment, layoutItems: segment.layoutItems.filter((item) => item.key !== itemKey) }
      }),
    )
  }

  const previewSegments = toPreviewDto(rewindMode, spareCount, segments).segments ?? []

  if (isOnSiteMode) {
    return (
      <div>
        <Typography.Text type="secondary" style={{ display: 'block', marginBottom: 12 }}>
          现场定尺模式：请输入预计成品件数，保存后由后端生成对应数量的正式成品号
        </Typography.Text>
        <InputNumber
          aria-label="预计成品件数"
          min={1}
          value={totalFinishCount || 1}
          onChange={(value) => {
            const count = value ?? 1
            onChange({
              processMode,
              mainStepType: 2,
              rewindMode,
              unitPrice,
              spareCount,
              finishSpecs: [{ count, finishWidth: 0, finishDiameter: 0, finishCoreDiameter: 0, estimateWeight: 0 }],
            })
          }}
          addonBefore="预计成品件数"
          suffix="件"
        />
      </div>
    )
  }

  return (
    <div>
      {isStandardMode && (
        <Row gutter={16} align="top">
          <Col span={14}>
            <Space direction="vertical" size={16} style={{ width: '100%' }}>
              <Space wrap>
                <Typography.Text strong>复卷模式</Typography.Text>
                <Select
                  aria-label="复卷模式"
                  value={rewindMode}
                  onChange={(value) => {
                    const nextSegments = value === 2 ? buildDefaultSegments(roll.originalWidth ?? 1000, roll.uuid, value) : segments
                    setRewindMode(value)
                    setSegments(nextSegments)
                    emitConfig({ nextRewindMode: value, nextSegments })
                  }}
                  style={{ width: 220 }}
                  options={Object.entries(REWIND_MODES).map(([value, label]) => ({ value: Number(value), label }))}
                />
                <Button type="dashed" icon={<PlusOutlined />} onClick={addSegment}>
                  添加分段
                </Button>
                {previewing && <Tag color="processing">预览计算中…</Tag>}
              </Space>

              {segments.map((segment, index) => {
                const layoutWidth = segment.layoutItems.reduce((sum, item) => sum + item.width * (item.quantity ?? 1), 0)
                const widthDanger = (roll.originalWidth ?? 0) > 0 && layoutWidth > (roll.originalWidth ?? 0)
                return (
                  <div key={segment.key} style={{ border: '1px solid #f0f0f0', borderRadius: 6, padding: 12 }}>
                    <Space wrap style={{ marginBottom: 12 }}>
                      <Tag color="blue">分段 {index + 1}</Tag>
                      {segments.length > 1 ? (
                        <MesTooltip title="该分段占母卷总直径/重量的比例，所有分段合计应为100%">
                          <InputNumber
                            aria-label={`分段 ${index + 1} 占比`}
                            min={0}
                            max={100}
                            precision={0}
                            value={segment.segmentRatio}
                            onChange={(value) => updateSegment(segment.key, { segmentRatio: value ?? 0 })}
                            addonBefore="分段占比"
                            suffix="%"
                          />
                        </MesTooltip>
                      ) : (
                        <Tag color="default">单分段 100%</Tag>
                      )}
                      <InputNumber
                        aria-label={`分段 ${index + 1} 成品直径上限`}
                        min={0}
                        value={segment.targetDiameter}
                        onChange={(value) => updateSegment(segment.key, { targetDiameter: value ?? undefined })}
                        addonBefore="成品直径 ≤"
                        suffix="cm"
                        placeholder="不限"
                        disabled={rewindMode === 1}
                      />
                      <Select
                        aria-label={`分段 ${index + 1} 成品纸芯`}
                        value={segment.finishCoreDiameter ?? 3}
                        onChange={(value) => updateSegment(segment.key, { finishCoreDiameter: value })}
                        style={{ width: 100 }}
                        disabled={rewindMode === 1}
                        options={CORE_DIAMETER_OPTIONS.map((v) => ({ value: v, label: `纸芯 ${v}"` }))}
                      />
                      <InputNumber
                        aria-label={`分段 ${index + 1} 重复次数`}
                        min={1}
                        value={segment.repeatCount}
                        onChange={(value) => updateSegment(segment.key, { repeatCount: value ?? 1 })}
                        addonBefore="重复"
                        suffix="次"
                      />
                      <Button
                        danger
                        aria-label={`删除复卷分段 ${index + 1}`}
                        icon={<DeleteOutlined />}
                        onClick={() => removeSegment(segment.key)}
                      />
                    </Space>

                    {rewindMode === 5 && (
                      <div style={{ marginBottom: 12, padding: 8, background: '#fafafa', borderRadius: 6 }}>
                        <Space direction="vertical" style={{ width: '100%' }}>
                          <Space wrap>
                            <Typography.Text strong>来源母卷（每段分摊比例合计必须 = 100%）</Typography.Text>
                            <Typography.Text type={Math.abs(segment.sources.reduce((sum, source) => sum + source.shareRatio, 0) - 100) < 0.01 ? 'success' : 'danger'}>
                              合计 {segment.sources.reduce((sum, source) => sum + source.shareRatio, 0).toFixed(2)}%
                            </Typography.Text>
                          </Space>
                          <Select
                            aria-label={`分段 ${index + 1} 来源母卷`}
                            mode="multiple"
                            value={segment.sources.map((source) => source.originalUuid)}
                            options={sourceRollOptions}
                            onChange={(values) => updateSegmentSources(segment.key, values)}
                            placeholder="选择参与这一段接纸的母卷"
                            style={{ width: '100%' }}
                          />
                          <Space wrap>
                            {segment.sources.map((source) => {
                              const option = sourceRollOptions.find((item) => item.value === source.originalUuid)
                              return (
                                <Space key={source.originalUuid} size={4}>
                                  <TooltipText
                                    className="rewinding-config-form__source-label"
                                    value={option?.label ?? source.originalUuid}
                                  />
                                  <InputNumber
                                    aria-label={`分段 ${index + 1} 来源母卷分摊比例`}
                                    min={0}
                                    max={100}
                                    precision={2}
                                    value={source.shareRatio}
                                    onChange={(value) => updateSourceRatio(segment.key, source.originalUuid, value ?? 0)}
                                    suffix="%"
                                    style={{ width: 120 }}
                                  />
                                </Space>
                              )
                            })}
                            {segment.sources.length >= 2 && (
                              <Button
                                size="small"
                                onClick={() => {
                                  const count = segment.sources.length
                                  const avg = parseFloat((100 / count).toFixed(2))
                                  const remainder = parseFloat((100 - avg * (count - 1)).toFixed(2))
                                  segment.sources.forEach((source, i) => {
                                    updateSourceRatio(segment.key, source.originalUuid, i === count - 1 ? remainder : avg)
                                  })
                                }}
                              >
                                自动均分
                              </Button>
                            )}
                          </Space>
                        </Space>
                      </div>
                    )}

                    <Space direction="vertical" style={{ width: '100%' }}>
                      {segment.layoutItems.map((item) => (
                        <Space key={item.key} wrap>
                          <Select
                            aria-label={`分段 ${index + 1} 排布类型`}
                            value={item.itemType}
                            onChange={(value) => updateLayoutItem(segment.key, item.key, { itemType: value })}
                            style={{ width: 96 }}
                            options={[
                              { value: 'FINISH', label: '成品' },
                              { value: 'TRIM', label: '修边' },
                            ]}
                          />
                          <InputNumber
                            aria-label={`分段 ${index + 1} 排布门幅`}
                            min={1}
                            value={item.width}
                            onChange={(value) => updateLayoutItem(segment.key, item.key, { width: value ?? 1 })}
                            addonBefore="门幅"
                            suffix="mm"
                          />
                          <InputNumber
                            aria-label={`分段 ${index + 1} 排布数量`}
                            min={1}
                            value={item.quantity}
                            onChange={(value) => updateLayoutItem(segment.key, item.key, { quantity: value ?? 1 })}
                            addonBefore="数量"
                          />
                          <Button
                            type="text"
                            danger
                            aria-label={`删除${item.itemType === 'FINISH' ? '成品' : '切边'}排布`}
                            icon={<DeleteOutlined />}
                            onClick={() => removeLayoutItem(segment.key, item.key)}
                          />
                        </Space>
                      ))}
                      <Space>
                        <Button size="small" icon={<PlusOutlined />} onClick={() => addLayoutItem(segment.key, 'FINISH')}>
                          加成品门幅
                        </Button>
                        <Button size="small" onClick={() => addLayoutItem(segment.key, 'TRIM')}>
                          加修边
                        </Button>
                        <Typography.Text type={widthDanger ? 'danger' : 'secondary'}>
                          已排布 {layoutWidth} / {roll.originalWidth ?? '-'} mm
                        </Typography.Text>
                      </Space>
                      <LayoutBar
                        layoutItems={segment.layoutItems}
                        originalWidth={roll.originalWidth}
                      />
                    </Space>
                  </div>
                )
              })}

              <Divider style={{ margin: '8px 0' }} />
              <Space wrap>
                <InputNumber
                  aria-label="复卷单价"
                  min={0}
                  precision={2}
                  value={unitPrice}
                  onChange={(value) => {
                    const nextUnitPrice = value ?? 0
                    setUnitPrice(nextUnitPrice)
                    emitConfig({ nextUnitPrice })
                  }}
                  addonBefore="复卷单价"
                  suffix="元/吨"
                />
                <InputNumber
                  aria-label="备用卷号数量"
                  min={0}
                  max={10}
                  value={spareCount}
                  onChange={(value) => {
                    const nextSpareCount = value ?? 0
                    setSpareCount(nextSpareCount)
                    emitConfig({ nextSpareCount })
                  }}
                  addonBefore="备用卷号"
                  suffix="个"
                />
                <Typography.Text type="secondary">母卷吨位：{tonnage}</Typography.Text>
              </Space>
            </Space>
          </Col>

          <Col span={10}>
            <div style={{ border: '1px solid #f0f0f0', borderRadius: 6, padding: 12, minHeight: 420 }}>
              <FinishConfigPlanPreviewPanel
                segments={previewSegments}
                originalWidth={roll.originalWidth}
                preview={preview}
                spareCount={spareCount}
                loading={previewing}
              />
            </div>
          </Col>
        </Row>
      )}
    </div>
  )
}
