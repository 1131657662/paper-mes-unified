import { Alert, Button, Checkbox, Drawer, Form, Input, InputNumber, Space, Statistic, Table, Typography, message } from 'antd'
import type { ColumnsType } from 'antd/es/table'
import type { ProcessStepPricingBatchPreviewRow } from '../../../api/processOrder'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { formatMoney, formatNumber } from '../orderDetailUtils'
import { buildPricingBatchRequest, initialPricingValues, type PricingBatchFormValues, type PricingStep } from '../pricingBatchModel'
import { useApplyProcessStepPricingBatch } from '../hooks/useApplyProcessStepPricingBatch'
import { usePreviewProcessStepPricingBatch } from '../hooks/usePreviewProcessStepPricingBatch'
import ServicePricingBatchFields from './ServicePricingBatchFields'
import './ProcessStepPricingBatchDrawer.css'

interface Props {
  detail: ProcessOrderDetailVO
  open: boolean
  selectedSteps: PricingStep[]
  onApplied: () => void
  onClose: () => void
}

export default function ProcessStepPricingBatchDrawer({ detail, open, selectedSteps, onApplied, onClose }: Props) {
  const [form] = Form.useForm<PricingBatchFormValues>()
  const { mutateAsync: previewPricing, data: preview, isPending: isPreviewing, reset: resetPreview } = usePreviewProcessStepPricingBatch()
  const { mutateAsync: applyPricing, isPending: isApplying } = useApplyProcessStepPricingBatch()
  const sawCount = selectedSteps.filter((step) => step.stepType === 1).length
  const rewindCount = selectedSteps.filter((step) => step.stepType === 2).length
  const stripCount = selectedSteps.filter((step) => step.stepType === 3).length
  const repackCount = selectedSteps.filter((step) => step.stepType === 4).length

  const request = async (requestId?: string) => {
    const values = await form.validateFields()
    return buildPricingBatchRequest({ orderVersion: detail.order.version ?? 1, selectedSteps, values, requestId })
  }
  const handlePreview = async () => previewPricing({ orderUuid: detail.order.uuid, values: await request() })
  const handleApply = async () => {
    await applyPricing({ orderUuid: detail.order.uuid, values: await request(crypto.randomUUID()) })
    message.success(`已核定 ${selectedSteps.length} 道工序费用`)
    onApplied()
  }

  return (
    <Drawer className="pricing-batch-drawer" width="min(720px, 100vw)" open={open} title="批量核定工序费用"
      destroyOnHidden onClose={onClose} footer={<DrawerFooter previewed={Boolean(preview)} loading={isPreviewing || isApplying}
        onApply={handleApply} onClose={onClose} onPreview={handlePreview} />}>
      <Alert type="info" showIcon message={`已选择 ${selectedSteps.length} 道工序`}
        description={`切纸 ${sawCount} 道，复卷 ${rewindCount} 道，剥损整理 ${stripCount} 道，重新包装 ${repackCount} 道。系统会按工艺分别核定，并一次重算整单费用。`} />
      <Form form={form} layout="vertical" initialValues={initialPricingValues(selectedSteps)}
        onValuesChange={resetPreview}>
        {sawCount > 0 && <PriceGroupFields form={form} type="saw" count={sawCount} />}
        {rewindCount > 0 && <PriceGroupFields form={form} type="rewind" count={rewindCount} />}
        {stripCount > 0 && <ServicePricingBatchFields form={form} prefix="strip" label="剥损整理" count={stripCount} />}
        {repackCount > 0 && <ServicePricingBatchFields form={form} prefix="repack" label="重新包装" count={repackCount} />}
        <Form.Item name="reason" label="核定原因" rules={[{ required: true, whitespace: true, max: 255, message: '请填写核定原因（最多255字）' }]}>
          <Input.TextArea rows={3} maxLength={255} showCount placeholder="例如：客户协议价格、临时优惠或特殊加工约定" />
        </Form.Item>
      </Form>
      {preview && <PricingPreview preview={preview} detail={detail} />}
    </Drawer>
  )
}

