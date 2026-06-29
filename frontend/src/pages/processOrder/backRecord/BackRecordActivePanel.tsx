import { Alert, Button, Form, Input, InputNumber, Space, Tag, Typography } from 'antd'
import { ArrowRightOutlined, CopyOutlined, SwapOutlined } from '@ant-design/icons'
import { PROCESS_MODE } from '../../../constants/processOrder'
import { buildConditionText, buildLayoutText } from '../../../components/processOrder/shared/detailHelpers'
import { formatKg } from '../../../features/processOrderDetail/orderDetailUtils'
import type { BackRecordFormValues } from './backRecordUtils'
import BackRecordFinishEntryList from './BackRecordFinishEntryList'
import { processLines } from './backRecordWorkbenchUtils'
import type { BackRecordWorkItem } from './backRecordWorkbenchTypes'

interface Props {
  item: BackRecordWorkItem
  onNext: () => void
  onProcessChange: (item: BackRecordWorkItem) => void
}

export default function BackRecordActivePanel({ item, onNext, onProcessChange }: Props) {
  const form = Form.useFormInstance<BackRecordFormValues>()

  return (
    <main className="back-record-active">
      <div className="back-record-active__head">
        <div>
          <Typography.Title level={5}>{item.title}</Typography.Title>
          {item.subtitle && <Typography.Text type="secondary">{item.subtitle}</Typography.Text>}
        </div>
        <Space wrap>
          {item.isMergeGroup && <Tag color="geekblue">多母卷</Tag>}
          <Tag color={item.sourceMode === 'linked' ? 'success' : 'warning'}>{sourceLabel(item.sourceMode)}</Tag>
          <Button icon={<ArrowRightOutlined />} onClick={onNext}>下一项</Button>
        </Space>
      </div>

      {item.kind === 'roll' && <RollActualPanel item={item} form={form} />}
      <ProcessPanel item={item} onProcessChange={onProcessChange} />
      <BackRecordFinishEntryList item={item} />
    </main>
  )
}

function RollActualPanel({
  item,
  form,
}: {
  item: BackRecordWorkItem
  form: ReturnType<typeof Form.useForm<BackRecordFormValues>>[0]
}) {
  const roll = item.roll
  if (!roll) return null

  const fillCurrent = () => {
    form.setFieldValue(['rolls', roll.uuid], {
      actualGramWeight: roll.actualGramWeight ?? roll.gramWeight,
      actualWidth: roll.actualWidth ?? roll.originalWidth,
      actualWeight: roll.actualWeight ?? (roll.rollWeight ?? 0) * (roll.pieceNum ?? 1),
      remark: roll.remark,
    })
  }

  return (
    <section className="back-record-panel">
      <PanelHead title="原纸复称" extra={<Button size="small" icon={<CopyOutlined />} onClick={fillCurrent}>带入标称</Button>} />
      <div className="back-record-roll-facts">
        <Fact label="标称" value={`${roll.paperName || '-'} / ${roll.gramWeight ?? '-'}g / ${roll.originalWidth ?? '-'}mm`} />
        <Fact label="来料重量" value={formatKg((roll.rollWeight ?? 0) * (roll.pieceNum ?? 1))} />
        <Fact label="加工方式" value={PROCESS_MODE[roll.processMode ?? 1] ?? '-'} />
      </div>
      <div className="back-record-input-grid">
        <Form.Item name={['rolls', roll.uuid, 'actualGramWeight']} label="实测克重">
          <InputNumber min={1} placeholder="g" addonAfter="g" />
        </Form.Item>
        <Form.Item name={['rolls', roll.uuid, 'actualWidth']} label="实测门幅">
          <InputNumber min={1} placeholder="mm" addonAfter="mm" />
        </Form.Item>
        <Form.Item name={['rolls', roll.uuid, 'actualWeight']} label="复称重量" rules={[{ required: true, message: '必填' }]}>
          <InputNumber min={0.001} placeholder="kg" addonAfter="kg" />
        </Form.Item>
        <Form.Item name={['rolls', roll.uuid, 'remark']} label="复核说明">
          <Input placeholder="破损、水湿、复称差异" />
        </Form.Item>
      </div>
    </section>
  )
}

function ProcessPanel({
  item,
  onProcessChange,
}: {
  item: BackRecordWorkItem
  onProcessChange: (item: BackRecordWorkItem) => void
}) {
  const production = item.production

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
    </section>
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

function sourceLabel(mode: BackRecordWorkItem['sourceMode']) {
  if (mode === 'linked') return '真实来源'
  if (mode === 'inferred') return '辅助匹配'
  if (mode === 'pool') return '待核对'
  return '无成品'
}
