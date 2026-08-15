import { describe, expect, it } from 'vitest'
import { parseProjectMemoryOperations } from './projectMemoryPatchParser'

describe('parseProjectMemoryOperations', () => {
  it('accepts the supported RFC 6902 subset', () => {
    const operations = parseProjectMemoryOperations(JSON.stringify([
      { op: 'replace', path: '/rules/rule-1/status', value: 'ACTIVE' },
      { op: 'remove', path: '/terms/term-1/aliases/0' },
    ]))

    expect(operations).toEqual([
      { op: 'replace', path: '/rules/rule-1/status', value: 'ACTIVE' },
      { op: 'remove', path: '/terms/term-1/aliases/0' },
    ])
  })

  it.each([
    ['[]', '1 到 20'],
    ['[{"op":"move","path":"/rules/r1/status"}]', 'op 仅支持'],
    ['[{"op":"replace","path":"/sources/local","value":"x"}]', '允许范围'],
    ['[{"op":"remove","path":"/rules/r1/status","value":"x"}]', '不能提供'],
    ['[{"op":"remove","path":"/rules/r1/status/"}]', '允许范围'],
    ['[{"op":"replace","path":"/rules/r1/status"}]', '必须提供'],
  ])('rejects invalid patch input', (text, expected) => {
    expect(() => parseProjectMemoryOperations(text)).toThrow(expected)
  })
})
