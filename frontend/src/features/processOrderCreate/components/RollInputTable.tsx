import { Button, Input, InputNumber, Select, Space } from 'antd'
import { CopyOutlined, DeleteOutlined } from '@ant-design/icons'
import type { ColumnType } from 'antd/es/table'
import ResizableTable from '../../../components/ResizableTable'
import MesTooltip from '../../../components/biz/MesTooltip'
import { MAX_SOURCE_PIECES } from '../../../constants/processOrder'
import type { RollDraft } from '../types'
import { newRollDraft } from '../draftMappers'
import { updateRollWeightStatus } from '../rollWeightStatus'

interface Props {
  onChange: (rolls: RollDraft[]) => void
  rolls: RollDraft[]
}

export default function RollInputTable({ onChange, rolls }: Props) {
  const setField = <K extends keyof RollDraft>(roll: RollDraft, key: K, value: RollDraft[K]) => {
    onChange(rolls.map((item) => item.localId === roll.localId ? { ...item, [key]: value } : item))
  }
  const columns: ColumnType<RollDraft>[] = [
    textColumn('品名', 'paperName', 130, setField),
    numberColumn('克重(g)', 'gramWeight', 90, setField),
    numberColumn('门幅(mm)', 'originalWidth', 132, setField, 'roll-width-input'),
    optionalNumberColumn('直径(in)', 'originalDiameter', 95, setField),
    optionalNumberColumn('纸芯(in)', 'coreDiameter', 90, setField),
    textColumn('母卷号', 'rollNo', 120, setField),
    textColumn('编号', 'extraNo', 110, setField),
    numberColumn('件数', 'pieceNum', 80, setField, undefined, MAX_SOURCE_PIECES),
    weightColumn(rolls, onChange),
    textColumn('备注', 'remark', 140, setField),
    actionColumn(rolls, onChange),
  ]
  return (
    <ResizableTable storageKey="unified_order_rolls" rowKey="localId" size="small"
      pagination={false} columns={columns} dataSource={rolls} />
  )
}

type SetField = <K extends keyof RollDraft>(roll: RollDraft, key: K, value: RollDraft[K]) => void

function textColumn(key: string, field: keyof RollDraft, width: number, setField: SetField): ColumnType<RollDraft> {
  return { title: key, dataIndex: field, width, render: (_, roll, index) => (
    <Input aria-label={`母卷 ${index + 1} ${key}`} value={String(roll[field] ?? '')}
      onChange={(event) => setField(roll, field, event.target.value)} />
  ) }
}

function numberColumn(key: string, field: 'gramWeight' | 'originalWidth' | 'pieceNum', width: number, setField: SetField, className?: string, max?: number): ColumnType<RollDraft> {
  return { title: key, dataIndex: field, width, minWidth: width, render: (_, roll, index) => (
    <InputNumber className={className} aria-label={`母卷 ${index + 1} ${key}`} min={1}
      max={max}
      value={positiveValue(roll[field])} onChange={(value) => setField(roll, field, value ?? 0)} />
  ) }
}

function optionalNumberColumn(key: string, field: 'originalDiameter' | 'coreDiameter', width: number, setField: SetField): ColumnType<RollDraft> {
  return { title: key, dataIndex: field, width, render: (_, roll, index) => (
    <InputNumber aria-label={`母卷 ${index + 1} ${key}`} min={0}
      value={roll[field]} onChange={(value) => setField(roll, field, value ?? undefined)} />
  ) }
}

function weightColumn(rolls: RollDraft[], onChange: Props['onChange']): ColumnType<RollDraft> {
  return { title: '重量', dataIndex: 'rollWeight', width: 260, render: (_, roll, index) => (
    <Space.Compact>
      <Select aria-label={`母卷 ${index + 1} 重量状态`} value={roll.weightStatus ?? (roll.rollWeight == null ? 'UNKNOWN' : 'ESTIMATED')}
        options={[{ value: 'UNKNOWN', label: '未知（无参考）' }, { value: 'ESTIMATED', label: '参考（未实测）' }]}
        onChange={(value) => {
          if (value === 'MEASURED') return
          onChange(updateRollWeightStatus(rolls, roll.localId, value))
        }} />
      <InputNumber aria-label={`母卷 ${index + 1} 单重`} min={0.001} precision={3}
        disabled={roll.weightStatus === 'UNKNOWN'} value={positiveValue(roll.rollWeight)}
        placeholder={roll.weightStatus === 'UNKNOWN' ? '未知' : 'kg'}
        onChange={(value) => onChange(rolls.map((item) => item.localId === roll.localId
          ? { ...item, rollWeight: value ?? undefined }
          : item))} />
    </Space.Compact>
  ) }
}

function actionColumn(rolls: RollDraft[], onChange: Props['onChange']): ColumnType<RollDraft> {
  return { title: '操作', key: 'action', width: 88, render: (_, roll) => (
    <Space>
      <MesTooltip title="复制原纸"><Button aria-label="复制原纸" icon={<CopyOutlined />} size="small"
        onClick={() => onChange([...rolls, newRollDraft(roll)])} /></MesTooltip>
      <MesTooltip title={rolls.length <= 1 ? '至少保留一条原纸' : '删除原纸'}>
        <Button danger aria-label="删除原纸" icon={<DeleteOutlined />} size="small" disabled={rolls.length <= 1}
          onClick={() => onChange(rolls.filter((item) => item.localId !== roll.localId))} />
      </MesTooltip>
    </Space>
  ) }
}

function positiveValue(value?: number): number | undefined {
  return value != null && value > 0 ? value : undefined
}
