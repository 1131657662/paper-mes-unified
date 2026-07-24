import { describe, expect, it } from 'vitest'
import { parseReportUrlState, serializeReportUrlState } from './reportUrlState'

describe('report URL state', () => {
  it('round trips filters without encoding the topic in query parameters', () => {
    const params = serializeReportUrlState({
      dateFrom: '2026-01-01', dateTo: '2026-07-21', customerUuid: 'customer-1',
      paperName: '牛皮纸', mainStepType: 2, isInvoice: 1,
    })

    const state = parseReportUrlState(params)

    expect(state).toEqual({
      query: {
        metricReleaseUuid: undefined, dateFrom: '2026-01-01', dateTo: '2026-07-21',
        customerUuid: 'customer-1', paperName: '牛皮纸', mainStepType: 2,
        processMode: undefined, machineUuid: undefined, settleType: undefined,
        isInvoice: 1, orderStatus: undefined,
      },
    })
  })

  it('ignores invalid numeric filters', () => {
    const state = parseReportUrlState(new URLSearchParams('mainStepType=x'))

    expect(state.query.mainStepType).toBeUndefined()
  })
})
