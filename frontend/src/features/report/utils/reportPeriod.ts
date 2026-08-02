import dayjs, { type Dayjs } from 'dayjs'

export type PeriodPresetKey = 'month' | 'previousMonth' | 'quarter' | 'year'

export function periodFor(key: PeriodPresetKey, today: Dayjs = dayjs()): [Dayjs, Dayjs] {
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
