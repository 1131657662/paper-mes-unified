import { Alert, Button, Drawer, Empty, Modal, Space, message } from 'antd'
import { SyncOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import { useState, type Dispatch, type SetStateAction } from 'react'
import type {
  ProcessOrderDetailVO,
  ProcessRoutePreviewVO,
} from '../../../types/processOrder'
import { queries } from '../../../queries'
import { usePreviewPendingRoute } from '../hooks/usePreviewPendingRoute'
import { useSavePendingRoute } from '../hooks/useSavePendingRoute'
import { usePreviewAppendRoute } from '../hooks/usePreviewAppendRoute'
import { useSaveAppendRoute } from '../hooks/useSaveAppendRoute'
import RouteReferenceGate from './RouteReferenceGate'
import ProcessRouteStageList from './ProcessRouteStageList'
import { withLastStageMachine } from '../processRouteConfigMachine'
import {
  RouteBaseControls,
  RouteDrawerFooter,
  RouteFirstOutputTable,
  RouteResult,
  RouteStageToolbar,
} from './ProcessRouteConfigSections'
import {
  addDetailRouteStage,
  buildAppendRouteDto,
  buildDetailRouteDto,
  detailRoutePriceDefaults,
  finalDetailRouteOutputs,
  initialDetailRouteFormForOrder,
  type DetailRouteFormState,
} from '../routeConfigDetail'
import { routeRequestFingerprint, isRoutePreviewCurrent } from '../routePreviewGuard'

interface Props {
  open: boolean
  detail?: ProcessOrderDetailVO
  mode?: 'replace' | 'append'
  initialOriginalUuid?: string
  initialOutputKey?: string
  onClose: () => void
}

export default function ProcessRouteConfigDrawer({
  open,
  detail,
  mode = 'replace',
  initialOriginalUuid,
  initialOutputKey,
  onClose,
}: Props) {
  const appendMode = mode === 'append'
  const rolls = detail?.originalRolls ?? []
  const [selectedUuid, setSelectedUuid] = useState(() => initialOriginalUuid ?? rolls[0]?.uuid)
  const roll = rolls.find((item) => item.uuid === selectedUuid) ?? rolls[0]
  const customerQuery = useQuery(queries.createOrder.customers)
  const machineQuery = useQuery(queries.createOrder.machines)
  const customerPage = customerQuery.data
  const machinePage = machineQuery.data
  const referenceError = customerQuery.isError || machineQuery.isError
  const referenceLoading = customerQuery.isLoading || machineQuery.isLoading
  const machines = machinePage?.records ?? []
  const prices = detailRoutePriceDefaults(detail, roll?.uuid, customerPage?.records ?? [])
  const [form, setForm] = useState<DetailRouteFormState | undefined>(() => roll && initialDetailRouteFormForOrder(
    detail,
    roll,
    prices,
    appendMode,
    initialOutputKey,
  ))
  const [preview, setPreview] = useState<ProcessRoutePreviewVO>()
  const [previewFingerprint, setPreviewFingerprint] = useState<string>()
  const { mutateAsync: previewRoute, isPending: isPreviewing } = usePreviewPendingRoute()
  const { mutateAsync: saveRoute, isPending: isSaving } = useSavePendingRoute()
  const { mutateAsync: previewAppendRoute, isPending: isPreviewingAppend } = usePreviewAppendRoute()
  const { mutateAsync: saveAppendRoute, isPending: isSavingAppend } = useSaveAppendRoute()
  const clearPreview = () => { setPreview(undefined); setPreviewFingerprint(undefined) }
  const setRouteForm: Dispatch<SetStateAction<DetailRouteFormState | undefined>> = (next) => {
    setForm(next)
    clearPreview()
  }
  const currentRequest = roll && form ? (appendMode ? buildAppendRouteDto(roll, form) : buildDetailRouteDto(roll, form)) : undefined
  const previewCurrent = isRoutePreviewCurrent(previewFingerprint, currentRequest)

  const handleRollChange = (uuid: string) => {
    const nextRoll = rolls.find((item) => item.uuid === uuid)
    setSelectedUuid(uuid)
    setForm(nextRoll && initialDetailRouteFormForOrder(
      detail,
      nextRoll,
      detailRoutePriceDefaults(detail, nextRoll?.uuid, customerPage?.records ?? []),
      appendMode,
    ))
    clearPreview()
  }

  const addStage = (stepType: number) => {
    if (!form) return
    setRouteForm(withLastStageMachine(addDetailRouteStage(form, stepType, prices), machines))
  }

  const handlePreview = async () => {
    if (!detail?.order.uuid || !roll || !form) return
    if (!requireRouteReady(form, appendMode)) return
    const request = appendMode ? buildAppendRouteDto(roll, form) : buildDetailRouteDto(roll, form)
    const action = appendMode ? previewAppendRoute : previewRoute
    setPreview(await action({ orderUuid: detail.order.uuid, request }))
    setPreviewFingerprint(routeRequestFingerprint(request))
  }

  const persistRoute = async (request: NonNullable<typeof currentRequest>) => {
    const action = appendMode ? saveAppendRoute : saveRoute
    await action({ orderUuid: detail!.order.uuid, request })
    message.success(appendMode ? '已按所选产物追加工艺，成品方案和加工费已重算' : '整卷工艺已重配，工序、阶段产物和成品号已重建')
    onClose()
  }

  const handleSave = () => {
    if (!detail?.order.uuid || !roll || !form) return
    if (!requireRouteReady(form, appendMode)) return
    const request = appendMode ? buildAppendRouteDto(roll, form) : buildDetailRouteDto(roll, form)
    if (!previewCurrent || routeRequestFingerprint(request) !== previewFingerprint) {
      message.warning('表单已变化，请先重新预览费用与最终产出')
      return
    }
    if (appendMode) {
      void persistRoute(request)
      return
    }
    Modal.confirm({
      title: '确认重配整卷工艺路线？',
      content: '保存后会清理旧工序、旧阶段产物和旧预生成成品号，再按当前预览重建。',
      okButtonProps: { danger: true },
      okText: '确认重配',
      cancelText: '取消',
      onOk: () => persistRoute(request),
    })
  }

  return (
    <Drawer
      title={appendMode ? '选择产物追加工艺' : '重配整卷工艺路线'}
      open={open}
      width="min(1180px, 96vw)"
      onClose={onClose}
      destroyOnHidden
      footer={<RouteDrawerFooter appendMode={appendMode}
        disabledReason={referenceError ? '客户价格或机台资料加载失败，请先重新加载'
          : referenceLoading ? '正在加载客户价格与机台资料'
          : !roll || !form ? '暂无可配置的母卷'
            : !previewCurrent ? '请先预览当前配置，表单变化后需重新预览' : undefined}
        loading={appendMode ? isSavingAppend : isSaving} onClose={onClose} onSave={handleSave} />}
    >
      <RouteReferenceGate isError={referenceError} isLoading={referenceLoading}
        onRetry={() => void Promise.all([customerQuery.refetch(), machineQuery.refetch()])}>
        {!roll || !form ? <Empty description="暂无可配置的母卷" /> : (
          <Space direction="vertical" size={14} className="process-route-config">
          <Alert
            type="info"
            showIcon
            message={appendMode
              ? '先选要继续加工的阶段产物，再配置下一段工艺；保存后只新增后续工序和新成品，不清理原有路线。'
              : '用于待下发单据的整卷工艺重配；保存后会按当前路线清理旧工序、旧阶段产物和旧预生成成品号，再重新生成。'}
          />
          <RouteBaseControls roll={roll} rolls={rolls} onRollChange={handleRollChange} />
          <RouteFirstOutputTable appendMode={appendMode} rows={form.firstOutputs} selectedOutputKey={initialOutputKey} />
          <RouteStageToolbar appendMode={appendMode} disabled={!finalDetailRouteOutputs(form).length} onAdd={addStage} />
          <ProcessRouteStageList appendMode={appendMode} form={form} machines={machines} prices={prices} setForm={setRouteForm} />
          <Button icon={<SyncOutlined />} loading={appendMode ? isPreviewingAppend : isPreviewing} onClick={handlePreview}>
            预览费用与最终产出
          </Button>
          <RouteResult form={form} preview={preview} />
          </Space>
        )}
      </RouteReferenceGate>
    </Drawer>
  )
}

function requireRouteReady(form: DetailRouteFormState, appendMode: boolean) {
  if (!appendMode) return true
  if (form.stages.length > 0) return true
  message.warning('请先选择产物并配置至少一道追加工艺')
  return false
}
