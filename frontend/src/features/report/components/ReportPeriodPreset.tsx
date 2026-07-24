import { CalendarOutlined } from '@ant-design/icons'
import { Button, Dropdown, Tooltip } from 'antd'
import type { MenuProps } from 'antd'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'

type PeriodPresetKey = 'month' | 'previousMonth' | 'quarter' | 'year'

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

function periodFor(key: PeriodPresetKey): [Dayjs, Dayjs] {
  const today = dayjs()
  if (key === 'previousMonth') {
    const previousMonth = today.subtract(1, 'month')
    return [previousMonth.startOf('month'), previousMonth.endOf('month')]
  }
  if (key === 'quarter') {
    const quarterStartMonth = Math.floor(today.month() / 3) * 3
    return [today.month(quarterStartMonth).startOf('month'), today]
  }
  if (key === 'year') return [today.startOf('year'), today]
  return [today.startOf('month'), today]
}
