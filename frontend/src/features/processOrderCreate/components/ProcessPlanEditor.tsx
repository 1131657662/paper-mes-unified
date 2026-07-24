import { Input, InputNumber, Radio, Space, Tag, Typography } from 'antd'
import { PROCESS_MODE, STEP_TYPE, processModeRequiresMain } from '../../../constants/processOrder'
import type { Machine } from '../../../types/machine'
import type { ProcessPlanDTO } from '../../../types/processOrder'
import { formatGram, formatKg, formatMm } from '../../../utils/numberFormatters'
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

const stepOptions = Object.entries(STEP_TYPE)
  .filter(([value]) => value === '1' || value === '2')
  .map(([value, label]) => ({ value: Number(value), label }))

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
  const mainStepType = processModeRequiresMain(processMode)
    ? plan.mainStepType ?? roll.mainStepType ?? 2
    : undefined
  const planDefaults = defaultPlanOptions ?? { spareCount: defaultSpareCount }
  const patch = (partial: Partial<ProcessPlanDTO>) => onChange({ ...plan, ...partial })
  const patchMainStep = (nextStepType: number) => {
    onChange(planForMode({ defaultPlanOptions: planDefaults, machines, plan, roll, processMode, mainStepType: nextStepType }))
  }

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <RollContextHeader roll={roll} />
      <div className="process-plan-mode-context">
        <Typography.Text strong>加工方式</Typography.Text>
        <Tag color="blue">{PROCESS_MODE[processMode] ?? '标准加工'}</Tag>
      </div>
      {processModeRequiresMain(processMode) && (
        <Space wrap>
          <Typography.Text strong>主工艺</Typography.Text>
          <Radio.Group
            aria-label="主工艺"
            value={mainStepType}
            options={stepOptions}
            optionType="button"
            buttonStyle="solid"
            onChange={(event) => patchMainStep(event.target.value)}
          />
        </Space>
      )}
      {processModeRequiresMain(processMode) && (
        <ProcessMachineSelect
          machines={machines}
          mainStepType={mainStepType}
          diameter={roll.originalDiameter}
          width={roll.originalWidth}
          weight={Number(roll.rollWeight ?? 0)}
          value={plan.machineUuid}
          onChange={(machineUuid) => patch({ machineUuid })}
        />
      )}
      {processModeRequiresMain(processMode) && (
        <Space wrap>
          {processMode === 1 && (
            <Space.Compact>
              <Input aria-label="备用号字段" readOnly tabIndex={-1} value="备用号" style={{ width: 72 }} />
              <InputNumber aria-label="备用卷号数量" min={0} value={plan.spareCount ?? 0} style={{ width: 120 }} onChange={(value) => patch({ spareCount: value ?? 0 })} />
            </Space.Compact>
          )}
          <Space.Compact>
            <Input aria-label="单价字段" readOnly tabIndex={-1} value="单价" style={{ width: 64 }} />
            <InputNumber aria-label="加工单价" min={0} precision={2} value={plan.unitPrice} style={{ width: 140 }} onChange={(value) => patch({ unitPrice: value ?? undefined })} />
          </Space.Compact>
        </Space>
      )}
      {processMode === 3 && (
        <Typography.Text type="secondary">直发卷无需配置工艺，最终预览中会保留该母卷。</Typography.Text>
      )}
      {processMode === 4 && (
        <Typography.Text type="secondary">附加工艺已在上一步维护，提交后按母卷件数生成整理成品号。</Typography.Text>
      )}
      {processMode === 2 && mainStepType && <OnSiteCountEditor />}
      {processMode === 1 && mainStepType === 1 && <SawPlanEditor plan={plan} roll={roll} onChange={onChange} />}
      {processMode === 1 && mainStepType === 2 && <RewindPlanEditor plan={plan} roll={roll} rolls={rolls} onChange={onChange} />}
    </Space>
  )
}

function RollContextHeader({ roll }: { roll: RollDraft }) {
  const weight = Number(roll.rollWeight ?? 0) * (roll.pieceNum ?? 1)
  return (
    <div className="process-plan-context">
      <Typography.Text strong className="process-plan-context__spec">
        {roll.paperName || '未命名品名'} / {formatGram(roll.gramWeight)} / {formatMm(roll.originalWidth)} / {formatKg(weight)}
      </Typography.Text>
      <Typography.Text type="secondary" className="process-plan-context__identity">
        卷号：{roll.rollNo || '-'} / 编号：{roll.extraNo || '-'}
      </Typography.Text>
    </div>
  )
}

function planForMode({ defaultPlanOptions = {}, machines, plan, roll, processMode, mainStepType }: PlanModeOptions): ProcessPlanDTO {
  if (processMode === 3) return { processMode: 3, spareCount: 0, finishSpecs: [] }
  if (processMode === 4) return { processMode: 4, spareCount: 0, finishSpecs: [] }
  const stepChanged = plan.mainStepType !== mainStepType || plan.processMode !== processMode
  if (processMode === 2) {
    const standardRoll = { ...roll, processMode, mainStepType }
    const fallback = defaultPlanForRoll(standardRoll, { ...defaultPlanOptions, spareCount: plan.spareCount ?? defaultPlanOptions.spareCount })
    const nextPlan = stepChanged
      ? fallback
      : applyLegacyPlanPriceDefaults({ ...plan, processMode, mainStepType }, defaultPlanOptions)
    return applyDefaultMachineToPlan(toOnSitePlan(nextPlan), machines, roll)
  }
  const standardRoll = { ...roll, processMode, mainStepType }
  const fallback = defaultPlanForRoll(standardRoll, { ...defaultPlanOptions, spareCount: plan.spareCount ?? defaultPlanOptions.spareCount })
  const hasOnSiteWidth = plan.finishSpecs?.some((spec) => Number(spec.finishWidth ?? 0) <= 0)
  return applyDefaultMachineToPlan(hasOnSiteWidth || stepChanged ? fallback : { ...plan, processMode, mainStepType }, machines, roll)
}

interface PlanModeOptions {
  defaultPlanOptions?: DefaultPlanOptions
  machines: Machine[]
  plan: ProcessPlanDTO
  roll: RollDraft
  processMode: number
  mainStepType?: number
}
