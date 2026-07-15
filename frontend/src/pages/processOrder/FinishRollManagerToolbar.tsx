import { Button, Dropdown, Select, Space } from 'antd'
import { DeleteOutlined, MoreOutlined, PlusOutlined } from '@ant-design/icons'
import MesTooltip from '../../components/biz/MesTooltip'
import type { FinishRollFilters } from './finishRollManagerModel'

export interface FinishRollToolbarState {
  allDirectShip: boolean
  filteredCount: number
  filters: FinishRollFilters
  selectedCount: number
  settled: boolean
  totalCount: number
}

export interface FinishRollToolbarActions {
  onAppendSpare: () => void
  onBatchVoid: () => void
  onChangeFilters: (filters: FinishRollFilters) => void
  onGenerate: () => void
}

interface Props {
  actions: FinishRollToolbarActions
  state: FinishRollToolbarState
}

export default function FinishRollManagerToolbar({ actions, state }: Props) {
  const update = (key: keyof FinishRollFilters, value?: number) => {
    actions.onChangeFilters({ ...state.filters, [key]: value })
  }
  const moreItems = [{
    danger: true,
    disabled: state.selectedCount === 0 || state.settled,
    icon: <DeleteOutlined />,
    key: 'void-selected',
    label: `作废已选卷号 (${state.selectedCount})`,
    onClick: actions.onBatchVoid,
  }]
  return (
    <div className="finish-roll-toolbar">
      <div className="finish-roll-toolbar__actions">
        <MesTooltip title={state.allDirectShip ? '直发卷在回录时自动产出，无需预生成' : undefined}>
          <span><Button icon={<PlusOutlined />} type="primary" disabled={state.allDirectShip || state.settled} onClick={actions.onGenerate}>生成正式号</Button></span>
        </MesTooltip>
        <Button icon={<PlusOutlined />} disabled={state.settled} onClick={actions.onAppendSpare}>追加备用号</Button>
        <Dropdown menu={{ items: moreItems }} placement="bottomRight" trigger={['click']}>
          <Button icon={<MoreOutlined />}>更多操作</Button>
        </Dropdown>
      </div>
      <Space className="finish-roll-toolbar__filters" size={8}>
        <span>{state.filteredCount === state.totalCount ? `共 ${state.totalCount} 条` : `${state.filteredCount}/${state.totalCount} 条`}</span>
        <Select aria-label="卷号状态" allowClear placeholder="卷号状态" value={state.filters.status} onChange={(value) => update('status', value)} options={[{ value: 1, label: '预生成' }, { value: 2, label: '已使用' }, { value: 3, label: '已作废' }]} />
        <Select aria-label="卷号类型" allowClear placeholder="卷号类型" value={state.filters.spare} onChange={(value) => update('spare', value)} options={[{ value: 0, label: '正式' }, { value: 1, label: '备用' }]} />
        <Select aria-label="来源类型" allowClear placeholder="来源类型" value={state.filters.source} onChange={(value) => update('source', value)} options={[{ value: 1, label: '加工产出' }, { value: 2, label: '母卷直发' }]} />
      </Space>
    </div>
  )
}
