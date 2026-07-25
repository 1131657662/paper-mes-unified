import { Typography, message } from 'antd'
import { useRef, useState } from 'react'
import type { ProcessStepDTO } from '../../../api/processOrder'
import type { CustomerProcessPrice } from '../../../types/customer'
import type { ProcessStep } from '../../../types/processOrder'
import { formatGram, formatKg, formatMm } from '../../../utils/numberFormatters'
import { useApplyDraftServiceStep } from '../hooks/useApplyDraftServiceStep'
import { useDeleteDraftServiceStep } from '../hooks/useDeleteDraftServiceStep'
import { useSaveDraftServiceStep } from '../hooks/useSaveDraftServiceStep'
import { serviceEditorActionBlockedReason } from '../serviceEditorGuard'
import type { RollDraft } from '../types'
import {
  resolveServiceApplyTargets,
  serviceStepsForRoll,
  serviceStepTemplate,
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
  detailError: boolean
  detailLoading: boolean
  onRetryDetail: () => void
  onStatusChange: (status?: ServiceEditorStatus) => void
  onSynchronizeVersion: () => Promise<void>
  onVersionSyncBlockedChange: (blocked: boolean) => void
  onWritePendingChange: (pending: boolean) => void
  versionSyncBlocked: boolean
}

export default function DraftAdditionalProcesses({
  allSteps, orderUuid, roll, selectedRolls = [], customerPrices,
  detailError, detailLoading, onRetryDetail, onStatusChange,
  onSynchronizeVersion, onVersionSyncBlockedChange, onWritePendingChange, versionSyncBlocked,
}: Props) {
  const [editor, setEditor] = useState<EditorState>({ mode: 'create', revision: 0 })
  const [editorStatus, setEditorStatus] = useState<ServiceEditorStatus>()
  const [writing, setWriting] = useState(false)
  const pendingWriteCount = useRef(0)
  const steps = serviceStepsForRoll(allSteps, roll?.uuid)
  const mutationOptions = {
    onSynchronizeVersion,
    onVersionSyncBlockedChange,
    orderUuid,
    versionSyncBlocked,
  }
  const saveMutation = useSaveDraftServiceStep({ ...mutationOptions, steps })
  const applyMutation = useApplyDraftServiceStep({
    ...mutationOptions,
    allSteps,
    selectedRolls,
  })
  const deleteMutation = useDeleteDraftServiceStep(mutationOptions)
  const writePending = writing || saveMutation.isPending || applyMutation.isPending || deleteMutation.isPending
  const runWrite = async (operation: () => Promise<void>) => {
    pendingWriteCount.current += 1
    if (pendingWriteCount.current === 1) {
      setWriting(true)
      onWritePendingChange(true)
    }
    try {
      await operation()
    } finally {
      pendingWriteCount.current -= 1
      if (pendingWriteCount.current === 0) {
        setWriting(false)
        onWritePendingChange(false)
      }
    }
  }
  const resetEditor = () => setEditor((current) => ({
    mode: 'create',
    revision: current.mode === 'create' ? current.revision + 1 : 0,
  }))
  const publishStatus = (status?: ServiceEditorStatus) => {
    setEditorStatus(status)
    onStatusChange(status)
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
          editingStepUuid={editor.mode === 'edit' ? editor.step.uuid : undefined}
          initialValues={stepInitialValues(editor.mode === 'edit' ? editor.step : steps[0])}
          savedSteps={steps}
          saving={saveMutation.isPending}
          batchSaving={applyMutation.isPending}
          writePending={writePending}
          selectedRollCount={selectedRolls.length}
          getTargetAnalysis={(stepType) => resolveServiceApplyTargets({
            rolls: selectedRolls,
            stepType,
            steps: allSteps,
          })}
          onCancel={resetEditor}
          onSave={(values, stepUuid) => runWrite(
            () => saveMutation.mutateAsync({ values, stepUuid }),
          )}
          onSaveToSelected={(values, scope) => runWrite(
            () => applyMutation.mutateAsync({ values, scope }),
          )}
          onStatusChange={publishStatus}
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
                  runSavedStepAction(editorStatus, writePending, () => {
                    publishStatus(undefined)
                    setEditor({ mode: 'edit', step })
                  })
                }}
                onDelete={() => runSavedStepAction(
                  editorStatus, writePending,
                  () => runWrite(() => deleteMutation.mutateAsync(step.uuid)).catch(() => undefined),
                )} disabled={writePending} />
            ))}
          </div>
        ) : (
          <div className="draft-service-processes__empty">当前卷暂无附加工艺</div>
        )}
      </ServiceStepsLoadGate>
    </section>
  )
}

function runSavedStepAction(
  status: ServiceEditorStatus | undefined,
  writePending: boolean,
  action: () => void,
) {
  if (writePending) {
    message.info('附加工艺正在保存，请稍候')
    return
  }
  const reason = serviceEditorActionBlockedReason(status)
  if (reason) {
    message.warning(reason)
    return
  }
  action()
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
