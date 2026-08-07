import { describe, expect, it } from 'vitest'
import type { ProcessOrderAppendSessionVO } from '../../types/processOrder'
import { appendOrderVersionForCommit, hasAppendOrderVersionChange } from './appendOrderVersion'

describe('appendOrderVersion', () => {
  it('detects a resumed session whose base order changed', () => {
    expect(hasAppendOrderVersionChange(session(8, 9))).toBe(true)
  })

  it('uses the current server order version for commit', () => {
    expect(appendOrderVersionForCommit(session(8, 9))).toBe(9)
  })

  it('rejects commit when the server returned no valid version', () => {
    expect(() => appendOrderVersionForCommit(session(undefined, undefined)))
      .toThrow('后端未返回有效的加工单版本')
  })
})

function session(baseOrderVersion?: number, currentOrderVersion?: number): ProcessOrderAppendSessionVO {
  return { sessionUuid: 'session-1', orderUuid: 'order-1', baseOrderVersion, currentOrderVersion }
}
