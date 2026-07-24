import { useState } from 'react'
import { Empty, Spin, message } from 'antd'
import type { ProcessStepDTO, ProcessStepPricingAdjustmentDTO } from '../../../api/processOrder'
import type { OriginalRoll, ProcessStep } from '../../../types/processOrder'
import ProcessStepFormModal from '../../../components/processOrder/ProcessStepFormModal'
import QueryLoadErrorAlert from '../../../components/feedback/QueryLoadErrorAlert'
import { useAddProcessStep } from '../hooks/useAddProcessStep'
import { useDeleteProcessStep } from '../hooks/useDeleteProcessStep'
import { useProcessOrderDetail } from '../hooks/useProcessOrderDetail'
import { useUpdateProcessStep } from '../hooks/useUpdateProcessStep'
import { useAdjustProcessStepPricing } from '../hooks/useAdjustProcessStepPricing'
import type { ProcessRouteConfigTarget } from '../routeConfigTypes'
import OrderDetailView from './OrderDetailView'
import ProcessRouteConfigDrawer from './ProcessRouteConfigDrawer'
import ProcessStepPricingModal from './ProcessStepPricingModal'

interface Props {
  uuid?: string | null
  mode?: 'page' | 'drawer'
  enabled?: boolean
  onBack?: () => void
}

export default function OrderDetailPanel({
  uuid,
  mode = 'page',
  enabled = true,
  onBack,
}: Props) {
  const [stepFormOpen, setStepFormOpen] = useState(false)
  const [routeConfigOpen, setRouteConfigOpen] = useState(false)
  const [routeConfigTarget, setRouteConfigTarget] = useState<ProcessRouteConfigTarget>({ mode: 'replace' })
  const [editingStep, setEditingStep] = useState<ProcessStep | null>(null)
  const [pricingStep, setPricingStep] = useState<ProcessStep | null>(null)
  const detailQuery = useProcessOrderDetail(uuid ?? undefined, { enabled })
  const { data: detail, isError: isDetailError, isLoading: isLoadingDetail } = detailQuery
  const { mutateAsync: addStep } = useAddProcessStep()
  const { mutateAsync: updateStep } = useUpdateProcessStep()
  const { mutateAsync: deleteStep } = useDeleteProcessStep()
  const { mutateAsync: adjustPricing, isPending: isAdjustingPricing } = useAdjustProcessStepPricing()

  const handleAdd = () => {
    setEditingStep(null)
    setStepFormOpen(true)
  }

  const handleEdit = (step: ProcessStep) => {
    setEditingStep(step)
    setStepFormOpen(true)
  }

  const handleDelete = async (stepUuid: string) => {
    if (!uuid) return
    await deleteStep({ orderUuid: uuid, stepUuid })
    message.success('删除工序成功')
  }

  const handleConfigureRoute = (target: ProcessRouteConfigTarget) => {
    setRouteConfigTarget(target)
    setRouteConfigOpen(true)
  }

  const handleAdjustPricing = async (values: ProcessStepPricingAdjustmentDTO) => {
    if (!uuid || !pricingStep) return
    await adjustPricing({ orderUuid: uuid, stepUuid: pricingStep.uuid, values })
    message.success('计价核定已保存')
    setPricingStep(null)
  }

  const handleStepFormOk = async (values: ProcessStepDTO, stepUuid?: string) => {
    if (!uuid) return
    if (stepUuid) {
      await updateStep({ orderUuid: uuid, stepUuid, values })
      message.success('编辑工序成功')
      return
    }
    await addStep({ orderUuid: uuid, values })
    message.success('新增工序成功')
  }

  return (
    <Spin spinning={isLoadingDetail} wrapperClassName="mes-spin-fill">
      {!uuid ? (
        <Empty description="未选择加工单" />
      ) : isDetailError ? (
        <QueryLoadErrorAlert
          message="加工单详情加载失败"
          description="详情未成功加载，当前空白不代表加工单不存在。"
          onRetry={() => void detailQuery.refetch()}
        />
      ) : !detail && !isLoadingDetail ? (
        <Empty description="加工单不存在或已删除" />
      ) : (
        <OrderDetailView
          detail={detail}
          mode={mode}
          onBack={onBack}
          onAddStep={handleAdd}
          onConfigureRoute={handleConfigureRoute}
          onEditStep={handleEdit}
          onDeleteStep={handleDelete}
          onAdjustPricing={setPricingStep}
        />
      )}

      <ProcessStepFormModal
        open={stepFormOpen}
        originalRolls={buildRollOptions(detail?.originalRolls)}
        initialValues={buildInitialValues(editingStep)}
        onCancel={() => {
          setStepFormOpen(false)
          setEditingStep(null)
        }}
        onOk={handleStepFormOk}
      />
      {routeConfigOpen && (
        <ProcessRouteConfigDrawer
          open={routeConfigOpen}
          detail={detail}
          mode={routeConfigTarget.mode}
          initialOriginalUuid={routeConfigTarget.originalUuid}
          initialOutputKey={routeConfigTarget.outputKey}
          onClose={() => setRouteConfigOpen(false)}
        />
      )}
      <ProcessStepPricingModal
        key={pricingStep?.uuid ?? 'pricing-modal-closed'}
        open={pricingStep != null}
        step={pricingStep}
        originalRoll={detail?.originalRolls?.find((roll) => roll.uuid === pricingStep?.originalUuid)}
        loading={isAdjustingPricing}
        onCancel={() => setPricingStep(null)}
        onSubmit={handleAdjustPricing}
      />
    </Spin>
  )
}

function buildRollOptions(rolls: OriginalRoll[] = []) {
  return rolls.map((roll) => ({
    uuid: roll.uuid,
    rollName: roll.rollNo || roll.extraNo || roll.paperName || '未命名母卷',
  }))
}

function buildInitialValues(step: ProcessStep | null): (ProcessStepDTO & { uuid?: string }) | undefined {
  if (!step) return undefined
  return {
    uuid: step.uuid,
    originalUuid: step.originalUuid!,
    stepType: step.stepType!,
    stepName: step.stepName,
    machineUuid: step.machineUuid,
    isMain: step.isMain,
    knifeCount: step.knifeCount,
    processWeight: step.processWeight,
    billingBasis: step.billingBasis,
    serviceQuantity: step.serviceQuantity,
    billingMode: step.billingMode,
    billingAmount: step.billingAmount,
    unitPrice: step.unitPrice,
    remark: step.remark,
  }
}
