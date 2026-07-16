import { describe, expect, it } from 'vitest'
import { formatCsvCell } from './exportCsv'

describe('formatCsvCell', () => {
  it.each(['=SUM(A1:A2)', '+cmd', '-1+2', '@payload', '  =1+1'])(
    'neutralizes formula-like string %s',
    (value) => {
      expect(formatCsvCell(value)).toBe(`"'${value}"`)
    },
  )

  it('keeps numeric negative values numeric', () => {
    expect(formatCsvCell(-12.5)).toBe('"-12.5"')
  })

  it('escapes embedded quotes', () => {
    expect(formatCsvCell('a"b')).toBe('"a""b"')
  })
})
