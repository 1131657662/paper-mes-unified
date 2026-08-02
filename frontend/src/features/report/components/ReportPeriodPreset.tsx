import { CalendarOutlined } from '@ant-design/icons'
import { Button, Dropdown, Tooltip } from 'antd'
import type { MenuProps } from 'antd'
import type { Dayjs } from 'dayjs'
import { periodFor, type PeriodPresetKey } from '../utils/reportPeriod'

interface Props {
  onApply: (period: [Dayjs, Dayjs]) => void
}

const items: MenuProps['items'] = [
  { key: 'month', label: '本月' },
  { key: 'previousMonth', label: '上月' },
  { key: 'quarter', label: '本季度' },
  { key: 'year', label: '本年' },
]

export default function ReportPeriodPreset({ onApply }: Props) {
  const selectPreset: MenuProps['onClick'] = ({ key }) => {
    onApply(periodFor(key as PeriodPresetKey))
  }

  return (
    <Tooltip title="快捷选择统计周期">
      <Dropdown menu={{ items, onClick: selectPreset }} placement="bottomLeft" trigger={['click']}>
        <Button className="report-filter__preset" aria-label="快捷选择统计周期"
          icon={<CalendarOutlined />} />
      </Dropdown>
    </Tooltip>
  )
}
