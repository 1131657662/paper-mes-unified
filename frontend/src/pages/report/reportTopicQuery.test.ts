import { describe, expect, it } from 'vitest'
import { reportTopicQuery } from './reportTopicQuery'

describe('reportTopicQuery', () => {
  it('drops hidden financial filters from production topics', () => {
    const query = reportTopicQuery({
      dateFrom: '2026-01-01', isInvoice: 1, settleType: 2, paperName: '白卡',
    })

    expect(query).toEqual({
      customerUuid: undefined, dateFrom: '2026-01-01', dateTo: undefined,
      machineUuid: undefined, mainStepType: undefined, metricReleaseUuid: undefined,
      orderStatus: undefined, paperName: '白卡', processMode: undefined,
    })
  })
})
