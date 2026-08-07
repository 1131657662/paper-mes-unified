import { EditOutlined, SendOutlined, StopOutlined } from '@ant-design/icons'
import { Alert, Button, Form, Input, InputNumber, Space, Tag, message } from 'antd'
import type { FormInstance } from 'antd'
import { useRef } from 'react'
import { useNavigate } from 'react-router'
import { PERMISSIONS } from '../../constants/permissions'
import { useRequestSettleDiscountApproval } from '../../features/settle/hooks/useRequestSettleDiscountApproval'
import { useSettleDiscountApprovalDecision } from '../../features/settle/hooks/useSettleDiscountApprovalDecision'
import { useHasPermission } from '../../stores/authStore'
import type { SettleDiscountApproval } from '../../types/settle'
import { approvalLevelText, approvalMatches, approvalStatusColor, approvalStatusText,
  DISCOUNT_APPROVAL_STATUS } from './discountApprovalModel'
import { discountReasonError, receiveTotalError, remainingAmount, resolveDiscountApprovalLevel, roundMoney,
  type DiscountApprovalSettings, type ReceiveFormValues } from './receiveFormModel'

interface Props {
  approval?: SettleDiscountApproval | null
  form: FormInstance<ReceiveFormValues>
  locked: boolean
  onModify: () => void
  onSubmitted: () => Promise<void>
  settings: DiscountApprovalSettings
  settleUuid: string
  unreceivedAmount: number
}

export default function ReceiveDiscountFields({ approval, form, locked, onModify, onSubmitted,
  settings, settleUuid, unreceivedAmount }: Props) {
  const navigate = useNavigate()
  const requestRef = useRef({ signature: '', uuid: crypto.randomUUID() })
  const cashAmount = roundMoney(Form.useWatch('cashAmount', form) ?? 0)
  const scrapOffsetAmount = roundMoney(Form.useWatch('scrapOffsetAmount', form) ?? 0)
  const discountAmount = roundMoney(Form.useWatch('discountAmount', form) ?? 0)
  const canDiscount = useHasPermission(PERMISSIONS.settleDiscount)
  const requestMutation = useRequestSettleDiscountApproval()
  const decisionMutation = useSettleDiscountApprovalDecision()
  const level = resolveDiscountApprovalLevel(discountAmount, unreceivedAmount, settings)
  const needsApproval = level !== 'DIRECT'
  const matches = approvalMatches(approval, form.getFieldsValue(), unreceivedAmount)
  const approved = matches && approval?.approvalStatus === DISCOUNT_APPROVAL_STATUS.approved
  const discountMax = remainingAmount(unreceivedAmount, { cashAmount, scrapOffsetAmount })

  async function submitApproval() {
    await form.validateFields(['discountAmount', 'discountReason'])
    const totalError = receiveTotalError({ cashAmount, scrapOffsetAmount, discountAmount }, unreceivedAmount)
    if (totalError) {
      message.error(totalError)
      return
    }
    const reason = form.getFieldValue('discountReason')?.trim() ?? ''
    const signature = [cashAmount, scrapOffsetAmount, discountAmount, unreceivedAmount, reason].join('|')
    const requestId = requestIdFor(requestRef.current, signature)
    await requestMutation.mutateAsync({
      uuid: settleUuid,
      data: { requestId, cashAmount, scrapOffsetAmount, discountAmount,
        unreceivedSnapshot: unreceivedAmount, reason },
    })
    requestRef.current = { signature: '', uuid: crypto.randomUUID() }
    await onSubmitted()
    message.success('优惠审批申请已提交')
  }

  async function cancelApproval() {
    if (!approval) return
    await decisionMutation.mutateAsync({
      uuid: approval.uuid,
      action: 'cancel',
      data: { reason: '申请人取消当前收款方案' },
    })
    form.setFieldValue('discountApprovalUuid', undefined)
    onModify()
    message.success('优惠审批申请已取消')
  }

  return <>
    <Form.Item name="discountAmount" label="优惠/尾差核销"
      tooltip="不计入实际到账，仅用于核销双方确认的优惠或尾差">
      <InputNumber disabled={!canDiscount || locked} style={{ width: '100%' }} min={0}
        max={discountMax} precision={2} />
    </Form.Item>
    <Form.Item name="discountReason" label="优惠原因" rules={[{
      validator: () => validateReason(form.getFieldsValue()),
    }]}>
      <Input.TextArea disabled={!canDiscount || discountAmount <= 0 || locked} rows={2} maxLength={255}
        placeholder="填写双方确认的优惠、抹零或尾差原因" />
    </Form.Item>
    <Form.Item name="discountApprovalUuid" hidden rules={[{
      validator: () => needsApproval && !approved
        ? Promise.reject(new Error('当前优惠必须先完成对应级别审批')) : Promise.resolve(),
    }]}><Input /></Form.Item>
    {needsApproval && <ApprovalState approval={approval} level={level} matches={matches} locked={locked}
      loading={requestMutation.isPending || decisionMutation.isPending}
      onCancel={() => void cancelApproval()} onModify={onModify}
      onOpenInbox={() => navigate('/settle-orders/discount-approvals?scope=mine')}
      onSubmit={() => void submitApproval()} />}
  </>
}

