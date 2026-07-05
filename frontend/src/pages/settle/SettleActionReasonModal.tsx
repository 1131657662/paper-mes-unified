import { Form, Input, Modal } from 'antd'
import type { FormInstance } from 'antd'
import type { ReceiveRecord } from '../../types/settle'

export type SettleActionTarget =
  | { type: 'cancelReceive'; record: ReceiveRecord }
  | { type: 'voidSettle' }

interface Props {
  actionTarget: SettleActionTarget | null
  form: FormInstance<{ reason: string }>
  loading: boolean
  onCancel: () => void
  onOk: () => void
}

export default function SettleActionReasonModal({ actionTarget, form, loading, onCancel, onOk }: Props) {
  return (
    <Modal
      title={actionTarget?.type === 'cancelReceive' ? '撤销收款' : '作废结算单'}
      open={!!actionTarget}
      okText="确认"
      cancelText="取消"
      okButtonProps={{ danger: true, loading }}
      onCancel={onCancel}
      onOk={onOk}
    >
      <p className="document-action-warning">
        {actionTarget?.type === 'cancelReceive'
          ? '撤销后系统会重新计算已结清、现金实收、废纸抵扣、未收和状态，原流水会保留为已撤销。'
          : '作废后关联加工单会退回已完成可结算状态；已有有效收款的结算单不能作废。'}
      </p>
      <Form form={form} layout="vertical">
        <Form.Item name="reason" label="原因" rules={[{ required: true, message: '请输入原因' }]}>
          <Input.TextArea rows={3} maxLength={255} showCount placeholder="请输入原因，便于业务追溯" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
