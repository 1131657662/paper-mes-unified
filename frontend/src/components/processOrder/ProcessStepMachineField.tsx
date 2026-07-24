import { Form, Select } from 'antd'
import type { Machine } from '../../types/machine'
import { machinesForStep } from '../../features/processOrderCreate/machineDefaults'

interface Props {
  required: boolean
  stepType?: number
  machines: Machine[]
  loading?: boolean
}

export default function ProcessStepMachineField({ required, stepType, machines, loading }: Props) {
  const candidates = machinesForStep(stepType, machines)
  return (
    <Form.Item label="机台 / 工位" name="machineUuid"
      rules={required ? [{ required: true, message: '请选择兼容的机台或工位' }] : undefined}>
      <Select
        allowClear={!required}
        showSearch
        loading={loading}
        disabled={!stepType || loading}
        optionFilterProp="label"
        placeholder={stepType ? '请选择兼容资源' : '先选择工序类型'}
        options={candidates.map((machine) => machineOption(machine, stepType))}
        notFoundContent={stepType ? '暂无兼容资源' : undefined}
      />
    </Form.Item>
  )
}

function machineOption(machine: Machine, stepType?: number) {
  const capability = machine.capabilities?.find((item) => item.stepType === stepType)
  const name = [machine.machineCode, machine.machineName].filter(Boolean).join(' / ')
  return {
    value: machine.uuid,
    label: capability?.defaultCapability ? `${name}（默认）` : name,
  }
}
