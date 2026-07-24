import { useEffect, useState } from 'react'
import { Modal, message } from 'antd'
import { usePreviewRoute } from '../../features/processOrderCreate/hooks/usePreviewRoute'
import { useSaveRoute } from '../../features/processOrderCreate/hooks/useSaveRoute'
import { useSaveRouteBatch } from '../../features/processOrderCreate/hooks/useSaveRouteBatch'
import { useRouteDraftHistory } from '../../features/processOrderRouteDraft/useRouteDraftHistory'
import { useUnsavedChangesGuard } from '../../hooks/useUnsavedChangesGuard'
import {
  ORIGINAL_OUTPUT_KEY,
  buildRouteDto,
  createRouteStages,
  removeOutputFromRoute,
} from '../../features/processOrderRouteDraft/routeDraftModel'
import type { RouteDraftStage } from '../../features/processOrderRouteDraft/routeDraftModel'
import type { DetailRouteOutputRow } from '../../features/processOrderDetail/routeConfigDetail'
import type { Customer } from '../../types/customer'
import type { Machine } from '../../types/machine'
import type {
  DraftOrderVO,
  OriginalRoll,
  ProcessConfigDraftVO,
  ProcessRoutePreviewVO,
} from '../../types/processOrder'
import RouteDesignerWorkspace from './RouteDesignerWorkspace'
import type { RouteDesignerActionState, RouteDesignerCommands } from './RouteDesignerWorkspace'

interface Props {
  config?: ProcessConfigDraftVO
  customer?: Customer
  draft: DraftOrderVO
  machines: Machine[]
  onBack: () => void
  roll: OriginalRoll
  uuid: string
}

interface OperationFeedback {
  description: string
  title: string
}

