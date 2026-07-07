import { useEffect, useMemo, useRef, useState } from 'react'
import { Alert, Button, Card, Empty, Modal, Space, Spin, Tag, Typography, message } from 'antd'
import { RedoOutlined, UndoOutlined } from '@ant-design/icons'
import { useNavigate, useParams } from 'react-router-dom'
import MesPageHeader from '../../components/layout/MesPageHeader'
import type { OriginalRoll, ProcessRoutePreviewVO } from '../../types/processOrder'
import { formatKg } from '../../utils/numberFormatters'
import { useGetDraft } from '../../features/processOrderCreate/hooks/useGetDraft'
import { useMachines, useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'
import { usePreviewRoute } from '../../features/processOrderCreate/hooks/usePreviewRoute'
import { useSaveRoute } from '../../features/processOrderCreate/hooks/useSaveRoute'
import RouteDraftFlow from '../../features/processOrderRouteDraft/RouteDraftFlow'
import RouteDraftPreviewResult from '../../features/processOrderRouteDraft/RouteDraftPreviewResult'
import RouteDraftStagePanel, { type RouteQuickAction } from '../../features/processOrderRouteDraft/RouteDraftStagePanel'
import RouteDraftSummaryBar from '../../features/processOrderRouteDraft/RouteDraftSummaryBar'
import { useRouteDraftHistory } from '../../features/processOrderRouteDraft/useRouteDraftHistory'
import {
  ORIGINAL_OUTPUT_KEY,
  allRouteOutputs,
  buildRouteDto,
  createRouteStages,
  finalRouteOutputs,
  removeOutputFromRoute,
} from '../../features/processOrderRouteDraft/routeDraftModel'
import type { RouteDraftStage } from '../../features/processOrderRouteDraft/routeDraftModel'
import type { DetailRouteOutputRow } from '../../features/processOrderDetail/routeConfigDetail'
import './RouteDesignerPage.css'

export default function RouteDesignerPage() {
  const { uuid, rollUuid } = useParams()
  const navigate = useNavigate()
  const { data: draft, isLoading: isLoadingDraft } = useGetDraft(uuid)
  const { data: machinePage } = useMachines()
  const { data: customerPage } = useCustomers()
  const roll = draft?.rolls?.find((item) => item.uuid === rollUuid)
  const config = draft?.configs?.find((item) => item.originalUuid === rollUuid)
  const customer = customerPage?.records?.find((item) => item.uuid === draft?.order?.customerUuid)
  const {
    canRedo,
    canUndo,
    commit: commitRouteStages,
    redo: redoRouteStages,
    reset: resetRouteStages,
    stages,
    undo: undoRouteStages,
  } = useRouteDraftHistory()
  const [preview, setPreview] = useState<ProcessRoutePreviewVO>()
  const [previewError, setPreviewError] = useState<string>()
  const [selectedOutputKey, setSelectedOutputKey] = useState<string>(ORIGINAL_OUTPUT_KEY)
  const [quickAction, setQuickAction] = useState<RouteQuickAction>()
  const hydratedKey = useRef<string>()
  const { mutateAsync: previewRoute, isPending: isPreviewing } = usePreviewRoute()
  const { mutateAsync: saveRoute, isPending: isSaving } = useSaveRoute()
  const prices = useMemo(() => ({
    sawUnitPrice: customer?.sawPrice,
    rewindUnitPrice: customer?.rewindPrice,
  }), [customer?.rewindPrice, customer?.sawPrice])
  const defaultPlanOptions = useMemo(() => ({
    sawPrice: customer?.sawPrice,
    rewindPrice: customer?.rewindPrice,
  }), [customer?.rewindPrice, customer?.sawPrice])

  useEffect(() => {
    const nextKey = `${uuid}-${roll?.uuid}-${config?.configType}-${customer?.sawPrice}-${customer?.rewindPrice}`
    if (!roll || !uuid || hydratedKey.current === nextKey) return
    resetRouteStages(createRouteStages(roll, config?.route, defaultPlanOptions))
    setPreview(config?.routePreview)
    setPreviewError(undefined)
    setSelectedOutputKey(ORIGINAL_OUTPUT_KEY)
    hydratedKey.current = nextKey
  }, [config, customer, defaultPlanOptions, resetRouteStages, roll, uuid])

  if (!uuid || !rollUuid) return <RouteMissing onBack={() => navigate('/process-orders')} />

  const backToDraft = () => navigate(`/process-orders/create?draft=${uuid}`)
  const handlePreview = async () => {
    if (!roll) return
    if (!stages.length) {
      message.warning('请先从母卷配置首道工艺')
      return
    }
    try {
      const result = await previewRoute({ orderUuid: uuid, request: buildRouteDto(roll, stages) })
      setPreview(result)
      setPreviewError(undefined)
      message.success('预览校验通过')
    } catch (error) {
      setPreview(undefined)
      setPreviewError(errorMessage(error))
    }
  }
  const handleSave = async () => {
    if (!roll) return
    if (!stages.length) {
      message.warning('请先从母卷配置首道工艺')
      return
    }
    try {
      const result = await saveRoute({ orderUuid: uuid, request: buildRouteDto(roll, stages) })
      resetRouteStages(stages)
      setPreview(result)
      setPreviewError(undefined)
      message.success('链式工艺已保存')
    } catch (error) {
      setPreviewError(errorMessage(error))
    }
  }
  const handleQuickAppend = (row: DetailRouteOutputRow, stepType: number) => {
    setSelectedOutputKey(row.outputKey)
    setQuickAction({ sourceKey: row.outputKey, stepType, nonce: Date.now() })
  }
  const handleDeleteFrom = (sourceKey: string) => {
    if (!roll) return
    const result = removeOutputFromRoute(roll, stages, sourceKey)
    if (!result.changed) {
      message.warning(result.message)
      return
    }
    commitRouteStages(result.stages)
    clearPreview()
    setSelectedOutputKey(ORIGINAL_OUTPUT_KEY)
    message.success(result.message)
  }
  const handleStagesChange = (nextStages: RouteDraftStage[]) => {
    commitRouteStages(nextStages)
    clearPreview()
  }
  const handleUndo = () => {
    undoRouteStages()
    clearPreview()
    setSelectedOutputKey(ORIGINAL_OUTPUT_KEY)
  }
  const handleRedo = () => {
    redoRouteStages()
    clearPreview()
    setSelectedOutputKey(ORIGINAL_OUTPUT_KEY)
  }
  const clearPreview = () => {
    setPreview(undefined)
    setPreviewError(undefined)
  }

  return (
    <Spin spinning={isLoadingDraft || isPreviewing || isSaving}>
      <div className="route-draft-page">
        <MesPageHeader
          eyebrow="链式工艺"
          title="工艺路线设计"
          description="从母卷或任一阶段产物继续配置下一道工艺，最终未被消耗的产物才会生成正式成品。"
          backText="返回新建单"
          onBack={backToDraft}
          actions={(
            <RouteActions
              canRedo={canRedo}
              canUndo={canUndo}
              isPreviewing={isPreviewing}
              isSaving={isSaving}
              onApply={() => confirmApplyToSame()}
              onBack={backToDraft}
              onPreview={handlePreview}
              onRedo={handleRedo}
              onSave={handleSave}
              onUndo={handleUndo}
            />
          )}
        />
        {!roll ? <RouteMissing onBack={backToDraft} /> : (
          <>
            <Alert type="info" showIcon message="多序路线独立配置" description="普通单道加工仍在新建单第四步完成；这里只处理需要二道、三道或更多工序的母卷。" />
            <section className="route-draft-layout">
              <RouteSourcePanel roll={roll} stages={stages} selectedKey={selectedOutputKey} onSelect={setSelectedOutputKey} />
              <Card className="route-draft-canvas-card" title="路线图">
                <RouteDraftFlow
                  roll={roll}
                  stages={stages}
                  selectedKey={selectedOutputKey}
                  onDeleteFrom={handleDeleteFrom}
                  onQuickAppend={handleQuickAppend}
                  onSelect={setSelectedOutputKey}
                />
              </Card>
              <Card className="route-draft-editor-card" title="工艺参数">
                <RouteDraftStagePanel
                  defaultPlanOptions={defaultPlanOptions}
                  machines={machinePage?.records ?? []}
                  prices={prices}
                  quickAction={quickAction}
                  roll={roll}
                  selectedKey={selectedOutputKey}
                  stages={stages}
                  onChange={handleStagesChange}
                />
              </Card>
            </section>
            {previewError && <Alert type="error" showIcon message="预览校验未通过" description={previewError} />}
            {preview && <RouteDraftPreviewResult preview={preview} />}
            <RouteDraftSummaryBar roll={roll} stages={stages} preview={preview} />
          </>
        )}
      </div>
    </Spin>
  )

  function confirmApplyToSame() {
    if (!roll || !draft?.rolls?.length) return
    if (!stages.length) {
      message.warning('请先配置并加入至少一道工艺')
      return
    }
    const targets = sameSpecRolls(draft.rolls, roll)
    Modal.confirm({
      title: '应用到同规格母卷',
      content: `将当前链式路线应用到 ${targets.length} 个同规格母卷，会覆盖这些母卷已保存的工艺草稿。`,
      okText: '确认应用',
      cancelText: '取消',
      onOk: () => applyToSame(targets),
    })
  }

  async function applyToSame(targets: OriginalRoll[]) {
    if (!targets.length || !uuid) return
    await Promise.all(targets.map((target) => saveRoute({
      orderUuid: uuid,
      request: buildRouteDto(target, stages),
    })))
    message.success(`已应用到 ${targets.length} 个同规格母卷`)
  }
}

function RouteActions({
  canRedo,
  canUndo,
  isPreviewing,
  isSaving,
  onApply,
  onBack,
  onPreview,
  onRedo,
  onSave,
  onUndo,
}: RouteActionProps) {
  return (
    <Space wrap>
      <Button onClick={onBack}>返回新建单</Button>
      <Button onClick={onApply}>应用到同类</Button>
      <Button icon={<UndoOutlined />} disabled={!canUndo} onClick={onUndo}>撤销</Button>
      <Button icon={<RedoOutlined />} disabled={!canRedo} onClick={onRedo}>重做</Button>
      <Button loading={isPreviewing} onClick={onPreview}>预览校验</Button>
      <Button type="primary" loading={isSaving} onClick={onSave}>保存草稿</Button>
    </Space>
  )
}

function errorMessage(error: unknown) {
  if (error instanceof Error) return error.message
  return '请检查工艺参数后重试'
}

function RouteSourcePanel({ onSelect, roll, selectedKey, stages }: SourcePanelProps) {
  const outputs = allRouteOutputs(roll, stages)
  const finals = new Set(finalRouteOutputs(roll, stages).map((row) => row.outputKey))
  return (
    <Card className="route-draft-source-card" title="母卷与产物">
      <button
        className={selectedKey === ORIGINAL_OUTPUT_KEY ? 'route-draft-roll route-draft-roll--selected' : 'route-draft-roll'}
        onClick={() => onSelect(ORIGINAL_OUTPUT_KEY)}
      >
        <Typography.Text strong>{roll.rollNo || roll.extraNo || '未编号母卷'}</Typography.Text>
        <span>{roll.paperName || '-'} / {roll.gramWeight ?? '-'}g / {roll.originalWidth ?? '-'}mm</span>
        <span>来料 {formatKg(Number(roll.totalWeight ?? 0))}</span>
      </button>
      <div className="route-draft-output-list">
        {outputs.length ? outputs.map((row) => (
          <button
            key={row.outputKey}
            className={selectedKey === row.outputKey ? 'route-draft-output route-draft-output--selected' : 'route-draft-output'}
            onClick={() => onSelect(row.outputKey)}
          >
            <span><b>{row.outputKey}</b><Tag color={finals.has(row.outputKey) ? 'green' : 'blue'}>{finals.has(row.outputKey) ? '最终' : '中间'}</Tag></span>
            <small>{row.paperName || '-'} / {row.gramWeight ?? '-'}g / {row.finishWidth ?? '-'}mm</small>
            <em>{formatKg(row.estimateWeight)}</em>
          </button>
        )) : <Empty description="配置首道工艺后生成阶段产物" />}
      </div>
    </Card>
  )
}

function sameSpecRolls(rolls: OriginalRoll[], current: OriginalRoll) {
  return rolls.filter((item) => item.uuid !== current.uuid
    && item.paperName === current.paperName
    && item.gramWeight === current.gramWeight
    && item.originalWidth === current.originalWidth)
}

function RouteMissing({ onBack }: { onBack: () => void }) {
  return (
    <Card>
      <Empty description="未找到草稿母卷">
        <Button type="primary" onClick={onBack}>返回</Button>
      </Empty>
    </Card>
  )
}

interface RouteActionProps {
  canRedo: boolean
  canUndo: boolean
  isPreviewing: boolean
  isSaving: boolean
  onApply: () => void
  onBack: () => void
  onPreview: () => void
  onRedo: () => void
  onSave: () => void
  onUndo: () => void
}

interface SourcePanelProps {
  onSelect: (key: string) => void
  roll: OriginalRoll
  selectedKey?: string
  stages: RouteDraftStage[]
}
