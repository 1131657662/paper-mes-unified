import { Alert, Button, Form, Typography } from 'antd'
import { createPortal } from 'react-dom'
import type { ProcessStepDTO } from '../../../api/processOrder'
import ProcessStepFormFields from '../../../components/processOrder/ProcessStepFormFields'
import {
  processStepInitialValues,
  type ProcessStepFormValues,
} from '../../../components/processOrder/processStepCatalogModel'
import { useProcessStepFormState } from '../../../components/processOrder/useProcessStepFormState'
import type { CustomerProcessPrice } from '../../../types/customer'
import type { ProcessStep } from '../../../types/processOrder'
import type {
  FixedAmountScope,
  ServiceApplyTargets,
} from '../serviceStepBatchModel'
import type { MachineContext } from '../machineDefaults'
import type { ServiceEditorStatus } from '../serviceStepEditorTypes'
import {
  servicePricingSummary,
  serviceStepMatchesDraft,
} from '../serviceStepPresentation'
import ServiceStepEditorActions from './ServiceStepEditorActions'
import './DraftServiceStepEditor.css'

interface Props {
  customerPrices?: CustomerProcessPrice[]
  footerContainer?: HTMLElement | null
  initialValues?: ProcessStepDTO & { uuid?: string }
  roll: { uuid: string; rollName: string; machineContext?: MachineContext }
  savedSteps: ProcessStep[]
  saving: boolean
  batchSaving: boolean
  selectedRollCount: number
  getTargetAnalysis: (stepType?: number) => ServiceApplyTargets
  onCancel: () => void
  onSave: (values: ProcessStepDTO, stepUuid?: string) => Promise<void>
  onSaveToSelected: (values: ProcessStepDTO, scope: FixedAmountScope) => Promise<void>
  onStatusChange: (status?: ServiceEditorStatus) => void
}

export default function DraftServiceStepEditor(props: Props) {
  const reset = () => {
    props.onStatusChange(undefined)
    props.onCancel()
  }
  const state = useProcessStepFormState({
    initialValues: props.initialValues,
    defaultOriginalUuid: props.roll.uuid,
    extraOnly: true,
    customerPrices: props.customerPrices,
    machineContext: props.roll.machineContext,
    onCancel: reset,
    onOk: props.onSave,
  })
  const values = Form.useWatch([], state.form) as ProcessStepFormValues | undefined
  const savedStep = findSavedStep(props.savedSteps, values?.stepType)
  const analysis = props.getTargetAnalysis(values?.stepType)
  const status = buildEditorStatus(values, savedStep, analysis)
  const formChange = (changed: Partial<ProcessStepFormValues>) => {
    state.change(changed)
    if (changed.stepType != null) {
      window.setTimeout(() => publishFormStatus(state.form, props), 0)
      return
    }
    const nextValues = state.form.getFieldsValue(true)
    const existing = findSavedStep(props.savedSteps, nextValues.stepType)
    props.onStatusChange(buildEditorStatus(nextValues, existing, props.getTargetAnalysis(nextValues.stepType)))
  }
  const saveCurrent = () => state.submitWith((payload) => props.onSave(payload, savedStep?.uuid))
  const applySelected = () => state.submitWith((payload) => props.onSaveToSelected(
    payload,
    state.form.getFieldValue('fixedAmountScope') ?? 'TOTAL',
  ))

  return (
    <div className="draft-service-editor">
      <EditorHeading rollName={props.roll.rollName} status={status} />
      <CatalogState state={state} />
      <ProcessStepFormFields
        state={state}
        originalRolls={[props.roll]}
        editMode={Boolean(savedStep)}
        extraOnly
        compact
        onValuesChange={formChange}
      />
      {props.footerContainer && createPortal(
        <ServiceStepEditorActions
          actions={{ onApply: applySelected, onReset: reset, onSave: saveCurrent }}
          state={{
            analysis,
            batchSaving: props.batchSaving,
            disabled: state.isLoading || !state.selectedCatalog,
            saving: props.saving,
            selectedRollCount: props.selectedRollCount,
            updatingCurrent: Boolean(savedStep),
          }}
        />,
        props.footerContainer,
      )}
    </div>
  )
}

function EditorHeading({ rollName, status }: { rollName: string; status?: ServiceEditorStatus }) {
  return (
    <div className="draft-service-editor__header">
      <div className="draft-service-editor__identity">
        <Typography.Text strong>配置附加工艺</Typography.Text>
        <Typography.Text type="secondary" ellipsis={{ tooltip: rollName }}>当前：{rollName}</Typography.Text>
      </div>
      {status?.dirty && (
        <div className="draft-service-editor__pending">
          <span>待应用</span>
          <strong>{status.previousSummary ? `${status.previousSummary} → ${status.summary}` : status.summary}</strong>
        </div>
      )}
    </div>
  )
}

function CatalogState({ state }: { state: ReturnType<typeof useProcessStepFormState> }) {
  if (state.isError) {
    return <Alert type="error" showIcon message="工艺目录加载失败"
      action={<Button size="small" onClick={() => state.refetch()}>重试</Button>} />
  }
  if (!state.isLoading && state.catalogs?.length === 0) {
    return <Alert type="warning" showIcon message="暂无启用的附加工艺" />
  }
  return null
}

function loadSavedStep(
  form: ReturnType<typeof useProcessStepFormState>['form'],
  steps: ProcessStep[],
  stepType: number,
  originalUuid: string,
) {
  const saved = findSavedStep(steps, stepType)
  if (!saved) return
  const initial = processStepInitialValues({
    initialValues: { ...saved, originalUuid, stepType },
  })
  form.setFieldsValue({
    ...initial,
    unitPrice: saved.unitPrice ?? saved.billingUnitPrice,
    billingMode: saved.billingMode,
  })
}

function publishFormStatus(
  form: ReturnType<typeof useProcessStepFormState>['form'],
  props: Props,
) {
  const values = form.getFieldsValue(true)
  loadSavedStep(form, props.savedSteps, values.stepType, props.roll.uuid)
  const nextValues = form.getFieldsValue(true)
  const saved = findSavedStep(props.savedSteps, nextValues.stepType)
  props.onStatusChange(buildEditorStatus(nextValues, saved, props.getTargetAnalysis(nextValues.stepType)))
}

function findSavedStep(steps: ProcessStep[], stepType?: number) {
  if (stepType == null) return undefined
  return steps.find((step) => Number(step.stepType) === Number(stepType))
}

function buildEditorStatus(
  values: ProcessStepFormValues | undefined,
  saved: ProcessStep | undefined,
  analysis: ServiceApplyTargets,
): ServiceEditorStatus | undefined {
  if (!values?.stepType) return undefined
  return {
    analysis,
    dirty: !serviceStepMatchesDraft(values, saved),
    previousSummary: saved ? servicePricingSummary(saved) : undefined,
    summary: servicePricingSummary(values),
  }
}