export default function RouteDesignerSession({ config, customer, draft, machines, onBack, roll, uuid }: Props) {
  const history = useRouteDraftHistory(createRouteStages(roll, config?.route, {
    rewindPrice: customer?.rewindPrice,
    sawPrice: customer?.sawPrice,
  }))
  const { clearDirty, markDirty, runIfClean } = useUnsavedChangesGuard()
  const [preview, setPreview] = useState<ProcessRoutePreviewVO | undefined>(config?.routePreview)
  const [feedback, setFeedback] = useState<OperationFeedback>()
  const [selectedOutputKey, setSelectedOutputKey] = useState(ORIGINAL_OUTPUT_KEY)
  const [quickAction, setQuickAction] = useState<{ nonce: number; sourceKey: string; stepType: number }>()
  const { mutateAsync: previewRoute, isPending: isPreviewing } = usePreviewRoute()
  const { mutateAsync: saveRoute, isPending: isSaving } = useSaveRoute()
  const { mutateAsync: saveRouteBatch, isPending: isSavingBatch } = useSaveRouteBatch()
  const draftVersion = draft.order?.version
  const targets = sameSpecRolls(draft.rolls ?? [], roll)

  useEffect(() => {
    if (history.canUndo) markDirty()
    else clearDirty()
  }, [clearDirty, history.canUndo, markDirty])

  const clearPreview = () => {
    setPreview(undefined)
    setFeedback(undefined)
  }
  const changeStages = (stages: RouteDraftStage[]) => {
    history.commit(stages)
    clearPreview()
  }
  const selectRoot = () => setSelectedOutputKey(ORIGINAL_OUTPUT_KEY)
  const undo = () => {
    history.undo()
    clearPreview()
    selectRoot()
  }
  const redo = () => {
    history.redo()
    clearPreview()
    selectRoot()
  }
  const deleteFrom = (sourceKey: string) => {
    const result = removeOutputFromRoute(roll, history.stages, sourceKey)
    if (!result.changed) return void message.warning(result.message)
    changeStages(result.stages)
    selectRoot()
    message.success(result.message)
  }
  const quickAppend = (row: DetailRouteOutputRow, stepType: number) => {
    setSelectedOutputKey(row.outputKey)
    setQuickAction({ nonce: Date.now(), sourceKey: row.outputKey, stepType })
  }
  const runPreview = async () => {
    if (draftVersion == null || history.stages.length === 0) return
    try {
      const result = await previewRoute({ orderUuid: uuid, request: routeRequest(roll, history.stages, draftVersion) })
      setPreview(result)
      setFeedback(undefined)
      message.success('预览校验通过')
    } catch (error) {
      setPreview(undefined)
      setFeedback({ title: '预览校验未通过', description: errorMessage(error) })
    }
  }
  const save = async () => {
    if (draftVersion == null || history.stages.length === 0) return
    try {
      const result = await saveRoute({ orderUuid: uuid, request: routeRequest(roll, history.stages, draftVersion) })
      history.reset(history.stages)
      clearDirty()
      setPreview(result)
      setFeedback(undefined)
      message.success('链式工艺已保存')
    } catch (error) {
      setFeedback({ title: '路线保存失败', description: errorMessage(error) })
    }
  }
  const applyToSame = async () => {
    if (draftVersion == null || targets.length === 0) return
    try {
      await saveRouteBatch({ orderUuid: uuid, dto: {
        expectedVersion: draftVersion,
        routes: targets.map((target) => buildRouteDto(target, history.stages)),
      } })
      setFeedback(undefined)
      message.success(`已应用到 ${targets.length} 个同规格母卷`)
    } catch (error) {
      setFeedback({ title: '批量应用失败', description: errorMessage(error) })
    }
  }
  const confirmApply = () => Modal.confirm({
    cancelText: '取消',
    content: `将当前链式路线应用到 ${targets.length} 个同规格母卷，会覆盖这些母卷已保存的工艺草稿。`,
    okText: '确认应用',
    onOk: applyToSame,
    title: '应用到同规格母卷',
  })

  const actionState: RouteDesignerActionState = {
    applyDisabledReason: applyDisabledReason(history.stages.length, targets.length, draftVersion),
    canRedo: history.canRedo,
    canUndo: history.canUndo,
    previewDisabledReason: routeDisabledReason(history.stages.length, draftVersion),
    saveDisabledReason: saveDisabledReason(history.stages.length, draftVersion, history.canUndo),
  }
  const commands: RouteDesignerCommands = {
    onApply: confirmApply,
    onBack: () => runIfClean(onBack),
    onDeleteFrom: deleteFrom,
    onPreview: runPreview,
    onQuickAppend: quickAppend,
    onRedo: redo,
    onSave: save,
    onSelect: setSelectedOutputKey,
    onStagesChange: changeStages,
    onUndo: undo,
  }

  return (
    <RouteDesignerWorkspace
      actionState={actionState}
      busy={isPreviewing || isSaving || isSavingBatch}
      commands={commands}
      defaultPlanOptions={{ rewindPrice: customer?.rewindPrice, sawPrice: customer?.sawPrice }}
      feedback={feedback}
      machines={machines}
      orderLabel={`加工单：${draft.order?.orderNo || '未生成单号'}`}
      preview={preview}
      prices={{ rewindUnitPrice: customer?.rewindPrice, sawUnitPrice: customer?.sawPrice }}
      quickAction={quickAction}
      roll={roll}
      selectedOutputKey={selectedOutputKey}
      stages={history.stages}
    />
  )
}

function routeRequest(roll: OriginalRoll, stages: RouteDraftStage[], expectedVersion: number) {
  return { ...buildRouteDto(roll, stages), expectedVersion }
}

function routeDisabledReason(stageCount: number, draftVersion?: number) {
  if (draftVersion == null) return '草稿版本缺失，请返回新建单重新加载'
  if (stageCount === 0) return '请先配置并加入至少一道工艺'
  return undefined
}

function saveDisabledReason(stageCount: number, draftVersion: number | undefined, changed: boolean) {
  return routeDisabledReason(stageCount, draftVersion) ?? (!changed ? '当前路线没有待保存修改' : undefined)
}

function applyDisabledReason(stageCount: number, targetCount: number, draftVersion?: number) {
  return routeDisabledReason(stageCount, draftVersion) ?? (targetCount === 0 ? '没有其他同品名、克重和门幅的母卷' : undefined)
}

function sameSpecRolls(rolls: OriginalRoll[], current: OriginalRoll) {
  return rolls.filter((item) => item.uuid !== current.uuid
    && item.paperName === current.paperName
    && item.gramWeight === current.gramWeight
    && item.originalWidth === current.originalWidth)
}

function errorMessage(error: unknown) {
  return error instanceof Error ? error.message : '请检查工艺参数后重试'
}
