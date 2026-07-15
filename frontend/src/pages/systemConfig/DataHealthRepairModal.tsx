import { Form, Input, Modal, message } from 'antd'
import { useReconcileSettlement } from '../../features/dataHealth/hooks/useReconcileSettlement'
import { useRestoreCompletedOrder } from '../../features/dataHealth/hooks/useRestoreCompletedOrder'
import type { DataHealthIssue, DataHealthRepairRequest } from '../../types/dataHealth'

interface Props {
  issue?: DataHealthIssue
  onClose: () => void
}

export default function DataHealthRepairModal({ issue, onClose }: Props) {
  const [form] = Form.useForm<DataHealthRepairRequest>()
  const { mutateAsync: reconcile, isPending: isReconciling } = useReconcileSettlement()
  const { mutateAsync: restore, isPending: isRestoring } = useRestoreCompletedOrder()
  const pending = isReconciling || isRestoring

  async function submit() {
    if (!issue?.repairAction) return
    const data = await form.validateFields()
    const result = issue.repairAction === 'RECONCILE_SETTLEMENT'
      ? await reconcile({ uuid: issue.businessUuid, data })
      : await restore({ uuid: issue.businessUuid, data })
    message.success(result.message)
    form.resetFields()
    onClose()
  }

  return (
    <Modal
      title={`修复 ${issue?.businessNo ?? ''}`}
      open={Boolean(issue)}
      confirmLoading={pending}
      okButtonProps={{ danger: true }}
      okText="确认修复"
      onCancel={onClose}
      onOk={() => void submit()}
      destroyOnHidden
    >
      <p className="data-health-repair-warning">修复会修改业务状态或金额，并清除过期结算快照。操作将写入审计日志。</p>
      <Form form={form} layout="vertical">
        <Form.Item name="reason" label="修复原因" rules={[{ required: true, message: '请填写修复原因' }]}>
          <Input.TextArea maxLength={255} showCount rows={3} />
        </Form.Item>
        <Form.Item
          name="confirmation"
          label={`输入 ${issue?.businessNo ?? '业务单号'} 确认`}
          rules={[{ required: true, message: '请输入完整业务单号' }]}
        >
          <Input autoComplete="off" />
        </Form.Item>
      </Form>
    </Modal>
  )
}
