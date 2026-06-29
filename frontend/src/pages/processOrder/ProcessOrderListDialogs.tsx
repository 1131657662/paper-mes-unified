import FinishRollManageDrawer from './FinishRollManageDrawer'
import PrintModal from './PrintModal'
import SnapshotDiffModal from './SnapshotDiffModal'

interface PrintState {
  uuid: string
  orderNo?: string
  printCount?: number
}

interface DialogState {
  diffOpen: boolean
  diffUuid: string | null
  manageRollOpen: boolean
  manageRollUuid: string | null
  printOpen: boolean
  printState: PrintState | null
}

interface DialogActions {
  onCloseDiff: () => void
  onCloseManageRoll: () => void
  onClosePrint: () => void
  onRefresh: () => void
}

interface Props {
  state: DialogState
  actions: DialogActions
}

export default function ProcessOrderListDialogs({
  state,
  actions,
}: Props) {
  return (
    <>
      <PrintModal
        uuid={state.printState?.uuid ?? null}
        orderNo={state.printState?.orderNo}
        printCount={state.printState?.printCount}
        open={state.printOpen}
        onClose={actions.onClosePrint}
        onSuccess={actions.onRefresh}
      />
      <SnapshotDiffModal uuid={state.diffUuid} open={state.diffOpen} onClose={actions.onCloseDiff} />
      <FinishRollManageDrawer
        orderUuid={state.manageRollUuid}
        open={state.manageRollOpen}
        onClose={actions.onCloseManageRoll}
        onSuccess={actions.onRefresh}
      />
    </>
  )
}
