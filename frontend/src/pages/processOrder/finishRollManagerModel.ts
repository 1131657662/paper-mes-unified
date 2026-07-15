import type { FinishRoll } from '../../types/processOrder'

export interface FinishRollFilters {
  source?: number
  spare?: number
  status?: number
}

export interface FinishRollStats {
  active: number
  actualWeight: number
  official: number
  spare: number
  total: number
  voided: number
}

export function filterFinishRolls(rolls: FinishRoll[], filters: FinishRollFilters): FinishRoll[] {
  return rolls.filter((roll) => {
    if (filters.status !== undefined && roll.rollNoStatus !== filters.status) return false
    if (filters.spare !== undefined && roll.isSpare !== filters.spare) return false
    if (filters.source !== undefined && roll.sourceType !== filters.source) return false
    return true
  })
}

export function finishRollStats(rolls: FinishRoll[]): FinishRollStats {
  const activeRolls = rolls.filter((roll) => roll.rollNoStatus !== 3)
  return {
    active: activeRolls.length,
    actualWeight: activeRolls.reduce((sum, roll) => sum + (roll.actualWeight ?? 0), 0),
    official: activeRolls.filter((roll) => roll.isSpare !== 1).length,
    spare: activeRolls.filter((roll) => roll.isSpare === 1).length,
    total: rolls.length,
    voided: rolls.filter((roll) => roll.rollNoStatus === 3).length,
  }
}

export function hasActiveFilters(filters: FinishRollFilters): boolean {
  return Object.values(filters).some((value) => value !== undefined)
}
