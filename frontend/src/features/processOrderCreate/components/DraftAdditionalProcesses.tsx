import { Typography, message } from 'antd'
import { useState } from 'react'
import type { ProcessStepDTO } from '../../../api/processOrder'
import type { CustomerProcessPrice } from '../../../types/customer'
import type { ProcessStep } from '../../../types/processOrder'
import { formatGram, formatKg, formatMm } from '../../../utils/numberFormatters'
import { useDraftServiceStepWrites } from '../hooks/useDraftServiceStepWrites'
import { serviceEditorActionBlockedReason } from '../serviceEditorGuard'
import type { RollDraft } from '../types'
import { rollTotalWeight } from '../../processOrderDetail/routeConfigSource'
import {
  resolveServiceApplyTargets,
  serviceStepsForRoll,
  serviceStepTemplate,
} from '../serviceStepBatchModel'
import DraftServiceStepEditor from './DraftServiceStepEditor'
import type { ServiceEditorStatus } from '../serviceStepEditorTypes'
import ServiceStepsLoadGate from './ServiceStepsLoadGate'
import SavedServiceStepList from './SavedServiceStepList'
import type { ProcessAiPackagingDraft } from '../../processAi/types'
import AiPackagingCandidateNotice from './AiPackagingCandidateNotice'

interface Props {
  aiPackagingDraft?: ProcessAiPackagingDraft
  allSteps: ProcessStep[]
  orderUuid?: string
  roll?: RollDraft
  selectedRolls?: RollDraft[]
  customerPrices?: CustomerProcessPrice[]
  detailError: boolean
  detailLoading: boolean
  draftVersion: number
  onRetryDetail: () => void
  onAiPackagingDraftConsumed?: (originalUuid: string) => void
  onAiPackagingDraftDismissed?: (draft: ProcessAiPackagingDraft) => Promise<void>
  onBatchApplied: () => void
  onCurrentSaved: () => void
  onStatusChange: (status?: ServiceEditorStatus) => void
  onSynchronizeVersion: () => Promise<number>
  onVersionSyncBlockedChange: (blocked: boolean) => void
  onWritePendingChange: (pending: boolean) => void
  versionSyncBlocked: boolean
}

export default function DraftAdditionalProcesses({
  aiPackagingDraft, allSteps, orderUuid, roll, selectedRolls = [], customerPrices,
  detailError, detailLoading, draftVersion, onRetryDetail, onStatusChange,
  onAiPackagingDraftConsumed, onAiPackagingDraftDismissed, onBatchApplied,
  onCurrentSaved,
  onSynchronizeVersion, onVersionSyncBlockedChange, onWritePendingChange, versionSyncBlocked,
}: Props) {
  const [editor, setEditor] = useState<EditorState>({ mode: 'create', revision: 0 })
  const [editorStatus, setEditorStatus] = useState<ServiceEditorStatus>()
  const steps = serviceStepsForRoll(allSteps, roll?.uuid)
  const writes = useDraftServiceStepWrites({
    allSteps,
    draftVersion,
    onSynchronizeVersion,
    onVersionSyncBlockedChange,
    onWritePendingChange,
    orderUuid,
    selectedRolls,
    steps,
    versionSyncBlocked,
  })
  const consumeAiDraft = () => {
    if (aiPackagingDraft) onAiPackagingDraftConsumed?.(aiPackagingDraft.values.originalUuid)
  }
  const resetEditor = () => {
    setEditor((current) => ({
      mode: 'create',
      revision: current.mode === 'create' ? current.revision + 1 : 0,
    }))
  }
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
      {aiPackagingDraft && <AiPackagingCandidateNotice draft={aiPackagingDraft}
        onDismiss={onAiPackagingDraftDismissed} />}
      <ServiceStepsLoadGate isError={detailError} isLoading={detailLoading} onRetry={onRetryDetail}>
        {roll?.uuid && <DraftServiceStepEditor
          key={`${editorKey(roll.uuid, editor)}:${steps[0]?.uuid ?? 'none'}:${aiPackagingDraft?.parseId ?? 'manual'}`}
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
          initialValues={editor.mode === 'edit'
            ? stepInitialValues(editor.step)
            : aiPackagingDraft?.values ?? stepInitialValues(steps[0])}
          savedSteps={steps}
          saving={writes.save.isPending}
          batchSaving={writes.apply.isPending}
          writePending={writes.writePending}
          selectedRollCount={selectedRolls.length}
          getTargetAnalysis={(stepType) => resolveServiceApplyTargets({
            rolls: selectedRolls,
            stepType,
            steps: allSteps,
          })}
          onCancel={resetEditor}
          onSave={(values, stepUuid) => writes.run(async () => {
            await writes.save.mutateAsync({ values: withAiCandidateSource(values, aiPackagingDraft), stepUuid })
            consumeAiDraft()
            publishStatus(undefined)
            onCurrentSaved()
          })}
          onSaveToSelected={(values, scope) => writes.run(async () => {
            await writes.apply.mutateAsync({ values: withAiCandidateSource(values, aiPackagingDraft), scope })
            consumeAiDraft()
            onBatchApplied()
          })}
          onStatusChange={publishStatus}
        />}
        <SavedServiceStepList
          disabled={writes.writePending}
          steps={steps}
          onEdit={(step) => runSavedStepAction(editorStatus, writes.writePending, () => {
            publishStatus(undefined)
            setEditor({ mode: 'edit', step })
          })}
          onDelete={(step) => runSavedStepAction(
            editorStatus,
            writes.writePending,
            () => writes.run(() => writes.remove.mutateAsync(step.uuid)).catch(() => undefined),
          )}
        />
      </ServiceStepsLoadGate>
    </section>
  )
}

function withAiCandidateSource(values: ProcessStepDTO, draft?: ProcessAiPackagingDraft): ProcessStepDTO {
  if (!draft) return values
  return { ...values, aiParseId: draft.parseId, aiOwnerRollRef: draft.ownerRollRef }
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
  return rollTotalWeight(roll)
}

function stepInitialValues(step?: ProcessStep): (ProcessStepDTO & { uuid?: string }) | undefined {
  const template = step && serviceStepTemplate(step)
  return template && step ? { ...template, uuid: step.uuid } : undefined
}
