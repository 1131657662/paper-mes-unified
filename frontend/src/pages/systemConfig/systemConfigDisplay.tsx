import { Tag } from 'antd'
import type { ConfigStatus, ConfigValueType } from '../../types/systemConfig'

export const statusOptions: Array<{ label: string; value: ConfigStatus }> = [
  { label: '启用', value: 1 },
  { label: '停用', value: 0 },
]

export const valueTypeOptions: Array<{ label: string; value: ConfigValueType }> = [
  { label: '文本', value: 'string' },
  { label: '数字', value: 'number' },
  { label: '开关', value: 'boolean' },
]

export function statusTag(status?: number) {
  return (
    <Tag className="mes-data-tag" color={status === 1 ? 'green' : 'default'}>
      {status === 1 ? '启用' : '停用'}
    </Tag>
  )
}

export function builtInTag(builtIn?: number) {
  return (
    <Tag className="mes-data-tag" color={builtIn === 1 ? 'blue' : 'default'}>
      {builtIn === 1 ? '内置' : '自定义'}
    </Tag>
  )
}

export function valueTypeTag(valueType?: string) {
  const label = valueTypeOptions.find((item) => item.value === valueType)?.label ?? valueType ?? '-'
  const color = valueType === 'number' ? 'purple' : valueType === 'boolean' ? 'cyan' : 'default'
  return <Tag className="mes-data-tag" color={color}>{label}</Tag>
}
