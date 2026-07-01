import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { Button, Card, Checkbox, List, Space, Spin, Tag, Typography, message } from 'antd'
import { ArrowLeftOutlined, ArrowRightOutlined } from '@ant-design/icons'
import { getProcessOrder, saveFinishConfig } from '../../api/processOrder'
import type { FinishConfigSaveDTO, OriginalRoll, ProcessOrderDetailVO } from '../../types/processOrder'
import { PROCESS_MODE, STEP_TYPE } from '../../constants/processOrder'
import FinishConfigPanel from '../../components/processOrder/FinishConfigPanel'

const buildDefaultConfig = (roll: OriginalRoll): FinishConfigSaveDTO => {
  const processMode = roll.processMode ?? 1
  const mainStepType = roll.mainStepType
  const originalWidth = roll.originalWidth ?? 500

  if (mainStepType === 1) {
    return {
      processMode,
      mainStepType,
      knifeCount: 0,
      unitPrice: 1.5,
      spareCount: 0,
      finishSpecs: [{ count: 1, finishWidth: processMode === 2 ? 0 : 400, estimateWeight: 0 }],
    }
  }

  if (mainStepType === 2) {
    return {
      processMode,
      mainStepType,
      rewindMode: 1,
      unitPrice: 200,
      spareCount: 0,
      finishSpecs: [
        {
          count: 1,
          finishWidth: processMode === 2 ? 0 : originalWidth,
          finishDiameter: 0,
          finishCoreDiameter: 3,
          estimateWeight: 0,
          splitRatio: 100,
        },
      ],
      rewindSegments: [
        {
          segmentSort: 1,
          segmentRatio: 1,
          targetDiameter: undefined,
          finishCoreDiameter: 3,
          repeatCount: 1,
          sources: [{ originalUuid: roll.uuid, shareRatio: 100 }],
          layoutItems: [{ width: originalWidth, quantity: 1, itemType: 'FINISH' }],
        },
      ],
    }
  }

  return {
    processMode,
    mainStepType,
    spareCount: 0,
    finishSpecs: [],
  }
}

