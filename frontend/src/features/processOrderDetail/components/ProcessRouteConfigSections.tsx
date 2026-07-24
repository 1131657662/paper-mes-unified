import { Button, Card, Descriptions, Select, Space, Table, Typography } from 'antd'
import { PlusOutlined, SaveOutlined } from '@ant-design/icons'
import MesTooltip from '../../../components/biz/MesTooltip'
import type { OriginalRoll, ProcessRoutePreviewVO } from '../../../types/processOrder'
import { formatGram, formatMm } from '../../../utils/numberFormatters'
import {
  STEP_TYPE_REWIND,
  STEP_TYPE_SAW,
  finalDetailRouteOutputs,
  type DetailRouteFormState,
  type DetailRouteOutputRow,
} from '../routeConfigDetail'
import { formatMoney } from '../orderDetailUtils'
import {
  formatWeight,
  outputColumns,
  previewOutputColumns,
  rollLabel,
  rollTotalWeight,
  stageColumns,
  stageRowKey,
} from './processRouteConfigPresentation'

export function RouteBaseControls(props: {
  roll: OriginalRoll; rolls: OriginalRoll[]; onRollChange: (uuid: string) => void
}) {
  return <Card size="small" title="来源母卷"><Space direction="vertical" size={10} className="process-route-config">
    <Select aria-label="来源母卷" showSearch value={props.roll.uuid} optionFilterProp="label"
      options={props.rolls.map((item) => ({ value: item.uuid, label: rollLabel(item) }))}
      className="process-route-config__roll-select" onChange={props.onRollChange} />
    <Descriptions size="small" column={4} bordered className="process-route-config__source-summary">
      <Descriptions.Item label="卷号">{props.roll.rollNo || '-'}</Descriptions.Item>
      <Descriptions.Item label="编号">{props.roll.extraNo || '-'}</Descriptions.Item>
      <Descriptions.Item label="品名">{props.roll.paperName || '-'}</Descriptions.Item>
      <Descriptions.Item label="规格">{formatGram(props.roll.gramWeight)} / {formatMm(props.roll.originalWidth)}</Descriptions.Item>
      <Descriptions.Item label="直径/纸芯">{props.roll.originalDiameter ?? '-'} / {props.roll.coreDiameter ?? '-'}</Descriptions.Item>
      <Descriptions.Item label="件数">{props.roll.pieceNum ?? 1} 件</Descriptions.Item>
      <Descriptions.Item label="重量">{formatWeight(rollTotalWeight(props.roll))}</Descriptions.Item>
      <Descriptions.Item label="首道工艺">{props.roll.mainStepType === STEP_TYPE_SAW ? '锯纸' : '复卷'}</Descriptions.Item>
    </Descriptions>
  </Space></Card>
}

export function RouteFirstOutputTable(props: {
  appendMode: boolean; rows: DetailRouteOutputRow[]; selectedOutputKey?: string
}) {
  return <Card size="small" title={props.appendMode ? '可继续加工的当前产物' : '首道产物'}>
    <Table size="small" rowKey="outputKey" pagination={false} columns={outputColumns}
      dataSource={props.rows} scroll={{ x: 760 }}
      rowClassName={(row) => selectedOutput(row, props.selectedOutputKey) ? 'process-route-config__selected-row' : ''} />
  </Card>
}

export function RouteStageToolbar(props: {
  appendMode: boolean; disabled: boolean; onAdd: (stepType: number) => void
}) {
  return <Card size="small" className="process-route-config__stage-toolbar">
    <div className="process-route-config__toolbar-inner">
      <Typography.Text strong>{props.appendMode ? '给选中产物追加下一段' : '从首道产物设计下一段'}</Typography.Text>
      <Space wrap size={8}>
        <Button disabled={props.disabled} icon={<PlusOutlined />} onClick={() => props.onAdd(STEP_TYPE_SAW)}>锯纸</Button>
        <Button disabled={props.disabled} type="primary" icon={<PlusOutlined />} onClick={() => props.onAdd(STEP_TYPE_REWIND)}>复卷</Button>
      </Space>
      <Typography.Text type="secondary">{props.appendMode ? '下一段阶段号会按所选产物自动顺延' : '删除本段后会回到上游产物重新选择'}</Typography.Text>
    </div>
  </Card>
}

export function RouteResult({ form, preview }: {
  form: DetailRouteFormState; preview?: ProcessRoutePreviewVO
}) {
  if (!preview) return <Descriptions size="small" column={2} bordered>
    <Descriptions.Item label="已配置后续阶段">{form.stages.length} 道</Descriptions.Item>
    <Descriptions.Item label="当前最终产物">{finalDetailRouteOutputs(form).length} 件</Descriptions.Item>
  </Descriptions>
  return <Space direction="vertical" size={10} className="process-route-config">
    <Descriptions size="small" column={2} bordered>
      <Descriptions.Item label="链式工艺费用">{formatMoney(preview.totalAmount)}</Descriptions.Item>
      <Descriptions.Item label="最终产出">{(preview.outputs ?? []).filter((item) => !item.consumedByNextStage).length} 件</Descriptions.Item>
    </Descriptions>
    <Table size="small" rowKey={stageRowKey} pagination={false} columns={stageColumns}
      dataSource={preview.stages ?? []} scroll={{ x: 760 }} />
    <Table size="small" rowKey={(record) => record.outputKey ?? ''} pagination={false}
      columns={previewOutputColumns} dataSource={preview.outputs ?? []} scroll={{ x: 860 }} />
  </Space>
}

export function RouteDrawerFooter(props: {
  appendMode: boolean; disabledReason?: string; loading: boolean; onClose: () => void; onSave: () => void
}) {
  return <Space className="process-route-config__footer">
    <Button onClick={props.onClose}>取消</Button>
    <MesTooltip title={props.disabledReason}><span>
      <Button type="primary" danger={!props.appendMode} icon={<SaveOutlined />} disabled={Boolean(props.disabledReason)}
        loading={props.loading} onClick={props.onSave}>
        {props.appendMode ? '保存追加工艺' : '保存重配路线'}
      </Button>
    </span></MesTooltip>
  </Space>
}

function selectedOutput(row: DetailRouteOutputRow, selectedOutputKey?: string): boolean {
  return Boolean(selectedOutputKey
    && (row.outputKey === selectedOutputKey || row.finishRollNo === selectedOutputKey))
}
