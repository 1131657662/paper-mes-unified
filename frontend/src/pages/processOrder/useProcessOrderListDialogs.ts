import { useCallback, useMemo, useState } from 'react'

interface PrintState {
  uuid: string
  orderNo?: string
  printCount?: number
}

export function useProcessOrderListDialogs() {
  const [printState, setPrintState] = useState<PrintState | null>(null)
  const [printOpen, setPrintOpen] = useState(false)
  const [diffUuid, setDiffUuid] = useState<string | null>(null)
  const [diffOpen, setDiffOpen] = useState(false)
  const [manageRollUuid, setManageRollUuid] = useState<string | null>(null)
  const [manageRollOpen, setManageRollOpen] = useState(false)

  const openPrint = useCallback((state: PrintState) => {
      setPrintState(state)
      setPrintOpen(true)
  }, [])
  const openDiff = useCallback((uuid: string) => {
      setDiffUuid(uuid)
      setDiffOpen(true)
  }, [])
  const openManageRoll = useCallback((uuid: string) => {
      setManageRollUuid(uuid)
      setManageRollOpen(true)
  }, [])
  const closeDiff = useCallback(() => setDiffOpen(false), [])
  const closeManageRoll = useCallback(() => setManageRollOpen(false), [])
  const closePrint = useCallback(() => setPrintOpen(false), [])

  return useMemo(() => ({
    state: { diffOpen, diffUuid, manageRollOpen, manageRollUuid, printOpen, printState },
    openPrint,
    openDiff,
    openManageRoll,
    closeDiff,
    closeManageRoll,
    closePrint,
  }), [closeDiff, closeManageRoll, closePrint, diffOpen, diffUuid, manageRollOpen, manageRollUuid, openDiff, openManageRoll, openPrint, printOpen, printState])
}
