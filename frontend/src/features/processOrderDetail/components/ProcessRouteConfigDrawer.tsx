import { Alert, Button, Card, Collapse, Descriptions, Drawer, Empty, Select, Segmented, Space, Table, Tag, Typography, message } from 'antd'
import { DeleteOutlined, PlusOutlined, SaveOutlined, SyncOutlined } from '@ant-design/icons'
import { useQuery } from '@tanstack/react-query'
import type { ColumnsType } from 'antd/es/table'
import { useState } from 'react'
import type { Dispatch, SetStateAction } from 'react'
import type { Customer } from '../../../types/customer'
import type {
  OriginalRoll,
  ProcessOrderDetailVO,
  ProcessPlanDTO,
  ProcessRouteOutputVO,
  ProcessRoutePreviewVO,
  ProcessRouteStageLineVO,
} from '../../../types/processOrder'
import RewindPlanEditor from '../../processOrderCreate/components/RewindPlanEditor'
import SawPlanEditor from '../../processOrderCreate/components/SawPlanEditor'
import { queries } from '../../../queries'
import { formatKg as formatWeightKg, formatTon } from '../../../utils/numberFormatters'
import { usePreviewPendingRoute } from '../hooks/usePreviewPendingRoute'
import { useSavePendingRoute } from '../hooks/useSavePendingRoute'
import { usePreviewAppendRoute } from '../hooks/usePreviewAppendRoute'
import { useSaveAppendRoute } from '../hooks/useSaveAppendRoute'
import { formatMoney } from '../orderDetailUtils'
import RouteSourcePicker from './RouteSourcePicker'
import {
  STEP_TYPE_REWIND,
  STEP_TYPE_SAW,
  addDetailRouteStage,
  buildAppendRouteDto,
  buildDetailRouteDto,
  changeDetailRouteStageInputs,
  changeDetailRouteStageType,
  finalDetailRouteOutputs,
  initialDetailRouteForm,
  inputRowsForStage,
  removeLastDetailRouteStage,
  routeStepName,
  selectedInputRowsForStage,
  sourceRollFromOutput,
  stageOutputRows,
  updateDetailRouteStagePlan,
  type DetailRouteFormState,
  type DetailRouteOutputRow,
  type DetailRoutePriceDefaults,
  type DetailRouteStageForm,
} from '../routeConfigDetail'

interface Props {
  open: boolean
  detail?: ProcessOrderDetailVO
  mode?: 'replace' | 'append'
  initialOriginalUuid?: string
  initialOutputKey?: string
  onClose: () => void
}

const stepOptions = [
  { label: '锯纸', value: STEP_TYPE_SAW },
  { label: '复卷', value: STEP_TYPE_REWIND },
]

