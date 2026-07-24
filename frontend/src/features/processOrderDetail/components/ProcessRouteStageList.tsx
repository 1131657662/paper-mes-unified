import { Button, Collapse, Descriptions, Empty, Segmented, Space, Table, Tag, Typography } from 'antd'
import { DeleteOutlined } from '@ant-design/icons'
import type { Dispatch, SetStateAction } from 'react'
import type { Machine } from '../../../types/machine'
import type { ProcessPlanDTO } from '../../../types/processOrder'
import type { RollDraft } from '../../processOrderCreate/types'
import RewindPlanEditor from '../../processOrderCreate/components/RewindPlanEditor'
import SawPlanEditor from '../../processOrderCreate/components/SawPlanEditor'
import ProcessMachineSelect from '../../processOrderCreate/components/ProcessMachineSelect'
import {
  STEP_TYPE_REWIND,
  STEP_TYPE_SAW,
  changeDetailRouteStageInputs,
  changeDetailRouteStageType,
  inputRowsForStage,
  removeLastDetailRouteStage,
  routeStepName,
  selectedInputRowsForStage,
  sourceRollFromOutput,
  stageOutputRows,
  updateDetailRouteStagePlan,
  type DetailRouteFormState,
  type DetailRoutePriceDefaults,
  type DetailRouteStageForm,
} from '../routeConfigDetail'
import RouteSourcePicker from './RouteSourcePicker'
import { withStageMachine } from '../processRouteConfigMachine'
import {
  formatWeight,
  outputColumns,
  sourceNoText,
  sourcePaperText,
  sourceSpecText,
  sourceStageText,
  totalSourceWeight,
} from './processRouteConfigPresentation'

interface Props {
  appendMode: boolean
  form: DetailRouteFormState
  machines: Machine[]
  prices: DetailRoutePriceDefaults
  setForm: Dispatch<SetStateAction<DetailRouteFormState | undefined>>
}

export default function ProcessRouteStageList(props: Props) {
  if (!props.form.stages.length) return <Empty
    description={props.appendMode ? '还没有配置追加工艺' : '暂无后续工艺，保存后仅保留首道产物'} />
  return <Collapse key={props.form.stages.map((stage) => stage.id).join('|')}
    className="process-route-config__stage-collapse" defaultActiveKey={lastStageKeys(props.form)}
    items={props.form.stages.map((stage, index) => ({
      key: stage.id,
      label: stageLabel(props.form, stage),
      extra: index === props.form.stages.length - 1
        ? <DeleteStage onDelete={() => props.setForm((prev) => prev && removeLastDetailRouteStage(prev))} /> : null,
      children: <StageEditor {...props} stage={stage} />,
    }))} />
}

function StageEditor({ form, machines, prices, setForm, stage }: Props & { stage: DetailRouteStageForm }) {
  const inputs = inputRowsForStage(form, stage.id)
  const sources = selectedInputRowsForStage(form, stage)
  const source = sources[0] ?? inputs[0]
  if (!source) return <Empty description="暂无可选择的来源产物" />
  const sourceRolls = (sources.length ? sources : [source]).map(sourceRollFromOutput)
  const sourceRoll = mergeSourceRolls(sourceRolls)
  if (!sourceRoll) return <Empty description="暂无可用的来源母卷" />
  const updatePlan = (plan: ProcessPlanDTO) => setForm((prev) => (
    prev && updateDetailRouteStagePlan(prev, stage.id, plan)
  ))
  return <Space direction="vertical" size={12} className="process-route-config">
    <RouteSourcePicker inputs={inputs} selectedKeys={stage.inputOutputKeys}
      onChange={(value) => setForm((prev) => prev && changeDetailRouteStageInputs(prev, stage.id, value, prices))} />
    <StageTypeControl machines={machines} prices={prices} setForm={setForm} stage={stage} />
    <ProcessMachineSelect machines={machines} mainStepType={stage.stepType}
      diameter={sourceRoll.originalDiameter} width={sourceRoll.originalWidth}
      weight={Number(sourceRoll.rollWeight ?? 0) * Number(sourceRoll.pieceNum ?? 1)}
      value={stage.plan.machineUuid} onChange={(machineUuid) => updatePlan({ ...stage.plan, machineUuid })} />
    <SourceSummary rows={sources.length ? sources : [source]} />
    {stage.stepType === STEP_TYPE_REWIND
      ? <RewindPlanEditor plan={stage.plan} roll={sourceRoll} rolls={sourceRolls} onChange={updatePlan} />
      : <SawPlanEditor plan={stage.plan} roll={sourceRoll} onChange={updatePlan} />}
    <Table size="small" rowKey="outputKey" pagination={false} columns={outputColumns}
      dataSource={stageOutputRows(form, stage)} scroll={{ x: 760 }} />
  </Space>
}

