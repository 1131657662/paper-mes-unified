import { Alert, Button, Form, Input, InputNumber, Space, Tag, Typography } from 'antd'
import { SwapOutlined } from '@ant-design/icons'
import { PROCESS_MODE } from '../../../constants/processOrder'
import { buildConditionText, buildLayoutText } from '../../../components/processOrder/shared/detailHelpers'
import { formatKg } from '../../../features/processOrderDetail/orderDetailUtils'
import { formatGram, formatMm } from '../../../utils/numberFormatters'
import type { ProcessStep } from '../../../types/processOrder'
import BackRecordFinishEntryList from './BackRecordFinishEntryList'
import BackRecordOnSiteOutputList from './BackRecordOnSiteOutputList'
import BackRecordTrimEntryList from './BackRecordTrimEntryList'
import { focusNextBackRecordField } from './backRecordKeyboard'
import { processLines } from './backRecordWorkbenchUtils'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'
import type { BackRecordSourceOption } from './BackRecordFinishFields'
import BackRecordCurrentToolbar from './BackRecordCurrentToolbar'

interface Props {
  item: BackRecordWorkItem
  onNext: () => void
  onPrevious: () => void
  onProcessChange: (item: BackRecordWorkItem) => void
  sourceOptions: BackRecordSourceOption[]
}

export default function BackRecordActivePanel({ item, onNext, onPrevious, onProcessChange, sourceOptions }: Props) {
  return (
    <main className="back-record-active">
      <BackRecordCurrentToolbar item={item} onNext={onNext} onPrevious={onPrevious} />

      {item.kind === 'roll' && <RollActualPanel item={item} onFieldExhausted={onNext} />}
      <ProcessPanel item={item} onFieldExhausted={onNext} onProcessChange={onProcessChange} />
      {item.roll?.processMode === 2 ? (
        <BackRecordOnSiteOutputList item={item} sourceOptions={sourceOptions} onFieldExhausted={onNext} />
      ) : (
        <>
          <BackRecordFinishEntryList item={item} sourceOptions={sourceOptions} onFieldExhausted={onNext} />
          <BackRecordTrimEntryList item={item} sourceOptions={sourceOptions} onFieldExhausted={onNext} />
        </>
      )}
    </main>
  )
}

function RollActualPanel({
  item,
  onFieldExhausted,
}: {
  item: BackRecordWorkItem
  onFieldExhausted: () => void
}) {
  const roll = item.roll
  if (!roll) return null

  return (
    <section className="back-record-panel">
      <PanelHead title="原纸复称" />
      <div className="back-record-roll-facts">
        <Fact label="卷号" value={roll.rollNo || '-'} />
        <Fact label="编号" value={roll.extraNo || '-'} />
        <Fact label="批次" value={roll.batchNo || '-'} />
        <Fact label="件数" value={`${roll.pieceNum ?? 1} 件`} />
        <Fact label="标称" value={`${roll.paperName || '-'} / ${formatGram(roll.gramWeight)} / ${formatMm(roll.originalWidth)}`} />
        <Fact label="来料重量" value={formatKg((roll.rollWeight ?? 0) * (roll.pieceNum ?? 1))} />
        <Fact label="加工方式" value={PROCESS_MODE[roll.processMode ?? 1] ?? '-'} />
      </div>
      <div className="back-record-input-grid">
        <Form.Item name={['rolls', roll.uuid, 'actualGramWeight']} label="实测克重">
          <InputNumber data-back-record-field="true" min={1} placeholder="g" suffix="g" onPressEnter={(event) => focusNextBackRecordField(event, onFieldExhausted)} />
        </Form.Item>
        <Form.Item name={['rolls', roll.uuid, 'actualWidth']} label="实测门幅">
          <InputNumber data-back-record-field="true" min={1} placeholder="mm" suffix="mm" onPressEnter={(event) => focusNextBackRecordField(event, onFieldExhausted)} />
        </Form.Item>
        <Form.Item name={['rolls', roll.uuid, 'actualWeight']} label="复称重量" rules={[{ required: true, message: '必填' }]}>
          <InputNumber data-back-record-field="true" min={0.001} placeholder="kg" suffix="kg" onPressEnter={(event) => focusNextBackRecordField(event, onFieldExhausted)} />
        </Form.Item>
        <Form.Item name={['rolls', roll.uuid, 'remark']} label="复核说明">
          <Input data-back-record-field="true" placeholder="破损、水湿、复称差异" onPressEnter={(event) => focusNextBackRecordField(event, onFieldExhausted)} />
        </Form.Item>
      </div>
    </section>
  )
}

