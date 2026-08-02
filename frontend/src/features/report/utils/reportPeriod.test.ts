import dayjs from 'dayjs'
import { describe, expect, it } from 'vitest'
import { periodFor } from './reportPeriod'

describe('report period presets', () => {
  it.each([
    ['2026-08-01', '2026-07-01', '2026-07-31'],
    ['2026-03-01', '2026-02-01', '2026-02-28'],
    ['2028-03-01', '2028-02-01', '2028-02-29'],
  ])('calculates previous month for %s', (today, expectedStart, expectedEnd) => {
    const [start, end] = periodFor('previousMonth', dayjs(today))

    expect(start.format('YYYY-MM-DD')).toBe(expectedStart)
    expect(end.format('YYYY-MM-DD')).toBe(expectedEnd)
  })
})