function StageTypeControl(props: Pick<Props, 'machines' | 'prices' | 'setForm'> & {
  stage: DetailRouteStageForm
}) {
  const { machines, prices, setForm, stage } = props
  return <Space wrap className="process-route-config__stage-head">
    <Typography.Text strong>本段工艺</Typography.Text>
    <Segmented aria-label={`第 ${stage.stageLevel} 段工艺类型`} value={stage.stepType}
      options={[{ label: '锯纸', value: STEP_TYPE_SAW }, { label: '复卷', value: STEP_TYPE_REWIND }]}
      onChange={(value) => setForm((prev) => prev && withStageMachine(
        changeDetailRouteStageType(prev, stage.id, Number(value), prices), stage.id, machines))} />
    <Tag color="blue">{routeStepName(stage.stepType)}</Tag>
  </Space>
}

function SourceSummary({ rows }: { rows: ReturnType<typeof stageOutputRows> }) {
  const row = rows[0]
  if (!row) return null
  return <Descriptions size="small" column={4} bordered className="process-route-config__source-summary">
    <Descriptions.Item label="来源数量">{rows.length} 件</Descriptions.Item>
    <Descriptions.Item label="来源阶段">{sourceStageText(rows)}</Descriptions.Item>
    <Descriptions.Item label="来源编号">{sourceNoText(rows)}</Descriptions.Item>
    <Descriptions.Item label="品名">{sourcePaperText(rows)}</Descriptions.Item>
    <Descriptions.Item label="规格">{sourceSpecText(rows)}</Descriptions.Item>
    <Descriptions.Item label="直径">{row.finishDiameter ?? '-'}</Descriptions.Item>
    <Descriptions.Item label="纸芯">{row.finishCoreDiameter ?? '-'}</Descriptions.Item>
    <Descriptions.Item label="预估重量">{formatWeight(totalSourceWeight(rows))}</Descriptions.Item>
    <Descriptions.Item label="产物编号">{rows.map((item) => item.outputKey).join('、')}</Descriptions.Item>
  </Descriptions>
}

function DeleteStage({ onDelete }: { onDelete: () => void }) {
  return <Button danger size="small" icon={<DeleteOutlined />} onClick={(event) => {
    event.stopPropagation(); onDelete()
  }}>删除本段</Button>
}

function stageLabel(form: DetailRouteFormState, stage: DetailRouteStageForm) {
  const sources = selectedInputRowsForStage(form, stage)
  return <Space wrap size={8} className="process-route-config__stage-label">
    <Typography.Text strong>第{stage.stageLevel}段工艺</Typography.Text>
    <Tag color="blue">{routeStepName(stage.stepType)}</Tag>
    <Typography.Text type="secondary">{sourceNoText(sources)} → {stageOutputRows(form, stage).length} 件产物</Typography.Text>
  </Space>
}

function lastStageKeys(form: DetailRouteFormState): string[] {
  const last = form.stages.at(-1)
  return last ? [last.id] : []
}

function mergeSourceRolls(rolls: RollDraft[]): RollDraft | undefined {
  const [first] = rolls
  if (!first || rolls.length <= 1) return first
  return { ...first, localId: rolls.map((roll) => roll.localId).join('+'),
    uuid: rolls.map((roll) => roll.uuid).join('+'), rollWeight: rolls.reduce((sum, roll) => (
      sum + Number(roll.rollWeight ?? 0) * Number(roll.pieceNum ?? 1)
    ), 0) }
}