function ProcessPanel({
  item,
  onFieldExhausted,
  onProcessChange,
}: {
  item: BackRecordWorkItem
  onFieldExhausted: () => void
  onProcessChange: (item: BackRecordWorkItem) => void
}) {
  const production = item.production
  const steps = processSteps(item)

  return (
    <section className="back-record-panel">
      <PanelHead
        title="工艺核对"
        extra={(
          <Space wrap size={6}>
            {production && <Tag>{PROCESS_MODE[production.processMode ?? 1]}</Tag>}
            <Button size="small" icon={<SwapOutlined />} onClick={() => onProcessChange(item)}>
              现场变更
            </Button>
          </Space>
        )}
      />
      {item.sourceMode === 'inferred' && (
        <Alert showIcon type="warning" message="该单缺少成品来源关系，当前按母卷顺序辅助匹配；提交仍以后端闭合校验为准。" />
      )}
      {item.kind === 'pool' && (
        <Alert showIcon type="warning" message="这些成品未绑定母卷，只能作为待核对成品录入，无法在前端计算逐卷闭合。" />
      )}
      {production && (
        <div className="back-record-plan-facts">
          <Fact label="条件" value={buildConditionText(production)} />
          <Fact label="排布" value={buildLayoutText(production)} />
        </div>
      )}
      <div className="back-record-process-lines">
        {processLines(item).map((line) => (
          <div className="back-record-process-line" key={line.header}>
            <span>{line.header}</span>
            <small>{line.details.join(' / ') || '暂无明细'}</small>
          </div>
        ))}
      </div>
      {steps.length > 0 && <StepLossEditor onSite={item.roll?.processMode === 2} steps={steps} onFieldExhausted={onFieldExhausted} />}
    </section>
  )
}

function StepLossEditor({
  steps,
  onSite,
  onFieldExhausted,
}: {
  steps: ProcessStep[]
  onSite: boolean
  onFieldExhausted: () => void
}) {
  return (
    <div className="back-record-step-loss-grid">
      {steps.map((step) => (
        <div key={step.uuid} className="back-record-step-actuals">
          {onSite && step.stepType === 1 && (
            <Form.Item name={['steps', step.uuid, 'knifeCount']} label={`${stepLabel(step)}实际刀数`} rules={[{ required: true, message: '请输入实际刀数' }]}>
              <InputNumber data-back-record-field="true" min={1} precision={0} suffix="刀" />
            </Form.Item>
          )}
          <Form.Item name={['steps', step.uuid, 'lossWeight']} label={`${stepLabel(step)}损耗`}>
            <InputNumber data-back-record-field="true" min={0} precision={3} placeholder="kg" suffix="kg" onPressEnter={(event) => focusNextBackRecordField(event, onFieldExhausted)} />
          </Form.Item>
        </div>
      ))}
    </div>
  )
}

function PanelHead({ title, extra }: { title: string; extra?: React.ReactNode }) {
  return (
    <div className="back-record-panel__head">
      <Typography.Text strong>{title}</Typography.Text>
      {extra}
    </div>
  )
}

function Fact({ label, value }: { label: string; value?: string }) {
  return (
    <div className="back-record-fact">
      <span>{label}</span>
      <strong>{value || '-'}</strong>
    </div>
  )
}

function processSteps(item: BackRecordWorkItem): ProcessStep[] {
  const productions = item.rollProductions.length ? item.rollProductions : item.production ? [item.production] : []
  const steps = productions.flatMap((production) => production.steps ?? [])
  return Array.from(new Map(steps.map((step) => [step.uuid, step])).values())
    .sort((a, b) => (a.stageLevel ?? a.stepSort ?? 0) - (b.stageLevel ?? b.stepSort ?? 0))
}

function stepLabel(step: ProcessStep) {
  const stage = step.stageLevel ? `第${step.stageLevel}道` : `工序${step.stepSort ?? ''}`
  return `${stage}${step.stepName ? ` ${step.stepName}` : ''}`
}
