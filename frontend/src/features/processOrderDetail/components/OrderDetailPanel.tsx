import { useState } from 'react'
import { Empty, Spin, message } from 'antd'
import type { ProcessStepDTO } from '../../../api/processOrder'
import type { OriginalRoll, ProcessStep } from '../../../types/processOrder'
import ProcessStepFormModal from '../../../components/processOrder/ProcessStepFormModal'
import { useAddProcessStep } from '../hooks/useAddProcessStep'
import { useDeleteProcessStep } from '../hooks/useDeleteProcessStep'
import { useProcessOrderDetail } from '../hooks/useProcessOrderDetail'
import { useUpdateProcessStep } from '../hooks/useUpdateProcessStep'
import OrderDetailView from './OrderDetailView'

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
  const [editingStep, setEditingStep] = useState<ProcessStep | null>(null)
  const { data: detail, isLoading: isLoadingDetail } = useProcessOrderDetail(uuid ?? undefined, { enabled })
  const { mutateAsync: addStep } = useAddProcessStep()
  const { mutateAsync: updateStep } = useUpdateProcessStep()
  const { mutateAsync: deleteStep } = useDeleteProcessStep()

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
      ) : !detail && !isLoadingDetail ? (
        <Empty description="加工单不存在或已删除" />
      ) : (
        <OrderDetailView
          detail={detail}
          mode={mode}
          onBack={onBack}
          onAddStep={handleAdd}
          onEditStep={handleEdit}
          onDeleteStep={handleDelete}
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
    isMain: step.isMain,
    knifeCount: step.knifeCount,
    processWeight: step.processWeight,
    unitPrice: step.unitPrice,
    remark: step.remark,
  }
}