function PriceGroupFields({ form, type, count }: { form: ReturnType<typeof Form.useForm<PricingBatchFormValues>>[0], type: 'saw' | 'rewind', count: number }) {
  const restoreName = type === 'saw' ? 'sawRestore' : 'rewindRestore'
  const priceName = type === 'saw' ? 'sawPrice' : 'rewindPrice'
  const restore = Form.useWatch(restoreName, form)
  const label = type === 'saw' ? '切纸' : '复卷'
  const unit = type === 'saw' ? '元/刀' : '元/吨'
  return (
    <section className="pricing-batch-group">
      <div className="pricing-batch-group__title"><strong>{label}</strong><span>{count} 道 · {unit}</span></div>
      <Form.Item name={restoreName} valuePropName="checked"><Checkbox>恢复标准单价</Checkbox></Form.Item>
      <Form.Item label={`核定单价（${unit}）`} required>
        <Space.Compact block>
          <Form.Item name={priceName} noStyle rules={[{
            validator: (_, value) => restore || (typeof value === 'number' && value > 0)
              ? Promise.resolve() : Promise.reject(new Error('请输入大于0的核定单价')),
          }]}>
            <InputNumber aria-label={`${label}核定单价`} disabled={restore} min={0.0001} precision={4}
              style={{ width: '100%' }} />
          </Form.Item>
          <Input aria-label="计价单位" readOnly tabIndex={-1} value={unit} style={{ width: 72 }} />
        </Space.Compact>
      </Form.Item>
    </section>
  )
}

function PricingPreview({ preview, detail }: { preview: NonNullable<ReturnType<typeof usePreviewProcessStepPricingBatch>['data']>, detail: ProcessOrderDetailVO }) {
  const columns = previewColumns(detail)
  return (
    <section className="pricing-batch-preview">
      <div className="pricing-batch-preview__summary">
        <Statistic title="标准金额" value={preview.standardAmount} precision={2} prefix="¥" />
        <Statistic title="核定后金额" value={preview.finalAmount} precision={2} prefix="¥" />
        <Statistic title="调整金额" value={preview.adjustmentAmount} precision={2} prefix="¥"
          valueStyle={{ color: preview.adjustmentAmount < 0 ? 'var(--mes-color-warning)' : undefined }} />
      </div>
      <Table rowKey="stepUuid" size="small" pagination={false} columns={columns} dataSource={preview.rows}
        scroll={{ x: 760, y: 280 }} />
    </section>
  )
}

function previewColumns(detail: ProcessOrderDetailVO): ColumnsType<ProcessStepPricingBatchPreviewRow> {
  return [
    { title: '所属母卷', width: 130, render: (_, row) => rollLabel(detail, row.originalUuid) },
    { title: '工序', width: 100, render: (_, row) => stepTypeName(row.stepType) },
    { title: '数量', width: 110, align: 'right', render: (_, row) => quantityLabel(row) },
    { title: '核价结果', width: 180, align: 'right', render: (_, row) => pricingResult(row) },
    { title: '最终金额', width: 110, align: 'right', render: (_, row) => formatMoney(row.finalAmount) },
    { title: '调整', width: 100, align: 'right', render: (_, row) => formatMoney(row.adjustmentAmount) },
  ]
}

function stepTypeName(type: number) {
  return ({ 1: '切纸', 2: '复卷', 3: '剥损整理', 4: '重新包装' } as Record<number, string>)[type] ?? '其他工序'
}

function quantityLabel(row: ProcessStepPricingBatchPreviewRow) {
  if (row.billingMode === 3) return '按合计金额'
  if (row.billingMode === 4) return '免费'
  const unit = row.stepType === 1 ? '刀' : row.billingBasis === 'PIECE' ? '件' : 't'
  return `${formatNumber(row.quantity, unit === 't' ? 3 : 0)} ${unit}`
}

function pricingResult(row: ProcessStepPricingBatchPreviewRow) {
  if (row.billingMode === 3) return <Typography.Text strong>固定金额</Typography.Text>
  if (row.billingMode === 4) return <Typography.Text strong>免费</Typography.Text>
  return <Typography.Text>{formatNumber(row.currentUnitPrice, 4)} → <strong>{formatNumber(row.finalUnitPrice, 4)}</strong></Typography.Text>
}

function rollLabel(detail: ProcessOrderDetailVO, originalUuid?: string) {
  const roll = detail.originalRolls?.find((item) => item.uuid === originalUuid)
  return roll?.rollNo || roll?.extraNo || `第 ${String(roll?.rowSort ?? '-').padStart(2, '0')} 卷`
}

function DrawerFooter({ previewed, loading, onApply, onClose, onPreview }: { previewed: boolean, loading: boolean, onApply: () => void, onClose: () => void, onPreview: () => void }) {
  return <Space className="pricing-batch-drawer__footer"><Button onClick={onClose}>取消</Button>
    {previewed ? <Button type="primary" loading={loading} onClick={onApply}>确认应用</Button>
      : <Button type="primary" loading={loading} onClick={onPreview}>预览变更</Button>}</Space>
}
