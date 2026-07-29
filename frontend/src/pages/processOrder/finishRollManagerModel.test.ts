import { describe, expect, it } from 'vitest'
import type { FinishRoll } from '../../types/processOrder'
import { filterFinishRolls } from './finishRollManagerModel'

const rolls: FinishRoll[] = [
  { uuid: 'planned', rollNoStatus: 1 },
  { uuid: 'used', rollNoStatus: 2 },
  { uuid: 'voided', rollNoStatus: 3 },
]

describe('filterFinishRolls', () => {
  it('hides voided numbers from the default operational view', () => {
    expect(filterFinishRolls(rolls, {}).map((roll) => roll.uuid)).toEqual(['planned', 'used'])
  })

  it('shows voided numbers when that status is explicitly selected', () => {
    expect(filterFinishRolls(rolls, { status: 3 }).map((roll) => roll.uuid)).toEqual(['voided'])
  })
})
