import { useState } from 'react'
import type { FinishConfigSaveDTO, OriginalRoll } from '../../types/processOrder'

export function useConfigFinishSelection(rolls: OriginalRoll[]) {
  const [selectedIndex, setSelectedIndex] = useState(0)
  const [checkedUuids, setCheckedUuids] = useState<string[]>([])
  const [configs, setConfigs] = useState<Record<string, FinishConfigSaveDTO>>({})
  const [dirtyUuids, setDirtyUuids] = useState<string[]>([])
  const currentRoll = rolls[selectedIndex]

  const selectIndex = (index: number) => {
    if (index < 0 || index >= rolls.length) return
    setSelectedIndex(index)
  }
  const toggleChecked = (uuid: string, checked: boolean) => {
    setCheckedUuids((current) => checked
      ? Array.from(new Set([...current, uuid]))
      : current.filter((item) => item !== uuid))
  }
  const updateCurrentConfig = (config: FinishConfigSaveDTO) => {
    if (!currentRoll) return
    setConfigs((current) => ({ ...current, [currentRoll.uuid]: config }))
    setDirtyUuids((current) => addDirtyUuids(current, [currentRoll.uuid]))
  }
  const copyToChecked = (config: FinishConfigSaveDTO) => {
    setConfigs((current) => {
      const next = { ...current }
      checkedUuids.forEach((uuid) => { next[uuid] = config })
      return next
    })
    setDirtyUuids((current) => addDirtyUuids(current, checkedUuids))
  }
  const clearDirty = (uuids: string[]) => setDirtyUuids((current) => removeDirtyUuids(current, uuids))
  const clearAllDirty = () => setDirtyUuids([])

  return {
    checkedUuids,
    clearAllDirty,
    clearDirty,
    configs,
    copyToChecked,
    currentRoll,
    dirtyUuids,
    selectIndex,
    selectedIndex,
    toggleChecked,
    updateCurrentConfig,
  }
}

export function addDirtyUuids(current: string[], additions: string[]) {
  return Array.from(new Set([...current, ...additions]))
}

export function removeDirtyUuids(current: string[], removals: string[]) {
  const removed = new Set(removals)
  return current.filter((uuid) => !removed.has(uuid))
}
