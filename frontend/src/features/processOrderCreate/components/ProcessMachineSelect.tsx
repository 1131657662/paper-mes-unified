import { Select, Space, Tag, Typography } from 'antd'
import type { Machine } from '../../../types/machine'
import { machinesForStep } from '../machineDefaults'

interface Props {
  mainStepType?: number
  machines: Machine[]
  diameter?: number
  weight?: number
  width?: number
  value?: string
  onChange: (machineUuid?: string) => void
}

const typeLabel: Record<number, string> = {
  1: '锯纸',
  2: '复卷',
  3: '通用',
}

export default function ProcessMachineSelect({ mainStepType, machines, diameter, weight, width, value, onChange }: Props) {
  const candidates = machinesForStep(mainStepType, machines, { diameter, weight, width })
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
      {single && <Tag color="blue">唯一兼容资源</Tag>}
      {selectedIsDefault(candidates, value, mainStepType) && <Tag color="green">工艺默认</Tag>}
      {!!mainStepType && candidates.length === 0 && <Typography.Text type="secondary">暂无匹配机台</Typography.Text>}
    </Space>
  )
}

function machineLabel(machine: Machine) {
  const code = machine.machineCode ? `${machine.machineCode} / ` : ''
  const capabilities = machine.capabilities?.map((item) => item.processName).join('、')
  return `${code}${machine.machineName}（${capabilities || typeLabel[machine.machineType ?? 0] || '未分类'}）`
}

function selectedIsDefault(machines: Machine[], machineUuid?: string, stepType?: number) {
  return machines.some((machine) => machine.uuid === machineUuid
    && machine.capabilities?.some((item) => item.stepType === stepType && item.defaultCapability))
}
