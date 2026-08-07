import { Alert, Form, Modal, message } from 'antd'
import dayjs from 'dayjs'
import { useEffect, useRef, useState } from 'react'
import { useLatestSettleDiscountApproval } from '../../features/settle/hooks/useLatestSettleDiscountApproval'
import { useReceiveSettle } from '../../features/settle/hooks/useReceiveSettle'
import { useNumberConfigValue } from '../../features/systemConfig/hooks/useSystemConfigValue'
import { useAuthUser } from '../../stores/authStore'
import ReceiveAmountFields from './ReceiveAmountFields'
import ReceivePaymentFields from './ReceivePaymentFields'
import ReceiveDiscountFields from './ReceiveDiscountFields'
import { approvalMatches, DISCOUNT_APPROVAL_STATUS, isRestorableApproval,
  shouldLockApproval } from './discountApprovalModel'
import {
  buildReceiveDTO,
  receiveTotalError,
  resolveDiscountApprovalLevel,
  resolveReceiveAmountChange,
  roundMoney,
  type ReceiveAmountBaseline,
  type ReceiveFormValues,
} from './receiveFormModel'

interface Props {
  settleUuid: string | null
  unreceivedAmount: number
  open: boolean
  onClose: () => void
  onSuccess: () => void
}

export default function ReceiveModal({
  settleUuid,
  unreceivedAmount,
  open,
  onClose,
  onSuccess,
}: Props) {
  const [form] = Form.useForm<ReceiveFormValues>()
  const requestIdRef = useRef(crypto.randomUUID())
  const sessionBaselineRef = useRef<ReceiveAmountBaseline | null>(null)
  const restoredApprovalRef = useRef('')
  const submittingRef = useRef(false)
  const [editingApproval, setEditingApproval] = useState(false)
  const { mutateAsync: receiveSettle, isPending: isReceiving } = useReceiveSettle()
  const approvalQuery = useLatestSettleDiscountApproval(settleUuid ?? '', open)
  const { value: autoApproveLimit } = useNumberConfigValue('settle.discountAutoApproveLimit', 1)
  const { value: maxAmount } = useNumberConfigValue('settle.discountMaxAmount', 500)
  const { value: maxPercent } = useNumberConfigValue('settle.discountMaxPercent', 10)
  const user = useAuthUser()
  const usableUnreceivedAmount = roundMoney(unreceivedAmount)
  const operatorName = user?.realName ?? user?.username ?? '当前登录账号'
  const watchedValues: ReceiveFormValues = {
    cashAmount: Form.useWatch('cashAmount', form),
    scrapOffsetAmount: Form.useWatch('scrapOffsetAmount', form),
    discountAmount: Form.useWatch('discountAmount', form),
    discountReason: Form.useWatch('discountReason', form),
  }
  const discountSettings = { autoApproveLimit, maxAmount, maxPercent }
  const latestApproval = approvalQuery.data
  const approvalMatchesForm = approvalMatches(latestApproval, watchedValues, usableUnreceivedAmount)
  const approvalLevel = resolveDiscountApprovalLevel(
    roundMoney(watchedValues.discountAmount), usableUnreceivedAmount, discountSettings)
  const approvalLocked = shouldLockApproval(
    latestApproval, approvalMatchesForm, editingApproval, approvalLevel !== 'DIRECT')
  const amountChange = resolveReceiveAmountChange({
    baseline: sessionBaselineRef.current,
    settleUuid,
    unreceivedAmount: usableUnreceivedAmount,
  })

  useEffect(() => {
    if (!open || !settleUuid) {
      sessionBaselineRef.current = null
      restoredApprovalRef.current = ''
      return
    }
    if (sessionBaselineRef.current?.settleUuid === settleUuid) return
    form.setFieldsValue({
      cashAmount: usableUnreceivedAmount > 0 ? usableUnreceivedAmount : undefined,
      scrapOffsetAmount: 0,
      discountAmount: 0,
      scrapWeight: undefined,
      payMethod: 2,
      receiveDate: dayjs(),
    })
    sessionBaselineRef.current = { settleUuid, unreceivedAmount: usableUnreceivedAmount }
  }, [form, open, settleUuid, usableUnreceivedAmount])

  useEffect(() => {
    if (!open || !isRestorableApproval(latestApproval) || editingApproval) return
    const restoreKey = `${latestApproval.uuid}:${latestApproval.approvalStatus}`
    if (restoredApprovalRef.current === restoreKey) return
    const fields: (keyof ReceiveFormValues)[] = [
      'cashAmount', 'scrapOffsetAmount', 'discountAmount', 'discountReason',
    ]
    const firstRestore = restoredApprovalRef.current === ''
    if (firstRestore && form.isFieldsTouched(fields)) return
    form.setFieldsValue({
      cashAmount: latestApproval.cashAmount,
      scrapOffsetAmount: latestApproval.scrapOffsetAmount,
      discountAmount: latestApproval.discountAmount,
      discountReason: latestApproval.reason,
      discountApprovalUuid: latestApproval.approvalStatus === DISCOUNT_APPROVAL_STATUS.approved
        ? latestApproval.uuid : undefined,
    })
    restoredApprovalRef.current = restoreKey
  }, [editingApproval, form, latestApproval, open])

  const handleClose = () => {
    form.resetFields()
    sessionBaselineRef.current = null
    restoredApprovalRef.current = ''
    setEditingApproval(false)
    requestIdRef.current = crypto.randomUUID()
    onClose()
  }

  const handleSubmit = async () => {
    if (submittingRef.current) return
    submittingRef.current = true
    try {
      const values = await form.validateFields()
      if (!settleUuid) {
        message.error('结算单 UUID 不能为空')
        return
      }
      const totalError = receiveTotalError(values, usableUnreceivedAmount)
      if (totalError) {
        message.error(totalError)
        return
      }
      await receiveSettle({ uuid: settleUuid, data: buildReceiveDTO(values, requestIdRef.current) })
      message.success('收款登记成功')
      form.resetFields()
      sessionBaselineRef.current = null
      restoredApprovalRef.current = ''
      setEditingApproval(false)
      requestIdRef.current = crypto.randomUUID()
      onSuccess()
    } finally {
      submittingRef.current = false
    }
  }

  return (
    <Modal
      title="登记收款"
      open={open}
      onOk={handleSubmit}
      onCancel={handleClose}
      confirmLoading={isReceiving}
      okText="确认登记"
      cancelText="取消"
      destroyOnHidden
    >
      <Form
        key={settleUuid ?? 'no-settle'}
        className="mes-modal-form"
        form={form}
        layout="vertical"
      >
        {amountChange && (
          <Form.Item>
            <Alert
              showIcon
              type="warning"
              message="未收金额已更新"
              description={`当前未收金额已从 ¥${amountChange.previousAmount.toFixed(2)} 变为 ¥${amountChange.currentAmount.toFixed(2)}，已保留本次输入，请核对后再登记。`}
            />
          </Form.Item>
        )}
        {approvalQuery.isError && (
          <Alert showIcon type="error" message="优惠审批状态加载失败"
            description="暂时不能确认已有审批是否仍有效，请刷新后再登记收款。" />
        )}
        <ReceiveAmountFields disabled={approvalLocked} form={form} unreceivedAmount={usableUnreceivedAmount} />
        <ReceiveDiscountFields approval={latestApproval} form={form} locked={approvalLocked}
          onModify={() => {
            setEditingApproval(true)
            form.setFieldValue('discountApprovalUuid', undefined)
          }} onSubmitted={async () => {
            await approvalQuery.refetch()
            restoredApprovalRef.current = ''
            setEditingApproval(false)
          }} settings={discountSettings} settleUuid={settleUuid ?? ''}
          unreceivedAmount={usableUnreceivedAmount} />
        <ReceivePaymentFields form={form} operatorName={operatorName} />
      </Form>
    </Modal>
  )
}
