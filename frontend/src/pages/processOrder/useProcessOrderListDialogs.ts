import { useState } from 'react'

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

  return {
    state: { diffOpen, diffUuid, manageRollOpen, manageRollUuid, printOpen, printState },
    openPrint: (state: PrintState) => {
      setPrintState(state)
      setPrintOpen(true)
    },
    openDiff: (uuid: string) => {
      setDiffUuid(uuid)
      setDiffOpen(true)
    },
    openManageRoll: (uuid: string) => {
      setManageRollUuid(uuid)
      setManageRollOpen(true)
    },
    closeDiff: () => setDiffOpen(false),
    closeManageRoll: () => setManageRollOpen(false),
    closePrint: () => setPrintOpen(false),
  }
}
