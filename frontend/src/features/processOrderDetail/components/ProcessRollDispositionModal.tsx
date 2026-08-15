import { Alert, Empty, Form, Modal, Space, message } from 'antd'
import { useRef, useState } from 'react'
import { useWarehouses } from '../../processOrderCreate/hooks/useReferenceData'
import type {
  ProcessOrderDetailVO,
  ProcessRollDispositionAction,
} from '../../../types/processOrder'
import { notifyErrorOnce } from '../../../api/request'
import { useDisposeProcessRoll } from '../hooks/useDisposeProcessRoll'
import { buildRollOptions } from './processRollDispositionOptions'
import {
  disposeSelectedRolls,
  eligibleRolls,
  normalizeRollSelection,
  successText,
  type ProcessRollDispositionFormValues,
} from './processRollDispositionSubmit'
import ProcessRollDispositionForm from './ProcessRollDispositionForm'
import './ProcessRollDispositionModal.css'

interface Props {
  detail: ProcessOrderDetailVO
  open: boolean
  onClose: () => void
  onSuccess: () => Promise<void> | void
}

export default function ProcessRollDispositionModal({ detail, open, onClose, onSuccess }: Props) {
  const [form] = Form.useForm<ProcessRollDispositionFormValues>()
  const [submittingBatch, setSubmittingBatch] = useState(false)
  const requestIds = useRef(new Map<string, string>())
  const warehousesQuery = useWarehouses()
  const mutation = useDisposeProcessRoll()
  const action = Form.useWatch('action', form)
  const selectedRollUuids = Form.useWatch('rollUuids', form) ?? []
  const rolls = eligibleRolls(detail.originalRolls)
  const rollOptions = buildRollOptions(rolls)
  const excludedCount = Math.max(detail.originalRolls.length - rolls.length, 0)
  const multipleSelection = action === 'CANCEL'

  const submit = async (values: ProcessRollDispositionFormValues) => {
    const rollUuids = normalizeRollSelection(values.action, values.rollUuids ?? [])
    if (!validateSelection(rollUuids, values.action)) return
    const version = detail.order.version
    if (version == null) {
      message.error('当前加工单缺少版本信息，请刷新后重试')
      return
    }
    let applied = 0
    setSubmittingBatch(true)
    try {
      await disposeSelectedRolls({
        rollUuids,
        orderUuid: detail.order.uuid,
        expectedOrderVersion: version,
        values: { ...values, rollUuids },
        dispose: mutation.mutateAsync,
        requestIdFor: (key) => stableRequestId(requestIds.current, key),
        onApplied: (count) => { applied = count },
      })
      requestIds.current.clear()
      message.success(successText(values.action, applied))
      onClose()
      await onSuccess()
    } catch (error) {
      if (applied > 0) {
        message.warning(`已完成 ${applied} 卷，剩余母卷未处理，请刷新后继续`)
        await onSuccess()
      } else {
        notifyErrorOnce(error, '母卷处置失败，请刷新加工单后重试')
      }
    } finally {
      setSubmittingBatch(false)
    }
  }

  return (
    <Modal
      className="process-roll-disposition-modal"
      title="处置未加工母卷"
      width="min(900px, calc(100vw - 24px))"
      centered
      open={open}
      destroyOnHidden
      okText="确认处置"
      cancelText="取消"
      confirmLoading={mutation.isPending || submittingBatch}
      okButtonProps={{ disabled: rolls.length === 0 }}
      onCancel={onClose}
      onOk={() => { void form.submit() }}
    >
      <Space direction="vertical" size="middle" style={{ width: '100%' }}>
        <Alert
          className="process-roll-disposition__intro"
          type="warning"
          showIcon
          message="处置会离开当前加工路线，成功后不可直接恢复"
          description="已回录、已出库、已结算或已被成品引用的母卷不能处置。取消加工支持多选；转直发和拆分代加工单需要逐卷处理。"
        />
        {rolls.length === 0 ? (
          <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="当前没有可处置的未加工母卷" />
        ) : (
          <ProcessRollDispositionForm
            detail={detail}
            form={form}
            rolls={rollOptions}
            excludedCount={excludedCount}
            multipleSelection={multipleSelection}
            selectedRollUuids={selectedRollUuids}
            warehouses={warehousesQuery.data?.records ?? []}
            onSubmit={submit}
          />
        )}
      </Space>
    </Modal>
  )
}

function stableRequestId(requestIds: Map<string, string>, key: string): string {
  const existing = requestIds.get(key)
  if (existing) return existing
  const created = crypto.randomUUID()
  requestIds.set(key, created)
  return created
}

function validateSelection(rollUuids: string[], action: ProcessRollDispositionAction): boolean {
  if (!rollUuids.length) {
    message.error('请至少选择 1 卷母卷')
    return false
  }
  if (action !== 'CANCEL' && rollUuids.length > 1) {
    message.warning('转直发和拆分代加工单一次只能处理 1 卷')
    return false
  }
  return true
}
