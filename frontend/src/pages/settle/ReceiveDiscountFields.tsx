import { Alert, Button, Form, Input, InputNumber, Select, Space, Tag, message } from 'antd'
import type { FormInstance } from 'antd'
import { CheckOutlined, SendOutlined } from '@ant-design/icons'
import { useRef } from 'react'
import { PERMISSIONS } from '../../constants/permissions'
import { useApproveSettleDiscount } from '../../features/settle/hooks/useApproveSettleDiscount'
import { useRequestSettleDiscountApproval } from '../../features/settle/hooks/useRequestSettleDiscountApproval'
import { useSettleDiscountApprovals } from '../../features/settle/hooks/useSettleDiscountApprovals'
import { useNumberConfigValue } from '../../features/systemConfig/hooks/useSystemConfigValue'
import { useHasPermission } from '../../stores/authStore'
import type { SettleDiscountApproval } from '../../types/settle'
import { discountReasonError, roundMoney, type ReceiveFormValues } from './receiveFormModel'

interface Props {
  form: FormInstance<ReceiveFormValues>
  open: boolean
  settleUuid: string
  unreceivedAmount: number
}

export default function ReceiveDiscountFields({ form, open, settleUuid, unreceivedAmount }: Props) {
  const discountAmount = roundMoney(Form.useWatch('discountAmount', form) ?? 0)
  const { value: autoLimit } = useNumberConfigValue('settle.discountAutoApproveLimit', 1)
  const { data: approvals = [] } = useSettleDiscountApprovals(settleUuid, open)
  const { mutateAsync: requestApproval, isPending: isRequesting } = useRequestSettleDiscountApproval()
  const { mutateAsync: approveDiscount, isPending: isApproving } = useApproveSettleDiscount()
  const canDiscount = useHasPermission(PERMISSIONS.settleDiscount)
  const canApprove = useHasPermission(PERMISSIONS.settleDiscountApprove)
  const requestRef = useRef({ signature: '', uuid: crypto.randomUUID() })
  const needsApproval = discountAmount > autoLimit
  const approvedOptions = matchingApprovedOptions(approvals, discountAmount)

  const submitApproval = async () => {
    await form.validateFields(['discountAmount', 'discountReason'])
    const reason = form.getFieldValue('discountReason')?.trim()
    const signature = `${discountAmount}|${reason}`
    const requestId = requestIdFor(requestRef.current, signature)
    await requestApproval({ uuid: settleUuid, data: { requestId, discountAmount, reason } })
    message.success('优惠审批申请已提交')
  }

  const approve = async (item: SettleDiscountApproval) => {
    await approveDiscount({ uuid: settleUuid, approvalUuid: item.uuid })
    form.setFieldValue('discountApprovalUuid', item.uuid)
    message.success('优惠审批已批准')
  }

  return <>
    <Form.Item name="discountAmount" label="优惠/尾差核销"
      tooltip="不计入实际到账，仅用于核销双方确认的优惠或尾差">
      <InputNumber disabled={!canDiscount} style={{ width: '100%' }} min={0} max={unreceivedAmount} precision={2} />
    </Form.Item>
    <Form.Item name="discountReason" label="优惠原因" rules={[{
      validator: () => validateReason(form.getFieldsValue()),
    }]}>
      <Input.TextArea disabled={!canDiscount || discountAmount <= 0} rows={2} maxLength={255}
        placeholder="填写双方确认的优惠、抹零或尾差原因" />
    </Form.Item>
    {needsApproval && <ApprovalFields approvals={approvals} approvedOptions={approvedOptions}
      canApprove={canApprove} isApproving={isApproving} isRequesting={isRequesting}
      onApprove={approve} onRequest={submitApproval} />}
  </>
}

function ApprovalFields({ approvals, approvedOptions, canApprove, isApproving, isRequesting, onApprove, onRequest }: {
  approvals: SettleDiscountApproval[]
  approvedOptions: { label: string; value: string }[]
  canApprove: boolean
  isApproving: boolean
  isRequesting: boolean
  onApprove: (item: SettleDiscountApproval) => Promise<void>
  onRequest: () => Promise<void>
}) {
  const pending = approvals.filter((item) => item.approvalStatus === 1)
  return <div className="receive-discount-approval">
    <Alert type="warning" showIcon message="该金额超过免审阈值，登记前必须由另一账号批准" />
    <Form.Item name="discountApprovalUuid" label="已批准记录"
      rules={[{ required: true, message: '请选择已批准且金额一致的审批记录' }]}>
      <Select options={approvedOptions} placeholder="选择审批记录" />
    </Form.Item>
    <Space wrap>
      <Button icon={<SendOutlined />} loading={isRequesting} onClick={() => void onRequest()}>提交审批申请</Button>
      {pending.map((item) => <Tag key={item.uuid} color="processing">
        {item.requestByName} 申请 ¥{item.discountAmount.toFixed(2)}
        {canApprove && <Button type="link" size="small" icon={<CheckOutlined />} loading={isApproving}
          onClick={() => void onApprove(item)}>批准</Button>}
      </Tag>)}
    </Space>
  </div>
}

function matchingApprovedOptions(items: SettleDiscountApproval[], amount: number) {
  return items.filter((item) => item.approvalStatus === 2 && roundMoney(item.discountAmount) === amount)
    .map((item) => ({ value: item.uuid, label: `${item.approveByName ?? '已批准'} · ¥${item.discountAmount.toFixed(2)}` }))
}

function requestIdFor(current: { signature: string; uuid: string }, signature: string): string {
  if (current.signature !== signature) {
    current.signature = signature
    current.uuid = crypto.randomUUID()
  }
  return current.uuid
}

function validateReason(values: ReceiveFormValues): Promise<void> {
  const error = discountReasonError(values)
  return error ? Promise.reject(new Error(error)) : Promise.resolve()
}
