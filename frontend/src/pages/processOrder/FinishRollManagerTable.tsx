import type { Key } from 'react'
import { Button, Popconfirm, Table, Tag } from 'antd'
import { DeleteOutlined } from '@ant-design/icons'
import type { ColumnsType } from 'antd/es/table'
import MesTooltip from '../../components/biz/MesTooltip'
import TooltipText from '../../components/biz/TooltipText'
import { FINISH_SOURCE_TYPE, FINISH_STATUS, ROLL_NO_STATUS } from '../../constants/processOrder'
import type { FinishRoll } from '../../types/processOrder'
import { formatGram, formatKg, formatMm } from '../../utils/numberFormatters'

interface Props {
  onSelectionChange: (keys: Key[]) => void
  onVoid: (uuid: string) => void
  rows: FinishRoll[]
  selectedKeys: Key[]
  settled: boolean
}

export default function FinishRollManagerTable(props: Props) {
  const rowSelection = {
    selectedRowKeys: props.selectedKeys,
    onChange: props.onSelectionChange,
    getCheckboxProps: (record: FinishRoll) => ({ disabled: props.settled || record.rollNoStatus !== 1 }),
  }
  return (
    <div className="finish-roll-manager__table mes-drawer-table">
      <Table
        columns={buildColumns(props.onVoid, props.settled)}
        dataSource={props.rows}
        locale={{ emptyText: '没有符合条件的成品卷号' }}
        pagination={false}
        rowKey="uuid"
        rowSelection={rowSelection}
        scroll={{ x: 800, y: 'calc(100vh - 370px)' }}
        size="small"
      />
    </div>
  )
}

function buildColumns(onVoid: (uuid: string) => void, settled: boolean): ColumnsType<FinishRoll> {
  return [
    { title: '成品卷号', dataIndex: 'finishRollNo', fixed: 'left', width: 140, render: (_, roll) => <RollIdentity roll={roll} /> },
    { title: '类型/来源', key: 'type', width: 116, render: (_, roll) => <RollType roll={roll} /> },
    { title: '成品规格', key: 'spec', width: 210, render: (_, roll) => <RollSpec roll={roll} /> },
    { title: '重量', key: 'weight', align: 'right', width: 160, render: (_, roll) => <RollWeight roll={roll} /> },
    { title: '成品状态', dataIndex: 'finishStatus', width: 100, render: (value) => {
      const status = FINISH_STATUS[value]
      return status ? <Tag color={status.color}>{status.text}</Tag> : '-'
    } },
    { title: '操作', key: 'action', fixed: 'right', width: 64, render: (_, roll) => <VoidAction onVoid={onVoid} roll={roll} settled={settled} /> },
  ]
}

function RollIdentity({ roll }: { roll: FinishRoll }) {
  const status = roll.rollNoStatus == null ? undefined : ROLL_NO_STATUS[roll.rollNoStatus]
  return (
    <div className={`finish-roll-cell${roll.rollNoStatus === 3 ? ' is-voided' : ''}`}>
      <strong>{roll.finishRollNo || '-'}</strong>
      <Tag color={status?.color}>{status?.text ?? '-'}</Tag>
    </div>
  )
}

function RollType({ roll }: { roll: FinishRoll }) {
  const source = FINISH_SOURCE_TYPE[roll.sourceType ?? 1]
  return (
    <div className="finish-roll-cell finish-roll-cell--inline">
      <Tag color={roll.isSpare === 1 ? 'orange' : undefined}>{roll.isSpare === 1 ? '备用' : '正式'}</Tag>
      <Tag color={source?.color}>{source?.text ?? '未知来源'}</Tag>
    </div>
  )
}

function RollSpec({ roll }: { roll: FinishRoll }) {
  return (
    <div className="finish-roll-cell">
      <TooltipText value={roll.paperName || '-'} />
      <span>{roll.gramWeight ? formatGram(roll.gramWeight) : '-'} / {roll.finishWidth ? formatMm(roll.finishWidth) : '-'}</span>
    </div>
  )
}

function RollWeight({ roll }: { roll: FinishRoll }) {
  return (
    <div className="finish-roll-cell">
      <span>预估 {formatKg(roll.estimateWeight)}</span>
      <span>实际 {roll.actualWeight == null ? '-' : formatKg(roll.actualWeight)}</span>
    </div>
  )
}

function VoidAction({ onVoid, roll, settled }: { onVoid: (uuid: string) => void; roll: FinishRoll; settled: boolean }) {
  if (settled || roll.rollNoStatus !== 1 || roll.sourceType === 2) return null
  return (
    <Popconfirm title="作废后不可恢复，是否继续？" okButtonProps={{ danger: true }} onConfirm={() => onVoid(roll.uuid)}>
      <MesTooltip title="作废卷号">
        <Button aria-label={`作废卷号 ${roll.finishRollNo ?? ''}`} danger icon={<DeleteOutlined />} size="small" type="text" />
      </MesTooltip>
    </Popconfirm>
  )
}