export default function ConfigFinishPage() {
  const { uuid } = useParams<{ uuid: string }>()
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [detail, setDetail] = useState<ProcessOrderDetailVO | null>(null)
  const [selectedRollIndex, setSelectedRollIndex] = useState(0)
  const [selectedRollUuids, setSelectedRollUuids] = useState<string[]>([])
  const [saving, setSaving] = useState(false)
  const [currentConfig, setCurrentConfig] = useState<FinishConfigSaveDTO | null>(null)
  const [configsByRollUuid, setConfigsByRollUuid] = useState<Record<string, FinishConfigSaveDTO>>({})

  const fetchDetail = useCallback((resetConfigs = true) => {
    if (!uuid) return
    setLoading(true)
    getProcessOrder(uuid)
      .then((data) => {
        setDetail(data)
        if (resetConfigs) {
          setSelectedRollIndex(0)
          setCurrentConfig(null)
          setConfigsByRollUuid({})
        }
      })
      .finally(() => setLoading(false))
  }, [uuid])

  useEffect(() => {
    if (uuid) {
      fetchDetail()
    }
  }, [fetchDetail, uuid])

  const originalRolls = detail?.originalRolls ?? []
  const rollProductions = detail?.rollProductions ?? []
  const rollProdByUuid = new Map(rollProductions.map((rp) => [rp.originalUuid, rp]))

  const getFinishCount = (rollUuid: string) => {
    const rp = rollProdByUuid.get(rollUuid)
    return rp?.finishes?.length ?? 0
  }

  // 找出被其他原纸的 mode 5 合并复卷引用的来源原纸
  const sourceOnlyUuids = new Set<string>()
  for (const rp of rollProductions) {
    if (rp.finishes) {
      for (const finish of rp.finishes) {
        if (finish.sources) {
          for (const source of finish.sources) {
            if (source.originalUuid && source.originalUuid !== rp.originalUuid) {
              sourceOnlyUuids.add(source.originalUuid)
            }
          }
        }
      }
    }
  }

  const currentRoll = originalRolls[selectedRollIndex]
  const currentRollUuid = currentRoll?.uuid
  const activeConfig = currentRollUuid ? currentConfig ?? configsByRollUuid[currentRollUuid] ?? null : null

  const handleConfigChange = (config: FinishConfigSaveDTO) => {
    if (!currentRollUuid) return
    setCurrentConfig(config)
    setConfigsByRollUuid((prev) => ({ ...prev, [currentRollUuid]: config }))
  }

  const selectRollIndex = (index: number) => {
    const roll = originalRolls[index]
    setSelectedRollIndex(index)
    setCurrentConfig(roll ? configsByRollUuid[roll.uuid] ?? null : null)
  }

  const configForRoll = (roll: OriginalRoll) => {
    const config = configsByRollUuid[roll.uuid] ?? buildDefaultConfig(roll)
    return {
      ...config,
      processMode: roll.processMode ?? config.processMode,
      mainStepType: roll.mainStepType ?? config.mainStepType,
    }
  }

  const handlePrevRoll = () => {
    if (selectedRollIndex > 0) {
      selectRollIndex(selectedRollIndex - 1)
    }
  }

  const handleNextRoll = () => {
    if (selectedRollIndex < originalRolls.length - 1) {
      selectRollIndex(selectedRollIndex + 1)
    }
  }

  const handleRollSelect = (rollUuid: string, checked: boolean) => {
    if (checked) {
      setSelectedRollUuids([...selectedRollUuids, rollUuid])
    } else {
      setSelectedRollUuids(selectedRollUuids.filter((item) => item !== rollUuid))
    }
  }

  const handleCopyToSelected = () => {
    if (!currentRoll || selectedRollUuids.length === 0) {
      message.warning('请先勾选要复制到的原纸')
      return
    }
    const sourceConfig = activeConfig ?? buildDefaultConfig(currentRoll)
    setConfigsByRollUuid((prev) => {
      const next = { ...prev }
      selectedRollUuids.forEach((rollUuid) => {
        next[rollUuid] = sourceConfig
      })
      return next
    })
    message.success(`已复制配置到 ${selectedRollUuids.length} 个原纸`)
  }

  const saveCurrentRoll = async () => {
    if (!uuid || !currentRoll) {
      return null
    }
    return saveFinishConfig(uuid, currentRoll.uuid, configForRoll(currentRoll))
  }

  const handleSaveCurrent = async () => {
    setSaving(true)
    try {
      const result = await saveCurrentRoll()
      if (result) {
        message.success(`保存成功，已生成 ${result.finishRollNos?.length ?? 0} 个正式成品号`)
        fetchDetail(false)
      }
    } finally {
      setSaving(false)
    }
  }

  const handleSaveAll = async () => {
    if (!uuid) return
    setSaving(true)
    try {
      // 只保存用户配置过但尚未生成成品的原纸，跳过：
      // 1. 已有成品的（已通过"保存当前"保存过）
      // 2. 作为 mode 5 来源的原纸（材料已被合并消耗，无需单独配置）
      // 3. 不加工直发的原纸
      const toSave: OriginalRoll[] = []
      const skippedConfigured: string[] = []
      const skippedSource: string[] = []
      for (const roll of originalRolls) {
        if (roll.processMode === 3) continue // 直发
        if (getFinishCount(roll.uuid) > 0) {
          skippedConfigured.push(`原纸${originalRolls.indexOf(roll) + 1}`)
          continue
        }
        if (sourceOnlyUuids.has(roll.uuid)) {
          skippedSource.push(`原纸${originalRolls.indexOf(roll) + 1}`)
          continue
        }
        toSave.push(roll)
      }

      if (toSave.length === 0) {
        if (skippedConfigured.length > 0 || skippedSource.length > 0) {
          const parts: string[] = []
          if (skippedConfigured.length > 0) parts.push(`${skippedConfigured.join('、')} 已配置过`)
          if (skippedSource.length > 0) parts.push(`${skippedSource.join('、')} 已作为合并来源`)
          message.info(`无需保存：${parts.join('；')}`)
        } else {
          message.info('没有需要保存的原纸')
        }
        setSaving(false)
        return
      }

      let finishCount = 0
      for (const roll of toSave) {
        const result = await saveFinishConfig(uuid, roll.uuid, configForRoll(roll))
        finishCount += result.finishRollNos?.length ?? 0
      }
      const extra = []
      if (skippedConfigured.length > 0) extra.push(`${skippedConfigured.join('、')} 已配置（跳过）`)
      if (skippedSource.length > 0) extra.push(`${skippedSource.join('、')} 为合并来源（跳过）`)
      message.success(`保存成功，新生成 ${finishCount} 个正式成品号${extra.length > 0 ? '；' + extra.join('；') : ''}`)
      navigate('/process-orders')
    } finally {
      setSaving(false)
    }
  }

  const getProcessModeText = (mode?: number) => (mode ? PROCESS_MODE[mode] ?? '-' : '-')
  const getStepTypeText = (type?: number) => (type ? STEP_TYPE[type] ?? '-' : '-')

  return (
    <div style={{ padding: 24 }}>
      <Card
        title={
          <Space>
            <Typography.Title level={4} style={{ margin: 0 }}>
              成品规格配置
            </Typography.Title>
            <Typography.Text type="secondary">
              加工单：{detail?.order.orderNo} | 客户：{detail?.order.customerName}
            </Typography.Text>
          </Space>
        }
        extra={
          <Space>
            <Button onClick={() => navigate('/process-orders')}>取消</Button>
            <Button type="primary" loading={saving} onClick={handleSaveAll}>
              保存全部并完成
            </Button>
          </Space>
        }
      >
        <Spin spinning={loading}>
          <div style={{ display: 'flex', gap: 24, minHeight: 600 }}>
            <div style={{ width: 280, borderRight: '1px solid #f0f0f0', paddingRight: 16 }}>
              <div style={{ marginBottom: 12, padding: 8, background: '#fafafa', borderRadius: 4, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Typography.Text strong>
                  原纸列表 ({originalRolls.length})
                </Typography.Text>
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                  已配 {originalRolls.filter((r) => getFinishCount(r.uuid) > 0).length}/{originalRolls.length}
                </Typography.Text>
              </div>
              <List
                size="small"
                dataSource={originalRolls}
                renderItem={(roll, index) => {
                  const finishCount = getFinishCount(roll.uuid)
                  const isConfigured = finishCount > 0
                  const isSourceOnly = !isConfigured && sourceOnlyUuids.has(roll.uuid)
                  return (
                  <List.Item
                    key={roll.uuid}
                    style={{
                      cursor: 'pointer',
                      padding: 12,
                      background: selectedRollIndex === index ? '#e6f7ff' : isConfigured ? '#f6ffed' : 'transparent',
                      borderRadius: 4,
                      marginBottom: 4,
                      border: selectedRollIndex === index ? '1px solid #1890ff' : isConfigured ? '1px solid #b7eb8f' : '1px solid #f0f0f0',
                    }}
                    onClick={() => {
                      selectRollIndex(index)
                    }}
                  >
                    <div style={{ width: '100%' }}>
                      <div style={{ display: 'flex', alignItems: 'center', marginBottom: 8 }}>
                        <Checkbox
                          checked={selectedRollUuids.includes(roll.uuid)}
                          onChange={(e) => handleRollSelect(roll.uuid, e.target.checked)}
                          onClick={(e) => e.stopPropagation()}
                          style={{ marginRight: 8 }}
                        />
                        <Typography.Text strong>
                          原纸{index + 1}
                        </Typography.Text>
                        {isConfigured && (
                          <Tag color="success" style={{ marginLeft: 8, fontSize: 11 }}>已配 {finishCount}件</Tag>
                        )}
                        {isSourceOnly && (
                          <Tag color="processing" style={{ marginLeft: 8, fontSize: 11 }}>已作为合并来源</Tag>
                        )}
                      </div>
                      <div style={{ fontSize: 12, color: '#666' }}>
                        <div>{roll.paperName}</div>
                        <div>
                          {roll.gramWeight}g | {roll.originalWidth}mm
                        </div>
                        <div>
                          {roll.rollWeight}kg × {roll.pieceNum || 1}件
                        </div>
                        <div style={{ marginTop: 4 }}>
                          <Typography.Text type="secondary">{getProcessModeText(roll.processMode)}</Typography.Text>
                          {' / '}
                          <Typography.Text type="secondary">{getStepTypeText(roll.mainStepType)}</Typography.Text>
                        </div>
                      </div>
                    </div>
                  </List.Item>
                  )
                }}
              />
              {selectedRollUuids.length > 0 && (
                <div style={{ marginTop: 12 }}>
                  <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    已选中 {selectedRollUuids.length} 项
                  </Typography.Text>
                </div>
              )}
            </div>

            <div style={{ flex: 1 }}>
              {currentRoll ? (
                <FinishConfigPanel
                  key={currentRoll.uuid}
                  roll={currentRoll}
                  originalRolls={originalRolls}
                  order={detail!.order}
                  config={activeConfig ?? buildDefaultConfig(currentRoll)}
                  onCopyToSelected={handleCopyToSelected}
                  selectedCount={selectedRollUuids.length}
                  onConfigChange={handleConfigChange}
                  onSaveCurrent={handleSaveCurrent}
                />
              ) : (
                <div style={{ textAlign: 'center', padding: 48, color: '#999' }}>
                  请选择原纸进行配置
                </div>
              )}
            </div>
          </div>

          <div
            style={{
              marginTop: 24,
              paddingTop: 16,
              borderTop: '1px solid #f0f0f0',
              display: 'flex',
              justifyContent: 'space-between',
            }}
          >
            <Button icon={<ArrowLeftOutlined />} onClick={handlePrevRoll} disabled={selectedRollIndex === 0}>
              上一个原纸
            </Button>
            <Button
              icon={<ArrowRightOutlined />}
              onClick={handleNextRoll}
              disabled={selectedRollIndex >= originalRolls.length - 1}
            >
              下一个原纸
            </Button>
          </div>
        </Spin>
      </Card>
    </div>
  )
}