interface ApprovalStateProps {
  approval?: SettleDiscountApproval | null
  level: 'FINANCE' | 'ADMIN'
  loading: boolean
  locked: boolean
  matches: boolean
  onCancel: () => void
  onModify: () => void
  onOpenInbox: () => void
  onSubmit: () => void
}

function ApprovalState({ approval, level, loading, locked, matches, onCancel, onModify,
  onOpenInbox, onSubmit }: ApprovalStateProps) {
  const active = matches && approval
    && [DISCOUNT_APPROVAL_STATUS.pending, DISCOUNT_APPROVAL_STATUS.approved].includes(approval.approvalStatus as 1 | 2)
  const canSubmit = !active || !matches || !locked
  return <div className="receive-discount-approval">
    <Space wrap>
      <Tag color={level === 'ADMIN' ? 'volcano' : 'blue'}>{approvalLevelText(level)}</Tag>
      {approval && <Tag color={approvalStatusColor(approval.approvalStatus)}>
        {approvalStatusText(approval.approvalStatus)}
      </Tag>}
    </Space>
    <Alert showIcon type={alertType(approval, matches)} message={approvalMessage(approval, matches)}
      description={approval?.decisionReason || approval?.reason || approvalDescription(level)} />
    <Space wrap>
      {canSubmit && <Button icon={<SendOutlined />} loading={loading} onClick={onSubmit}>
        {approval ? '提交新方案' : '提交审批'}
      </Button>}
      {active && locked && <Button icon={<EditOutlined />} onClick={onModify}>修改方案</Button>}
      {active && <Button icon={<StopOutlined />} loading={loading} onClick={onCancel}>取消申请</Button>}
      <Button type="link" onClick={onOpenInbox}>查看审批记录</Button>
    </Space>
  </div>
}

function approvalMessage(approval: SettleDiscountApproval | null | undefined, matches: boolean): string {
  if (!approval) return '当前方案需要审批'
  if (!matches) return '当前输入与已有审批方案不一致'
  if (approval.approvalStatus === DISCOUNT_APPROVAL_STATUS.pending) return '审批申请已提交，等待另一账号处理'
  if (approval.approvalStatus === DISCOUNT_APPROVAL_STATUS.approved) return '审批已通过，可以登记收款'
  return `审批${approvalStatusText(approval.approvalStatus)}，请调整后重新提交`
}

function alertType(approval: SettleDiscountApproval | null | undefined, matches: boolean) {
  if (matches && approval?.approvalStatus === DISCOUNT_APPROVAL_STATUS.approved) return 'success' as const
  if (approval?.approvalStatus === DISCOUNT_APPROVAL_STATUS.rejected) return 'error' as const
  return 'warning' as const
}

function approvalDescription(level: 'FINANCE' | 'ADMIN'): string {
  return level === 'ADMIN'
    ? '提交后由另一管理员账号在优惠审批工作台处理。'
    : '提交后由另一财务或管理员账号在优惠审批工作台处理。'
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
