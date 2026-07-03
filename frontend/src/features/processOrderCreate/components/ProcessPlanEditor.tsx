import { InputNumber, Radio, Select, Space, Typography } from 'antd'
import { PROCESS_MODE, STEP_TYPE } from '../../../constants/processOrder'
import type { Machine } from '../../../types/machine'
import type { ProcessPlanDTO } from '../../../types/processOrder'
import type { RollDraft } from '../types'
import { applyDefaultMachineToPlan } from '../machineDefaults'
import { applyLegacyPlanPriceDefaults, defaultPlanForRoll, type DefaultPlanOptions } from '../draftMappers'
import { toOnSitePlan } from '../onSitePlanUtils'
import OnSiteCountEditor from './OnSiteCountEditor'
import ProcessMachineSelect from './ProcessMachineSelect'
import RewindPlanEditor from './RewindPlanEditor'
import SawPlanEditor from './SawPlanEditor'

interface Props {
  defaultSpareCount?: number
  defaultPlanOptions?: DefaultPlanOptions
  machines: Machine[]
  plan: ProcessPlanDTO
  roll: RollDraft
  rolls: RollDraft[]
  onChange: (plan: ProcessPlanDTO) => void
}

const processOptions = Object.entries(PROCESS_MODE).map(([value, label]) => ({ value: Number(value), label }))
const stepOptions = Object.entries(STEP_TYPE).map(([value, label]) => ({ value: Number(value), label }))

export default function ProcessPlanEditor({
  defaultSpareCount = 0,
  defaultPlanOptions,
  machines,
  plan,
  roll,
  rolls,
  onChange,
}: Props) {
  const processMode = plan.processMode ?? roll.processMode ?? 1
  const mainStepType = processMode === 3 ? undefined : plan.mainStepType ?? roll.mainStepType ?? 2
  const planDefaults = defaultPlanOptions ?? { spareCount: defaultSpareCount }
  const patch = (partial: Partial<ProcessPlanDTO>) => onChange({ ...plan, ...partial })
  const patchMode = (nextMode: number) => {
    const nextStepType = nextMode === 3 ? undefined : mainStepType ?? 2
    onChange(planForMode({ defaultPlanOptions: planDefaults, machines, plan, roll, processMode: nextMode, mainStepType: nextStepType }))
  }
  const patchMainStep = (nextStepType: number) => {
    onChange(planForMode({ defaultPlanOptions: planDefaults, machines, plan, roll, processMode, mainStepType: nextStepType }))
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Typography.Text strong>{roll.rollNo || roll.paperName || '未命名母卷'}</Typography.Text>
      <Space wrap>
        <Typography.Text strong>加工方式</Typography.Text>
        <Select
          value={processMode}
          options={processOptions}
          style={{ width: 160 }}
          onChange={patchMode}
        />
        {processMode !== 3 && (
          <Radio.Group
            value={mainStepType}
            options={stepOptions}
            optionType="button"
            buttonStyle="solid"
            onChange={(event) => patchMainStep(event.target.value)}
          />
        )}
      </Space>
      {processMode !== 3 && (
        <ProcessMachineSelect
          machines={machines}
          mainStepType={mainStepType}
          value={plan.machineUuid}
          onChange={(machineUuid) => patch({ machineUuid })}
        />
      )}
      {processMode !== 3 && (
        <Space wrap>
          <InputNumber addonBefore="备用号" min={0} value={plan.spareCount ?? 0} onChange={(value) => patch({ spareCount: value ?? 0 })} />
          <InputNumber addonBefore="单价" min={0} precision={2} value={plan.unitPrice} onChange={(value) => patch({ unitPrice: value ?? undefined })} />
        </Space>
      )}
      {processMode === 3 && (
        <Typography.Text type="secondary">直发卷无需配置工艺，最终预览中会保留该母卷。</Typography.Text>
      )}
      {processMode === 2 && mainStepType && <OnSiteCountEditor plan={planForMode({ defaultPlanOptions: planDefaults, machines, plan, roll, processMode, mainStepType })} onChange={onChange} />}
      {processMode === 1 && mainStepType === 1 && <SawPlanEditor plan={plan} roll={roll} onChange={onChange} />}
      {processMode === 1 && mainStepType === 2 && <RewindPlanEditor plan={plan} roll={roll} rolls={rolls} onChange={onChange} />}
    </Space>
  )
}

function planForMode({ defaultPlanOptions = {}, machines, plan, roll, processMode, mainStepType }: PlanModeOptions): ProcessPlanDTO {
  if (processMode === 3) return { processMode: 3, spareCount: 0, finishSpecs: [] }
  const stepChanged = plan.mainStepType !== mainStepType || plan.processMode !== processMode
  if (processMode === 2) {
    const standardRoll = { ...roll, processMode, mainStepType }
    const fallback = defaultPlanForRoll(standardRoll, { ...defaultPlanOptions, spareCount: plan.spareCount ?? defaultPlanOptions.spareCount })
    const nextPlan = stepChanged
      ? fallback
      : applyLegacyPlanPriceDefaults({ ...plan, processMode, mainStepType }, defaultPlanOptions)
    return applyDefaultMachineToPlan(toOnSitePlan(nextPlan), machines)
  }
  const standardRoll = { ...roll, processMode, mainStepType }
  const fallback = defaultPlanForRoll(standardRoll, { ...defaultPlanOptions, spareCount: plan.spareCount ?? defaultPlanOptions.spareCount })
  const hasOnSiteWidth = plan.finishSpecs?.some((spec) => Number(spec.finishWidth ?? 0) <= 0)
  return applyDefaultMachineToPlan(hasOnSiteWidth || stepChanged ? fallback : { ...plan, processMode, mainStepType }, machines)
}

interface PlanModeOptions {
  defaultPlanOptions?: DefaultPlanOptions
  machines: Machine[]
  plan: ProcessPlanDTO
  roll: RollDraft
  processMode: number
  mainStepType?: number
}
