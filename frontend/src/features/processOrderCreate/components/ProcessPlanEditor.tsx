import { EditOutlined } from '@ant-design/icons'
import { Button, Input, InputNumber, Space, Tag, Typography } from 'antd'
import { PROCESS_MODE, STEP_TYPE, processModeRequiresMain } from '../../../constants/processOrder'
import type { Machine } from '../../../types/machine'
import type { ProcessPlanDTO } from '../../../types/processOrder'
import { formatGram, formatMm, formatOptionalKg } from '../../../utils/numberFormatters'
import type { RollDraft } from '../types'
import OnSiteCountEditor from './OnSiteCountEditor'
import ProcessMachineSelect from './ProcessMachineSelect'
import RewindPlanEditor from './RewindPlanEditor'
import SawPlanEditor from './SawPlanEditor'

interface Props {
  machines: Machine[]
  onEditMode?: () => void
  plan: ProcessPlanDTO
  roll: RollDraft
  rolls: RollDraft[]
  onChange: (plan: ProcessPlanDTO) => void
}

export default function ProcessPlanEditor({
  machines,
  onEditMode,
  plan,
  roll,
  rolls,
  onChange,
}: Props) {
  const processMode = plan.processMode ?? roll.processMode ?? 1
  const mainStepType = processModeRequiresMain(processMode)
    ? plan.mainStepType ?? roll.mainStepType ?? 2
    : undefined
  const patch = (partial: Partial<ProcessPlanDTO>) => onChange({ ...plan, ...partial })

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <RollContextHeader roll={roll} />
      <div className="process-plan-mode-context">
        <Typography.Text strong>加工方式</Typography.Text>
        <Tag color="blue">{PROCESS_MODE[processMode] ?? '标准加工'}</Tag>
        {mainStepType && <><Typography.Text strong>主工艺</Typography.Text>
          <Tag color="green">{STEP_TYPE[mainStepType]}</Tag></>}
        {onEditMode && (
          <Button type="link" icon={<EditOutlined />} onClick={onEditMode}>返回加工方式修改</Button>
        )}
      </div>
      {processModeRequiresMain(processMode) && (
        <ProcessMachineSelect
          machines={machines}
          mainStepType={mainStepType}
          diameter={roll.originalDiameter}
          width={roll.originalWidth}
          weight={roll.rollWeight}
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
  const weight = roll.weightStatus === 'UNKNOWN' ? undefined : Number(roll.rollWeight ?? 0) * (roll.pieceNum ?? 1)
  return (
    <div className="process-plan-context">
      <Typography.Text strong className="process-plan-context__spec">
        {roll.paperName || '未命名品名'} / {formatGram(roll.gramWeight)} / {formatMm(roll.originalWidth)} / {formatOptionalKg(weight)}
      </Typography.Text>
      <Typography.Text type="secondary" className="process-plan-context__identity">
        卷号：{roll.rollNo || '-'} / 编号：{roll.extraNo || '-'}
      </Typography.Text>
    </div>
  )
}
