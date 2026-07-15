import { ArrowDownOutlined, ArrowUpOutlined } from '@ant-design/icons'
import { Button, Select, Space, Tooltip } from 'antd'
import type { WorkbenchRollSortMode, WorkbenchRollSortPreference } from '../workbenchRollSort'

interface Props {
  checkedCount: number
  preference: WorkbenchRollSortPreference
  onClearSelection: () => void
  onSelectSameSpec: () => void
  onSortChange: (preference: WorkbenchRollSortPreference) => void
}

const SORT_OPTIONS = [
  { label: '录入顺序', value: 'original' },
  { label: '同规格归组', value: 'spec' },
  { label: '门幅', value: 'width' },
  { label: '件重', value: 'weight' },
] satisfies Array<{ label: string; value: WorkbenchRollSortMode }>

export default function WorkbenchRollToolbar(props: Props) {
  const directional = props.preference.mode === 'width' || props.preference.mode === 'weight'
  const toggleDirection = () => props.onSortChange({
    ...props.preference,
    direction: props.preference.direction === 'asc' ? 'desc' : 'asc',
  })

  return (
    <div className="workbench-roll-toolbar">
      <Space size="small">
        <Button size="small" onClick={props.onSelectSameSpec}>全选同规格</Button>
        <Button size="small" disabled={!props.checkedCount} onClick={props.onClearSelection}>全不选</Button>
      </Space>
      <Space.Compact className="workbench-roll-toolbar__sort">
        <Select
          aria-label="母卷排序方式"
          options={SORT_OPTIONS}
          size="small"
          value={props.preference.mode}
          onChange={(mode) => props.onSortChange({ ...props.preference, mode })}
        />
        <Tooltip title={props.preference.direction === 'asc' ? '升序' : '降序'}>
          <Button
            aria-label={props.preference.direction === 'asc' ? '当前升序，点击切换降序' : '当前降序，点击切换升序'}
            className={directional ? '' : 'workbench-roll-toolbar__direction--hidden'}
            disabled={!directional}
            icon={props.preference.direction === 'asc' ? <ArrowUpOutlined /> : <ArrowDownOutlined />}
            size="small"
            onClick={toggleDirection}
          />
        </Tooltip>
      </Space.Compact>
    </div>
  )
}