export default function ProcessRouteConfigDrawer({
  open,
  detail,
  mode = 'replace',
  initialOriginalUuid,
  initialOutputKey,
  onClose,
}: Props) {
  const appendMode = mode === 'append'
  const rolls = detail?.originalRolls ?? []
  const [selectedUuid, setSelectedUuid] = useState(() => initialOriginalUuid ?? rolls[0]?.uuid)
  const roll = rolls.find((item) => item.uuid === selectedUuid) ?? rolls[0]
  const { data: customerPage } = useQuery(queries.createOrder.customers)
  const prices = priceDefaults(detail, roll?.uuid, customerPage?.records ?? [])
  const [form, setForm] = useState<DetailRouteFormState | undefined>(() => roll && initialForm(
    detail,
    roll,
    prices,
    appendMode,
    initialOutputKey,
  ))
  const [preview, setPreview] = useState<ProcessRoutePreviewVO>()
  const { mutateAsync: previewRoute, isPending: isPreviewing } = usePreviewPendingRoute()
  const { mutateAsync: saveRoute, isPending: isSaving } = useSavePendingRoute()
  const { mutateAsync: previewAppendRoute, isPending: isPreviewingAppend } = usePreviewAppendRoute()
  const { mutateAsync: saveAppendRoute, isPending: isSavingAppend } = useSaveAppendRoute()

  const handleRollChange = (uuid: string) => {
    const nextRoll = rolls.find((item) => item.uuid === uuid)
    setSelectedUuid(uuid)
    setForm(nextRoll && initialForm(
      detail,
      nextRoll,
      priceDefaults(detail, nextRoll?.uuid, customerPage?.records ?? []),
      appendMode,
    ))
    setPreview(undefined)
  }

  const addStage = (stepType: number) => {
    if (!form) return
    setForm(addDetailRouteStage(form, stepType, prices))
    setPreview(undefined)
  }

  const handlePreview = async () => {
    if (!detail?.order.uuid || !roll || !form) return
    if (!requireRouteReady(form, appendMode)) return
    const request = appendMode ? buildAppendRouteDto(roll, form) : buildDetailRouteDto(roll, form)
    const action = appendMode ? previewAppendRoute : previewRoute
    setPreview(await action({ orderUuid: detail.order.uuid, request }))
  }

  const handleSave = async () => {
    if (!detail?.order.uuid || !roll || !form) return
    if (!requireRouteReady(form, appendMode)) return
    const request = appendMode ? buildAppendRouteDto(roll, form) : buildDetailRouteDto(roll, form)
    const action = appendMode ? saveAppendRoute : saveRoute
    await action({ orderUuid: detail.order.uuid, request })
    message.success(appendMode ? '已按所选产物追加工艺，成品方案和加工费已重算' : '整卷工艺已重配，工序、阶段产物和成品号已重建')
    onClose()
  }

  return (
    <Drawer
      title={appendMode ? '选择产物追加工艺' : '重配整卷工艺路线'}
      open={open}
      width={1180}
      onClose={onClose}
      destroyOnClose
      footer={<DrawerFooter appendMode={appendMode} loading={appendMode ? isSavingAppend : isSaving} onClose={onClose} onSave={handleSave} />}
    >
      {!roll || !form ? (
        <Empty description="暂无可配置的母卷" />
      ) : (
        <Space direction="vertical" size={14} className="process-route-config">
          <Alert
            type="info"
            showIcon
            message={appendMode
              ? '先选要继续加工的阶段产物，再配置下一段工艺；保存后只新增后续工序和新成品，不清理原有路线。'
              : '用于待下发单据的整卷工艺重配；保存后会按当前路线清理旧工序、旧阶段产物和旧预生成成品号，再重新生成。'}
          />
          <RouteBaseControls roll={roll} rolls={rolls} onRollChange={handleRollChange} />
          <RouteFirstOutputTable appendMode={appendMode} rows={form.firstOutputs} selectedOutputKey={initialOutputKey} />
          <RouteStageToolbar appendMode={appendMode} disabled={!finalDetailRouteOutputs(form).length} onAdd={addStage} />
          <RouteStageList appendMode={appendMode} form={form} prices={prices} setForm={setForm} />
          <Button icon={<SyncOutlined />} loading={appendMode ? isPreviewingAppend : isPreviewing} onClick={handlePreview}>
            预览费用与最终产出
          </Button>
          {preview ? <RoutePreviewResult preview={preview} /> : <RouteDraftSummary form={form} />}
        </Space>
      )}
    </Drawer>
  )
}

