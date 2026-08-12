import { useState } from 'react'
import { message } from 'antd'
import type { OriginalRollRemarkDTO, ProcessOrderDetailVO, ProcessOrderRemarkDTO, RollProductionVO } from '../../../types/processOrder'
import { useUpdateOrderRemark } from '../hooks/useUpdateOrderRemark'
import { useUpdatePostProductionNote } from '../hooks/useUpdatePostProductionNote'
import { useUpdateRollRemark } from '../hooks/useUpdateRollRemark'

export interface OrderRemarkEditor {
  close: () => void
  loading: boolean
  open: boolean
  show: () => void
  submit: (values: ProcessOrderRemarkDTO) => Promise<void>
}

export interface RollRemarkEditor {
  close: () => void
  loading: boolean
  roll?: RollProductionVO
  show: (roll: RollProductionVO) => void
  submit: (values: OriginalRollRemarkDTO) => Promise<void>
}

export interface PostProductionNoteEditor {
  close: () => void
  loading: boolean
  open: boolean
  show: () => void
  submit: (values: { postProductionNote?: string }) => Promise<void>
}

export function useOrderRemarkEditor(detail?: ProcessOrderDetailVO): OrderRemarkEditor {
  const [open, setOpen] = useState(false)
  const mutation = useUpdateOrderRemark()
  const submit = async (values: ProcessOrderRemarkDTO) => {
    if (!detail?.order.uuid) return
    if (detail.order.version == null) return
    await mutation.mutateAsync({ orderUuid: detail.order.uuid, values: { ...values, expectedVersion: detail.order.version } })
    setOpen(false)
    message.success('主单备注已更新')
  }
  return { close: () => setOpen(false), loading: mutation.isPending, open, show: () => setOpen(true), submit }
}

export function useRollRemarkEditor(detail?: ProcessOrderDetailVO): RollRemarkEditor {
  const [roll, setRoll] = useState<RollProductionVO>()
  const mutation = useUpdateRollRemark()
  const submit = async (values: OriginalRollRemarkDTO) => {
    if (!detail?.order.uuid || !roll?.originalUuid) return
    await mutation.mutateAsync({ orderUuid: detail.order.uuid, rollUuid: roll.originalUuid, values })
    setRoll(undefined)
    message.success('原纸备注已更新')
  }
  return { close: () => setRoll(undefined), loading: mutation.isPending, roll, show: setRoll, submit }
}

export function usePostProductionNoteEditor(detail?: ProcessOrderDetailVO): PostProductionNoteEditor {
  const [open, setOpen] = useState(false)
  const mutation = useUpdatePostProductionNote()
  const submit = async (values: { postProductionNote?: string }) => {
    if (!detail?.order.uuid || detail.order.version == null) return
    await mutation.mutateAsync({
      orderUuid: detail.order.uuid,
      values: { expectedVersion: detail.order.version, postProductionNote: values.postProductionNote },
    })
    setOpen(false)
    message.success('后生产备注已更新')
  }
  return { close: () => setOpen(false), loading: mutation.isPending, open, show: () => setOpen(true), submit }
}
