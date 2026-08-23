import { Typography, message } from 'antd'
import { useState } from 'react'
import type { ProcessStepDTO } from '../../../api/processOrder'
import type { CustomerProcessPrice } from '../../../types/customer'
import type { ProcessStep } from '../../../types/processOrder'
import type { RollDraft } from '../types'
import { rollTotalWeight } from '../../processOrderDetail/routeConfigSource'
import {
  buildServiceStepBatch,
  resolveServiceApplyTargets,
  serviceStepTemplate,
} from '../serviceStepBatchModel'
import type { FixedAmountScope } from '../serviceStepBatchModel'
import type { ServiceEditorStatus } from '../serviceStepEditorTypes'
import DraftServiceStepEditor from './DraftServiceStepEditor'
import SavedServiceStepList from './SavedServiceStepList'

interface Props {
  allSteps: ProcessStep[]
  customerPrices?: CustomerProcessPrice[]
  roll: RollDraft
  rolls: RollDraft[]
  selectedRolls: RollDraft[]
  onPersist: (changes: Record<string, ProcessStep[]>) => Promise<void>
  onBatchApplied: () => void
  onCurrentSaved: () => void
  onStatusChange: (status?: ServiceEditorStatus) => void
}

type EditorState = { mode: 'create'; revision: number } | { mode: 'edit'; step: ProcessStep }

export default function AppendServiceStepEditor(props: Props) {
  const [editor, setEditor] = useState<EditorState>({ mode: 'create', revision: 0 })
  const [status, setStatus] = useState<ServiceEditorStatus>()
  const [pending, setPending] = useState(false)
  const steps = props.roll.serviceSteps ?? []
  const reset = () => {
    setStatus(undefined)
    props.onStatusChange(undefined)
    setEditor((current) => ({
      mode: 'create',
      revision: current.mode === 'create' ? current.revision + 1 : 0,
    }))
  }
  const publish = (next?: ServiceEditorStatus) => {
    setStatus(next)
    props.onStatusChange(next)
  }
  const persist = async (changes: Record<string, ProcessStep[]>, after: () => void) => {
    setPending(true)
    try {
      await props.onPersist(changes)
      publish(undefined)
      after()
    } finally {
      setPending(false)
    }
  }
  const save = async (values: ProcessStepDTO, stepUuid?: string) => {
    const next = upsertStep(steps, values, props.roll.uuid!, stepUuid)
    await persist({ [props.roll.localId]: next }, props.onCurrentSaved)
  }
  const applyToSelected = async (values: ProcessStepDTO, scope: FixedAmountScope) => {
    const analysis = resolveServiceApplyTargets({
      rolls: props.selectedRolls,
      stepType: values.stepType,
      steps: props.allSteps,
    })
    const templates = buildServiceStepBatch(values, analysis.targetUuids, scope)
    const changes = Object.fromEntries(templates.map((template) => {
      const target = props.rolls.find((roll) => roll.uuid === template.originalUuid)
      if (!target) return [template.originalUuid, []]
      const current = target.serviceSteps ?? []
      return [target.localId, upsertStep(current, template, template.originalUuid)]
    }))
    await persist(changes, props.onBatchApplied)
  }
  const remove = async (step: ProcessStep) => {
    await persist({ [props.roll.localId]: steps.filter((item) => item.uuid !== step.uuid) }, reset)
  }
  const editingStepUuid = editor.mode === 'edit' ? editor.step.uuid : undefined
  const editingStep = editor.mode === 'edit' ? editor.step : steps[0]
  const analysis = (stepType?: number) => resolveServiceApplyTargets({
    rolls: props.selectedRolls,
    stepType,
    steps: props.allSteps,
  })

  return (
    <section className="draft-service-processes">
      <div className="draft-service-processes__header">
        <div>
          <Typography.Text strong>附加工艺</Typography.Text>
          <Typography.Text type="secondary">本次追加会话独立保存，不会修改原有母卷工艺</Typography.Text>
        </div>
      </div>
      <DraftServiceStepEditor
        key={`${props.roll.localId}:${editor.mode}:${editingStepUuid ?? (editor.mode === 'create' ? editor.revision : 'edit')}`}
        roll={{ uuid: props.roll.uuid!, rollName: rollLabel(props.roll), machineContext: {
          diameter: props.roll.originalDiameter,
          weight: totalWeight(props.roll),
          width: props.roll.originalWidth,
        }}}
        customerPrices={props.customerPrices}
        editingStepUuid={editingStepUuid}
        initialValues={stepInitialValues(editingStep)}
        savedSteps={steps}
        saving={pending}
        batchSaving={pending}
        writePending={pending}
        selectedRollCount={props.selectedRolls.length}
        getTargetAnalysis={analysis}
        onCancel={reset}
        onSave={save}
        onSaveToSelected={applyToSelected}
        onStatusChange={publish}
      />
      <SavedServiceStepList
        disabled={pending}
        steps={steps}
        onEdit={(step) => {
          if (status?.dirty) {
            message.warning('请先保存当前附加工艺修改')
            return
          }
          setEditor({ mode: 'edit', step })
        }}
        onDelete={(step) => {
          if (status?.dirty) {
            message.warning('请先保存当前附加工艺修改')
            return
          }
          void remove(step)
        }}
      />
    </section>
  )
}

function upsertStep(
  current: ProcessStep[], values: ProcessStepDTO, originalUuid: string, stepUuid?: string,
) {
  const match = stepUuid
    ? current.find((step) => step.uuid === stepUuid)
    : current.find((step) => step.stepType === values.stepType)
  const next: ProcessStep = {
    ...(match ?? {}),
    ...values,
    uuid: match?.uuid ?? stepUuid ?? crypto.randomUUID(),
    originalUuid,
    isMain: 0,
  }
  return [...current.filter((step) => step.uuid !== match?.uuid), next]
}

function stepInitialValues(step?: ProcessStep): (ProcessStepDTO & { uuid?: string }) | undefined {
  const template = step && serviceStepTemplate(step)
  return template && step ? { ...template, uuid: step.uuid } : undefined
}

function rollLabel(roll: RollDraft) {
  return `${roll.rollNo || '-'} / ${roll.extraNo || '-'} / ${roll.paperName || '-'}`
}

function totalWeight(roll: RollDraft) {
  return rollTotalWeight(roll)
}