function RouteBaseControls({ roll, rolls, onRollChange }: BaseControlProps) {
  return (
    <Card size="small" title="来源母卷">
      <Space direction="vertical" size={10} className="process-route-config">
        <Select
          showSearch
          value={roll.uuid}
          optionFilterProp="label"
          options={rolls.map((item) => ({ value: item.uuid, label: rollLabel(item) }))}
          className="process-route-config__roll-select"
          onChange={onRollChange}
        />
        <Descriptions size="small" column={4} bordered className="process-route-config__source-summary">
          <Descriptions.Item label="卷号">{roll.rollNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="编号">{roll.extraNo || '-'}</Descriptions.Item>
          <Descriptions.Item label="品名">{roll.paperName || '-'}</Descriptions.Item>
          <Descriptions.Item label="规格">{roll.gramWeight ?? '-'}g / {roll.originalWidth ?? '-'}mm</Descriptions.Item>
          <Descriptions.Item label="直径/纸芯">{roll.originalDiameter ?? '-'} / {roll.coreDiameter ?? '-'}</Descriptions.Item>
          <Descriptions.Item label="件数">{roll.pieceNum ?? 1} 件</Descriptions.Item>
          <Descriptions.Item label="重量">{formatWeight(rollTotalWeight(roll))}</Descriptions.Item>
          <Descriptions.Item label="首道工艺">{routeStepName(roll.mainStepType)}</Descriptions.Item>
        </Descriptions>
      </Space>
    </Card>
  )
}

function RouteFirstOutputTable({
  appendMode,
  rows,
  selectedOutputKey,
}: {
  appendMode: boolean
  rows: DetailRouteOutputRow[]
  selectedOutputKey?: string
}) {
  return (
    <Card size="small" title={appendMode ? '可继续加工的当前产物' : '首道产物'}>
      <Table
        size="small"
        rowKey="outputKey"
        pagination={false}
        columns={outputColumns}
        dataSource={rows}
        rowClassName={(row) => isSelectedOutput(row, selectedOutputKey) ? 'process-route-config__selected-row' : ''}
        scroll={{ x: 760 }}
      />
    </Card>
  )
}

function RouteStageToolbar({ appendMode, disabled, onAdd }: { appendMode: boolean; disabled: boolean; onAdd: (stepType: number) => void }) {
  return (
    <Card size="small" className="process-route-config__stage-toolbar">
      <div className="process-route-config__toolbar-inner">
        <Typography.Text strong>{appendMode ? '给选中产物追加下一段' : '从首道产物设计下一段'}</Typography.Text>
        <Space wrap size={8}>
          <Button disabled={disabled} icon={<PlusOutlined />} onClick={() => onAdd(STEP_TYPE_SAW)}>
            锯纸
          </Button>
          <Button disabled={disabled} type="primary" icon={<PlusOutlined />} onClick={() => onAdd(STEP_TYPE_REWIND)}>
            复卷
          </Button>
        </Space>
        <Typography.Text type="secondary">{appendMode ? '下一段阶段号会按所选产物自动顺延' : '删除本段后会回到上游产物重新选择'}</Typography.Text>
      </div>
    </Card>
  )
}

function RouteStageList({ appendMode, form, prices, setForm }: StageListProps) {
  if (!form.stages.length) {
    return (
      <Empty
        description={appendMode ? '还没有配置追加工艺' : '暂无后续工艺，保存后仅保留首道产物'}
      />
    )
  }
  return (
    <Collapse
      key={form.stages.map((stage) => stage.id).join('|')}
      className="process-route-config__stage-collapse"
      defaultActiveKey={lastStageKeys(form)}
      items={form.stages.map((stage, index) => ({
        key: stage.id,
        label: routeStagePanelLabel(form, stage),
        extra: index === form.stages.length - 1
          ? <RouteStageDeleteButton onDelete={() => setForm((prev) => prev && removeLastDetailRouteStage(prev))} />
          : null,
        children: (
          <RouteStageEditor
            form={form}
            prices={prices}
            setForm={setForm}
            stage={stage}
          />
        ),
      }))}
    />
  )
}

function RouteStageEditor({ form, prices, setForm, stage }: StageEditorProps) {
  const inputs = inputRowsForStage(form, stage.id)
  const sources = selectedInputRowsForStage(form, stage)
  const source = sources[0] ?? inputs[0]
  const outputRows = stageOutputRows(form, stage)
  if (!source) return <Empty description="暂无可选择的来源产物" />
  const sourceRolls = sources.length ? sources.map(sourceRollFromOutput) : [sourceRollFromOutput(source)]
  const sourceRoll = mergeSourceRolls(sourceRolls)

  const updatePlan = (plan: ProcessPlanDTO) => {
    setForm((prev) => prev && updateDetailRouteStagePlan(prev, stage.id, plan))
  }

  return (
    <Space direction="vertical" size={12} className="process-route-config">
      <RouteSourcePicker
        inputs={inputs}
        selectedKeys={stage.inputOutputKeys}
        onChange={(value) => setForm((prev) => prev && changeDetailRouteStageInputs(prev, stage.id, value, prices))}
      />
      <Space wrap className="process-route-config__stage-head">
        <Typography.Text strong>本段工艺</Typography.Text>
        <Segmented
          value={stage.stepType}
          options={stepOptions}
          onChange={(value) => setForm((prev) => prev && changeDetailRouteStageType(prev, stage.id, Number(value), prices))}
        />
        <Tag color="blue">{routeStepName(stage.stepType)}</Tag>
      </Space>
      <RouteSourceSummary rows={sources.length ? sources : [source]} />
      {stage.stepType === STEP_TYPE_REWIND ? (
        <RewindPlanEditor plan={stage.plan} roll={sourceRoll} rolls={sourceRolls} onChange={updatePlan} />
      ) : (
        <SawPlanEditor plan={stage.plan} roll={sourceRoll} onChange={updatePlan} />
      )}
      <Table size="small" rowKey="outputKey" pagination={false} columns={outputColumns} dataSource={outputRows} scroll={{ x: 760 }} />
    </Space>
  )
}

function RouteStageDeleteButton({ onDelete }: { onDelete: () => void }) {
  return (
    <Button
      danger
      size="small"
      icon={<DeleteOutlined />}
      onClick={(event) => {
        event.stopPropagation()
        onDelete()
      }}
    >
      删除本段
    </Button>
  )
}

function routeStagePanelLabel(form: DetailRouteFormState, stage: DetailRouteStageForm) {
  const sources = selectedInputRowsForStage(form, stage)
  const outputs = stageOutputRows(form, stage)
  return (
    <Space wrap size={8} className="process-route-config__stage-label">
      <Typography.Text strong>第{stage.stageLevel}段工艺</Typography.Text>
      <Tag color="blue">{routeStepName(stage.stepType)}</Tag>
      <Typography.Text type="secondary">
        {sourceNoText(sources)} → {outputs.length} 件产物
      </Typography.Text>
    </Space>
  )
}

function lastStageKeys(form: DetailRouteFormState) {
  const last = form.stages.at(-1)
  return last ? [last.id] : []
}

function RouteSourceSummary({ rows }: { rows: DetailRouteOutputRow[] }) {
  const row = rows[0]
  if (!row) return null
  return (
    <Descriptions size="small" column={4} bordered className="process-route-config__source-summary">
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
  )
}

function RouteDraftSummary({ form }: { form: DetailRouteFormState }) {
  const finals = finalDetailRouteOutputs(form)
  return (
    <Descriptions size="small" column={2} bordered>
      <Descriptions.Item label="已配置后续阶段">{form.stages.length} 道</Descriptions.Item>
      <Descriptions.Item label="当前最终产物">{finals.length} 件</Descriptions.Item>
    </Descriptions>
  )
}

function RoutePreviewResult({ preview }: { preview: ProcessRoutePreviewVO }) {
  return (
    <Space direction="vertical" size={10} className="process-route-config">
      <Descriptions size="small" column={2} bordered>
        <Descriptions.Item label="链式工艺费用">{formatMoney(preview.totalAmount)}</Descriptions.Item>
        <Descriptions.Item label="最终产出">{(preview.outputs ?? []).filter((item) => !item.consumedByNextStage).length} 件</Descriptions.Item>
      </Descriptions>
      <Table size="small" rowKey={stageRowKey} pagination={false} columns={stageColumns} dataSource={preview.stages ?? []} scroll={{ x: 760 }} />
      <Table size="small" rowKey={(record) => record.outputKey ?? ''} pagination={false} columns={previewOutputColumns} dataSource={preview.outputs ?? []} scroll={{ x: 860 }} />
    </Space>
  )
}

function DrawerFooter({
  appendMode,
  loading,
  onClose,
  onSave,
}: {
  appendMode: boolean
  loading: boolean
  onClose: () => void
  onSave: () => void
}) {
  return (
    <Space className="process-route-config__footer">
      <Button onClick={onClose}>取消</Button>
      <Button type="primary" icon={<SaveOutlined />} loading={loading} onClick={onSave}>
        {appendMode ? '保存追加工艺' : '保存重配路线'}
      </Button>
    </Space>
  )
}

const outputColumns: ColumnsType<DetailRouteOutputRow> = [
  { title: '产物', dataIndex: 'label', width: 150, render: (value, row) => <Space size={4}>{value}<Tag>{row.outputKey}</Tag></Space> },
  { title: '来源编号', width: 180, render: (_, row) => row.finishRollNo || row.sourceRollNo || row.sourceOutputKey || '-' },
  { title: '品名', dataIndex: 'paperName', width: 140 },
  { title: '规格', width: 150, render: (_, row) => `${row.gramWeight ?? '-'}g / ${row.finishWidth ?? '-'}mm` },
  { title: '直径/纸芯', width: 130, render: (_, row) => `${row.finishDiameter ?? '-'} / ${row.finishCoreDiameter ?? '-'}` },
  { title: '预估重量', dataIndex: 'estimateWeight', width: 120, align: 'right', render: formatWeight },
]

const stageColumns: ColumnsType<ProcessRouteStageLineVO> = [
  { title: '阶段', dataIndex: 'stageLevel', width: 70, render: (value) => `第${value ?? '-'}段` },
  { title: '工艺', dataIndex: 'stepName', width: 90 },
  { title: '来源产物', dataIndex: 'inputOutputKeys', width: 130, render: (value) => Array.isArray(value) && value.length ? value.join('、') : '原卷' },
  { title: '刀数', dataIndex: 'knifeCount', width: 76, render: (value) => value ?? '-' },
  { title: '吨位', dataIndex: 'processWeight', width: 100, render: (value) => value == null ? '-' : formatTon(Number(value)) },
  { title: '单价', dataIndex: 'unitPrice', width: 96, render: formatMoney },
  { title: '费用', dataIndex: 'stepAmount', width: 96, render: formatMoney },
]

const previewOutputColumns: ColumnsType<ProcessRouteOutputVO> = [
  { title: '产出', dataIndex: 'outputKey', width: 90 },
  { title: '阶段', dataIndex: 'stageLevel', width: 70, render: (value) => `第${value ?? '-'}段` },
  { title: '状态', dataIndex: 'consumedByNextStage', width: 100, render: (value) => value ? <Tag color="orange">进入下道</Tag> : <Tag color="green">最终成品</Tag> },
  { title: '门幅', dataIndex: 'finishWidth', width: 90, render: (value) => value ? `${value}mm` : '-' },
  { title: '预估重', dataIndex: 'estimateWeight', width: 110, render: formatWeight },
  { title: '备注', dataIndex: 'remark', width: 180 },
]

function initialForm(
  detail: ProcessOrderDetailVO | undefined,
  roll: OriginalRoll,
  prices: DetailRoutePriceDefaults,
  appendMode: boolean,
  initialOutputKey?: string,
) {
  const production = productionFor(detail, roll.uuid)
  return initialDetailRouteForm(
    roll,
    production,
    prices,
    appendMode ? maxStageLevel(production) : 1,
    appendMode,
    initialOutputKey,
  )
}

function isSelectedOutput(row: DetailRouteOutputRow, selectedOutputKey?: string) {
  return Boolean(selectedOutputKey && (row.outputKey === selectedOutputKey || row.finishRollNo === selectedOutputKey))
}

function productionFor(detail: ProcessOrderDetailVO | undefined, originalUuid?: string) {
  return detail?.rollProductions?.find((item) => item.originalUuid === originalUuid)
}

function maxStageLevel(production: ReturnType<typeof productionFor>) {
  return Math.max(1, ...(production?.steps ?? []).map((step) => step.stageLevel ?? 1))
}

function priceDefaults(
  detail: ProcessOrderDetailVO | undefined,
  originalUuid: string | undefined,
  customers: Customer[],
): DetailRoutePriceDefaults {
  const rollSteps = (detail?.steps ?? []).filter((step) => step.originalUuid === originalUuid)
  const customer = customers.find((item) => item.uuid === detail?.order.customerUuid)
  return {
    sawUnitPrice: rollSteps.find((step) => step.stepType === STEP_TYPE_SAW)?.unitPrice ?? customer?.sawPrice,
    rewindUnitPrice: rollSteps.find((step) => step.stepType === STEP_TYPE_REWIND)?.unitPrice ?? customer?.rewindPrice,
  }
}

function requireRouteReady(form: DetailRouteFormState, appendMode: boolean) {
  if (!appendMode) return true
  if (form.stages.length > 0) return true
  message.warning('请先选择产物并配置至少一道追加工艺')
  return false
}

function stageRowKey(record: ProcessRouteStageLineVO) {
  return `${record.stageLevel ?? '-'}-${record.stepType ?? '-'}-${record.stepName ?? '-'}`
}

function rollLabel(roll: OriginalRoll) {
  const no = [roll.rollNo && `卷号:${roll.rollNo}`, roll.extraNo && `编号:${roll.extraNo}`].filter(Boolean).join(' / ')
  return `${roll.rowSort ?? '-'} | ${no || '未编号'} | ${roll.paperName || '-'} | ${roll.gramWeight ?? '-'}g / ${roll.originalWidth ?? '-'}mm | ${formatWeight(rollTotalWeight(roll))}`
}

function stageName(stageLevel?: number) {
  if (!stageLevel || stageLevel <= 1) return '首道产物'
  return `第${stageLevel}段产物`
}

function rollTotalWeight(roll: OriginalRoll) {
  return Number(roll.actualWeight ?? roll.totalWeight ?? (Number(roll.rollWeight ?? 0) * Number(roll.pieceNum ?? 1)))
}

function mergeSourceRolls(rolls: ReturnType<typeof sourceRollFromOutput>[]) {
  const [first] = rolls
  if (!first || rolls.length <= 1) return first
  return {
    ...first,
    localId: rolls.map((roll) => roll.localId).join('+'),
    uuid: rolls.map((roll) => roll.uuid).join('+'),
    rollWeight: totalSourceRollWeight(rolls),
  }
}

function sourceStageText(rows: DetailRouteOutputRow[]) {
  return Array.from(new Set(rows.map((row) => stageName(row.stageLevel)))).join('、')
}

function sourceNoText(rows: DetailRouteOutputRow[]) {
  return rows.map((row) => row.finishRollNo || row.sourceRollNo || row.sourceOutputKey || row.outputKey).join('、')
}

function sourcePaperText(rows: DetailRouteOutputRow[]) {
  return Array.from(new Set(rows.map((row) => row.paperName || '-'))).join('、')
}

function sourceSpecText(rows: DetailRouteOutputRow[]) {
  return rows.map((row) => `${row.gramWeight ?? '-'}g / ${row.finishWidth ?? '-'}mm`).join('、')
}

function totalSourceWeight(rows: DetailRouteOutputRow[]) {
  return rows.reduce((sum, row) => sum + Number(row.estimateWeight ?? 0), 0)
}

function totalSourceRollWeight(rolls: ReturnType<typeof sourceRollFromOutput>[]) {
  return rolls.reduce((sum, roll) => sum + Number(roll.rollWeight ?? 0) * Number(roll.pieceNum ?? 1), 0)
}

function formatWeight(value?: number) {
  return formatWeightKg(value)
}

interface BaseControlProps {
  roll: OriginalRoll
  rolls: OriginalRoll[]
  onRollChange: (uuid: string) => void
}

interface StageListProps {
  appendMode: boolean
  form: DetailRouteFormState
  prices: DetailRoutePriceDefaults
  setForm: Dispatch<SetStateAction<DetailRouteFormState | undefined>>
}

interface StageEditorProps {
  form: DetailRouteFormState
  prices: DetailRoutePriceDefaults
  setForm: Dispatch<SetStateAction<DetailRouteFormState | undefined>>
  stage: DetailRouteStageForm
}
