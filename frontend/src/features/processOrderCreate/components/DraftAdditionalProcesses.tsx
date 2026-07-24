import { Typography, message } from 'antd'
import { useState } from 'react'
import type { ProcessStepBatchResult, ProcessStepDTO } from '../../../api/processOrder'
import type { CustomerProcessPrice } from '../../../types/customer'
import type { ProcessStep } from '../../../types/processOrder'
import { formatGram, formatKg, formatMm } from '../../../utils/numberFormatters'
import { useAddProcessStep } from '../../processOrderDetail/hooks/useAddProcessStep'
import { useAddProcessStepsBatch } from '../../processOrderDetail/hooks/useAddProcessStepsBatch'
import { useDeleteProcessStep } from '../../processOrderDetail/hooks/useDeleteProcessStep'
import { useUpdateProcessStep } from '../../processOrderDetail/hooks/useUpdateProcessStep'
import type { RollDraft } from '../types'
import {
  buildServiceStepBatch,
  resolveServiceApplyTargets,
  serviceStepsForRoll,
  serviceStepTemplate,
  type FixedAmountScope,
} from '../serviceStepBatchModel'
import DraftServiceStepRow from './DraftServiceStepRow'
import DraftServiceStepEditor from './DraftServiceStepEditor'
import type { ServiceEditorStatus } from '../serviceStepEditorTypes'
import ServiceStepsLoadGate from './ServiceStepsLoadGate'

interface Props {
  allSteps: ProcessStep[]
  orderUuid?: string
  roll?: RollDraft
  selectedRolls?: RollDraft[]
  customerPrices?: CustomerProcessPrice[]
  footerContainer?: HTMLElement | null
  detailError: boolean
  detailLoading: boolean
  onRetryDetail: () => void
  onStatusChange: (status?: ServiceEditorStatus) => void
}

export default function DraftAdditionalProcesses({
  allSteps, orderUuid, roll, selectedRolls = [], customerPrices, footerContainer,
  detailError, detailLoading, onRetryDetail, onStatusChange,
}: Props) {
  const [editor, setEditor] = useState<EditorState>({ mode: 'create', revision: 0 })
  const addMutation = useAddProcessStep()
  const batchMutation = useAddProcessStepsBatch()
  const updateMutation = useUpdateProcessStep()
  const deleteMutation = useDeleteProcessStep()
  const steps = serviceStepsForRoll(allSteps, roll?.uuid)
  const resetEditor = () => setEditor((current) => ({
    mode: 'create',
    revision: current.mode === 'create' ? current.revision + 1 : 0,
  }))
  const save = async (values: ProcessStepDTO, stepUuid?: string) => {
    if (!orderUuid) return
    const sameType = steps.filter((step) => step.stepType === values.stepType)
    if (sameType.length > 1) {
      message.error('当前卷存在重复同类工艺，请先删除重复项')
      throw new Error('duplicate service steps')
    }
    const updateUuid = stepUuid ?? sameType[0]?.uuid
    if (updateUuid) await updateMutation.mutateAsync({ orderUuid, stepUuid: updateUuid, values })
    else await addMutation.mutateAsync({ orderUuid, values })
    message.success(updateUuid ? '当前卷附加工艺已更新' : '当前卷附加工艺已添加')
  }
  const saveToSelected = async (values: ProcessStepDTO, scope: FixedAmountScope) => {
    if (!orderUuid) return
    const targets = resolveServiceApplyTargets({
      rolls: selectedRolls,
      stepType: values.stepType,
      steps: allSteps,
    })
    if (!targets.targetUuids.length) {
      message.warning('请先在左侧勾选已保存且非直发的母卷')
      return
    }
    const result = await batchMutation.mutateAsync({ orderUuid, values: {
      steps: buildServiceStepBatch(values, targets.targetUuids, scope),
    } })
    showBatchSuccess(result, targets.excludedCount)
  }
  const remove = async (stepUuid: string) => {
    if (!orderUuid) return
    await deleteMutation.mutateAsync({ orderUuid, stepUuid })
    message.success('附加工艺已删除')
  }
  return (
    <section className="draft-service-processes">
      <div className="draft-service-processes__header">
        <div>
          <Typography.Text strong>附加工艺</Typography.Text>
          <Typography.Text type="secondary">服务计费，不改变成品规格</Typography.Text>
        </div>
      </div>
      <ServiceStepsLoadGate isError={detailError} isLoading={detailLoading} onRetry={onRetryDetail}>
        {roll?.uuid && <DraftServiceStepEditor
          key={`${editorKey(roll.uuid, editor)}:${steps[0]?.uuid ?? 'none'}`}
          roll={{
            uuid: roll.uuid,
            rollName: currentRollLabel(roll),
            machineContext: {
              diameter: roll.originalDiameter,
              weight: totalWeight(roll),
              width: roll.originalWidth,
            },
          }}
          customerPrices={customerPrices}
          footerContainer={footerContainer}
          initialValues={stepInitialValues(editor.mode === 'edit' ? editor.step : steps[0])}
          savedSteps={steps}
          saving={addMutation.isPending || updateMutation.isPending}
          batchSaving={batchMutation.isPending}
          selectedRollCount={selectedRolls.length}
          getTargetAnalysis={(stepType) => resolveServiceApplyTargets({
            rolls: selectedRolls,
            stepType,
            steps: allSteps,
          })}
          onCancel={resetEditor}
          onSave={save}
          onSaveToSelected={saveToSelected}
          onStatusChange={onStatusChange}
        />}
        <div className="draft-service-processes__list-header">
          <Typography.Text strong>当前卷已保存配置</Typography.Text>
          <span>{steps.length}</span>
        </div>
        {steps.length ? (
          <div className="draft-service-processes__list">
            {steps.map((step) => (
              <DraftServiceStepRow key={step.uuid} step={step}
                onEdit={() => {
                  onStatusChange(undefined)
                  setEditor({ mode: 'edit', step })
                }}
                onDelete={() => remove(step.uuid)} />
            ))}
          </div>
        ) : (
          <div className="draft-service-processes__empty">当前卷暂无附加工艺</div>
        )}
      </ServiceStepsLoadGate>
    </section>
  )
}

function showBatchSuccess(result: ProcessStepBatchResult, excludedCount: number) {
  const excluded = excludedCount ? `，${excludedCount} 卷因未保存或为直发未参与` : ''
  message.success(`已应用 ${result.selectedCount} 卷：新增 ${result.createdCount}，更新 ${result.updatedCount}${excluded}`)
}

type EditorState = { mode: 'create'; revision: number } | { mode: 'edit'; step: ProcessStep }

function editorKey(rollUuid: string, editor: EditorState) {
  return editor.mode === 'edit'
    ? `${rollUuid}:edit:${editor.step.uuid}`
    : `${rollUuid}:create:${editor.revision}`
}

function currentRollLabel(roll: RollDraft) {
  const identity = `卷号：${roll.rollNo || '-'} / 编号：${roll.extraNo || '-'}`
  return `${identity} · ${roll.paperName || '-'} / ${formatGram(roll.gramWeight)} / ${formatMm(roll.originalWidth)} / ${formatKg(totalWeight(roll))}`
}

function totalWeight(roll: RollDraft) {
  return Number(roll.rollWeight ?? 0) * Number(roll.pieceNum ?? 1)
}

function stepInitialValues(step?: ProcessStep): (ProcessStepDTO & { uuid?: string }) | undefined {
  const template = step && serviceStepTemplate(step)
  return template && step ? { ...template, uuid: step.uuid } : undefined
}
