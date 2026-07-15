import { useState } from 'react'
import type { FinishConfigSaveDTO, OriginalRoll } from '../../types/processOrder'

export function useConfigFinishSelection(rolls: OriginalRoll[]) {
  const [selectedIndex, setSelectedIndex] = useState(0)
  const [checkedUuids, setCheckedUuids] = useState<string[]>([])
  const [configs, setConfigs] = useState<Record<string, FinishConfigSaveDTO>>({})
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
  }
  const copyToChecked = (config: FinishConfigSaveDTO) => {
    setConfigs((current) => {
      const next = { ...current }
      checkedUuids.forEach((uuid) => { next[uuid] = config })
      return next
    })
  }

  return {
    checkedUuids,
    configs,
    copyToChecked,
    currentRoll,
    selectIndex,
    selectedIndex,
    toggleChecked,
    updateCurrentConfig,
  }
}
