import { useState, type Key } from 'react'
import { Modal, message } from 'antd'
import { appendSpareRolls, batchGenerateFinishRolls, batchVoidFinishRolls, voidFinishRoll } from '../../api/processOrder'
import type { FinishRollBatchDTO, SpareRollAppendDTO, SpareRollBatchVoidDTO } from '../../types/processOrder'
import type { FinishRollFilters } from './finishRollManagerModel'

interface Options {
  onSuccess: () => void
  orderUuid: string
  refetch: () => Promise<unknown>
}

export function useFinishRollManagerSession(options: Options) {
  const [filters, setFilters] = useState<FinishRollFilters>({})
  const [selectedKeys, setSelectedKeys] = useState<Key[]>([])
  const [dialog, setDialog] = useState<'generate' | 'spare'>()
  const reload = async () => {
    setSelectedKeys([])
    await options.refetch()
    options.onSuccess()
  }
  return {
    appendSpare: (values: SpareRollAppendDTO) => appendSpare(options.orderUuid, values, reload, setDialog),
    dialog,
    filters,
    generate: (values: FinishRollBatchDTO) => generateRolls(options.orderUuid, values, reload, setDialog),
    selectedKeys,
    setDialog,
    setFilters,
    setSelectedKeys,
    voidSelected: () => confirmSelectedVoid(selectedKeys, reload),
    voidSingle: (uuid: string) => voidRoll(uuid, reload),
  }
}

async function generateRolls(orderUuid: string, values: FinishRollBatchDTO, reload: () => Promise<void>, close: (value: undefined) => void) {
  const rollNos = await batchGenerateFinishRolls(orderUuid, values)
  message.success(`已生成 ${rollNos.length} 个卷号`)
  close(undefined)
  await reload()
}

async function appendSpare(orderUuid: string, values: SpareRollAppendDTO, reload: () => Promise<void>, close: (value: undefined) => void) {
  const rollNos = await appendSpareRolls(orderUuid, values)
  message.success(`已追加 ${rollNos.length} 个备用号`)
  close(undefined)
  await reload()
}

async function voidRoll(uuid: string, reload: () => Promise<void>) {
  await voidFinishRoll(uuid)
  message.success('卷号已作废')
  await reload()
}

function confirmSelectedVoid(keys: Key[], reload: () => Promise<void>) {
  Modal.confirm({
    content: `已选择 ${keys.length} 个卷号，作废后不可恢复。`,
    okButtonProps: { danger: true },
    okText: '确认作废',
    title: '作废已选卷号',
    onOk: () => batchVoid(keys, reload),
  })
}

async function batchVoid(keys: Key[], reload: () => Promise<void>) {
  const dto: SpareRollBatchVoidDTO = { uuids: keys.map(String) }
  await batchVoidFinishRolls(dto)
  message.success(`已作废 ${keys.length} 个卷号`)
  await reload()
}
