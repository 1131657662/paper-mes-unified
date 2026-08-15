import { Alert, Form, Input, InputNumber, Select, Space, Typography, type FormInstance } from 'antd'
import type { ProcessOrderDetailVO } from '../../../types/processOrder'
import { DEFAULT_DISPOSITION_VALUES, type RollOption } from './processRollDispositionOptions'
import { ActionSelection, RollSelectionList } from './ProcessRollDispositionChoices'
import { normalizeRollSelection, type ProcessRollDispositionFormValues } from './processRollDispositionSubmit'

interface Props {
  detail: ProcessOrderDetailVO
  form: FormInstance<ProcessRollDispositionFormValues>
  rolls: RollOption[]
  excludedCount: number
  multipleSelection: boolean
  selectedRollUuids: string[]
  warehouses: Array<{ uuid: string; warehouseName: string; status?: number }>
  onSubmit: (values: ProcessRollDispositionFormValues) => Promise<void>
}

export default function ProcessRollDispositionForm({
  detail,
  form,
  rolls,
  excludedCount,
  multipleSelection,
  selectedRollUuids,
  warehouses,
  onSubmit,
}: Props) {
  return (
    <Form<ProcessRollDispositionFormValues>
      key={`${detail.order.uuid}-${rolls.map((roll) => roll.value).join(',')}`}
      form={form}
      layout="vertical"
      initialValues={DEFAULT_DISPOSITION_VALUES}
      onFinish={onSubmit}
      onValuesChange={(changedValues) => {
        if (changedValues.action === undefined || changedValues.action === 'CANCEL') return
        const current = form.getFieldValue('rollUuids') ?? []
        const normalized = normalizeRollSelection(changedValues.action, current)
        if (normalized.length !== current.length) form.setFieldValue('rollUuids', normalized)
      }}
    >
      <section className="process-roll-disposition__section">
        <div className="process-roll-disposition__section-heading">
          <div className="process-roll-disposition__section-title">
            <strong>选择母卷</strong>
            <span className="process-roll-disposition__count-badge">已选 {selectedRollUuids.length} 卷</span>
          </div>
          <span>{rolls.length} 卷可处置 · {multipleSelection && rolls.length > 1 ? '可多选取消' : '当前操作需单选'}</span>
        </div>
        {excludedCount > 0 ? (
          <Typography.Text type="secondary" className="process-roll-disposition__excluded-note">
            另有 {excludedCount} 卷已完成或已处置，已自动隐藏
          </Typography.Text>
        ) : null}
        <Form.Item name="rollUuids" rules={[{ required: true, type: 'array', min: 1, message: '请至少选择 1 卷母卷' }]}>
          <RollSelectionList options={rolls} multiple={multipleSelection} />
        </Form.Item>
        {selectedRollUuids.length > 1 && !multipleSelection ? (
          <Alert
            className="process-roll-disposition__selection-alert"
            type="info"
            showIcon
            message="当前操作一次只能处理 1 卷"
            description="转直发需要分别录入现场称重，拆分代加工单会分别生成新单；请选择 1 卷后继续。"
          />
        ) : null}
      </section>
      <section className="process-roll-disposition__section">
        <div className="process-roll-disposition__section-heading">
          <strong>处置方式</strong>
          <span>按所选母卷和业务规则执行</span>
        </div>
        <Form.Item name="action" rules={[{ required: true, message: '请选择处置方式' }]}>
          <ActionSelection selectedCount={selectedRollUuids.length} />
        </Form.Item>
      </section>
      <Form.Item noStyle shouldUpdate={(previous, current) => previous.action !== current.action}>
        {({ getFieldValue }) => getFieldValue('action') === 'DIRECT_SHIP' && selectedRollUuids.length === 1
          ? <DirectShipFields warehouses={warehouses} />
          : null}
      </Form.Item>
      <Form.Item name="reason" label="处置原因" rules={[{ required: true, whitespace: true, max: 500, message: '请填写处置原因（不超过 500 字）' }]}>
        <Input.TextArea rows={3} maxLength={500} showCount placeholder="例如：客户取消本次加工，现场称重后改为直发" />
      </Form.Item>
    </Form>
  )
}

function DirectShipFields({ warehouses }: { warehouses: Array<{ uuid: string; warehouseName: string; status?: number }> }) {
  return (
    <Space direction="vertical" size={0} style={{ width: '100%' }}>
      <Form.Item name="warehouseUuid" label="入库仓库" rules={[{ required: true, message: '请选择入库仓库' }]}>
        <Select options={warehouses.filter((item) => item.status === 1).map((item) => ({ label: item.warehouseName, value: item.uuid }))} placeholder="请选择启用中的仓库" />
      </Form.Item>
      <Form.Item name="actualWeight" label="现场称重总重量（kg）" rules={[{ required: true, message: '请填写现场称重总重量' }]}>
        <InputNumber min={0.001} precision={3} style={{ width: '100%' }} placeholder="请输入实测重量" />
      </Form.Item>
    </Space>
  )
}
