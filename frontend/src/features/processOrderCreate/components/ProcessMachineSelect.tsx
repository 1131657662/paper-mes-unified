import { Select, Space, Tag, Typography } from 'antd'
import type { Machine } from '../../../types/machine'
import { machinesForStep } from '../machineDefaults'

interface Props {
  mainStepType?: number
  machines: Machine[]
  value?: string
  onChange: (machineUuid?: string) => void
}

const typeLabel: Record<number, string> = {
  1: '锯纸',
  2: '复卷',
  3: '通用',
}

export default function ProcessMachineSelect({ mainStepType, machines, value, onChange }: Props) {
  const candidates = machinesForStep(mainStepType, machines)
  const single = candidates.length === 1
  const options = candidates.map((machine) => ({
    value: machine.uuid,
    label: machineLabel(machine),
  }))

  return (
    <Space wrap align="center">
      <Typography.Text strong>机台</Typography.Text>
      <Select
        allowClear={!single}
        disabled={!mainStepType || single}
        options={options}
        placeholder={mainStepType ? '请选择机台' : '先选择主工艺'}
        style={{ minWidth: 240 }}
        value={value}
        onChange={onChange}
      />
      {single && <Tag color="blue">唯一机台，已自动带出</Tag>}
      {!!mainStepType && candidates.length === 0 && <Typography.Text type="secondary">暂无匹配机台</Typography.Text>}
    </Space>
  )
}

function machineLabel(machine: Machine) {
  const code = machine.machineCode ? `${machine.machineCode} / ` : ''
  return `${code}${machine.machineName}（${typeLabel[machine.machineType ?? 0] ?? '未分类'}）`
}
