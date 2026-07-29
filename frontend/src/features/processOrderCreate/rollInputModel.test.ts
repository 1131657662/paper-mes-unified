import { describe, expect, it } from 'vitest'
import { newRollDraft } from './draftMappers'
import { mergeImportedRolls } from './rollInputModel'

describe('mergeImportedRolls', () => {
  it('replaces the initial placeholder row with imported rows', () => {
    const imported = [newRollDraft({ paperName: 'white card', rollWeight: 500 })]

    expect(mergeImportedRolls([newRollDraft()], imported)).toEqual(imported)
  })

  it('keeps rows that contain user-entered data', () => {
    const existing = newRollDraft({ paperName: 'kraft' })
    const imported = newRollDraft({ paperName: 'white card' })

    expect(mergeImportedRolls([existing], [imported])).toEqual([existing, imported])
  })
})
