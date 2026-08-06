import { CloseOutlined, SortAscendingOutlined } from '@ant-design/icons'
import { Button, Tooltip, Typography } from 'antd'
import './SortClearControl.css'

interface Props {
  activeCount: number
  onClear: () => void
}

export default function SortClearControl({ activeCount, onClear }: Props) {
  if (activeCount <= 0) return null

  return (
    <span className="mes-sort-clear-control" aria-label={`${activeCount} 列已排序`}>
      <SortAscendingOutlined className="mes-sort-clear-control__status-icon" />
      <Typography.Text className="mes-sort-clear-control__count">已排序 {activeCount} 列</Typography.Text>
      <Tooltip title="清除所有排序">
        <Button
          aria-label="清除所有排序"
          className="mes-sort-clear-control__button"
          icon={<CloseOutlined />}
          shape="circle"
          size="small"
          type="text"
          onClick={onClear}
        />
      </Tooltip>
    </span>
  )
}
