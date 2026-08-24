import { describe, expect, it } from 'vitest'
import { buildProcessAiDefaultNotices, PROCESS_AI_DEFAULT_IDS } from './processAiDefaultModel'

describe('processAiDefaultModel', () => {
  it('shows the fixed 3-inch rewind-core acknowledgement', () => {
    expect(buildProcessAiDefaultNotices([PROCESS_AI_DEFAULT_IDS.rewindFinishCore3Inch]))
      .toEqual([expect.objectContaining({
        id: 'REWIND_FINISH_CORE_3_INCH',
        message: '普通复卷未指定成品纸芯，系统默认按 3 英寸处理，请确认',
      })])
  })

  it('does not render unknown default identifiers as model text', () => {
    expect(buildProcessAiDefaultNotices(['UNKNOWN_DEFAULT'])).toEqual([])
  })
})
