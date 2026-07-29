import { useRef, useState } from 'react'
import { message } from 'antd'
import type { ServiceEditorStatus } from '../serviceStepEditorTypes'
import { notifyErrorOnce } from '../../../api/request'

interface Options {
  onDirtyChange: (dirty: boolean) => void
  onSynchronizeVersion: () => Promise<number>
  onWritePendingChange: (pending: boolean) => void
}

type CoordinatedAction = () => void | Promise<void>

export function useConfigStepServiceCoordinator(options: Options) {
  const state = useCoordinatorState(options.onDirtyChange, options.onWritePendingChange)
  const synchronizeLatest = options.onSynchronizeVersion
  const synchronizeVersion = async () => {
    const version = await synchronizeLatest()
    state.changeVersionSyncBlocked(false)
    return version
  }
  const commands = createCoordinatorCommands(state, synchronizeVersion)
  return {
    ...commands,
    changeStatus: state.changeStatus,
    changeVersionSyncBlocked: state.changeVersionSyncBlocked,
    changeWritePending: state.changeWritePending,
    status: state.status,
    synchronizeLatest,
    synchronizeVersion,
    versionSyncBlocked: state.versionSyncBlocked,
    writePending: state.writePending,
  }
}

function useCoordinatorState(
  onDirtyChange: Options['onDirtyChange'],
  onWritePendingChange: Options['onWritePendingChange'],
) {
  const [status, setStatus] = useState<ServiceEditorStatus>()
  const [versionSyncBlocked, setVersionSyncBlocked] = useState(false)
  const [writePending, setWritePending] = useState(false)
  const statusRef = useRef(status)
  const versionSyncBlockedRef = useRef(versionSyncBlocked)
  const writePendingRef = useRef(writePending)

  const changeStatus = (next?: ServiceEditorStatus) => {
    statusRef.current = next
    setStatus(next)
    onDirtyChange(next?.dirty === true || versionSyncBlockedRef.current)
  }
  const changeVersionSyncBlocked = (blocked: boolean) => {
    versionSyncBlockedRef.current = blocked
    setVersionSyncBlocked(blocked)
    onDirtyChange(blocked || statusRef.current?.dirty === true)
  }
  const changeWritePending = (pending: boolean) => {
    writePendingRef.current = pending
    setWritePending(pending)
    onWritePendingChange(pending)
  }
  return { changeStatus, changeVersionSyncBlocked, changeWritePending, status, statusRef,
    versionSyncBlocked, versionSyncBlockedRef, writePending, writePendingRef }
}

function createCoordinatorCommands(
  state: ReturnType<typeof useCoordinatorState>,
  synchronizeVersion: () => Promise<number>,
) {
  const runAfterVersionSync = async (action: CoordinatedAction) => {
    if (writeBlocked(state.writePendingRef.current)) return
    if (!await restoreVersionIfNeeded(state.versionSyncBlockedRef.current, synchronizeVersion)) return
    await executeAction(action)
  }
  const runNext = async (action: CoordinatedAction) => {
    if (writeBlocked(state.writePendingRef.current)) return
    if (!await restoreVersionIfNeeded(state.versionSyncBlockedRef.current, synchronizeVersion)) return
    if (state.statusRef.current?.dirty) {
      message.warning('当前卷附加工艺有未保存修改，请先保存、批量应用或还原修改')
      return
    }
    await executeAction(action)
  }
  const runSelection = async (action: CoordinatedAction) => {
    if (state.statusRef.current?.dirty) {
      message.warning('当前卷附加工艺有未保存修改，请先保存或还原后再切换母卷')
      return
    }
    await runAfterVersionSync(action)
  }

  return { runAfterVersionSync, runNext, runSelection }
}

function writeBlocked(pending: boolean) {
  if (!pending) return false
  message.info('附加工艺正在保存或删除，请稍候')
  return true
}

async function restoreVersionIfNeeded(
  blocked: boolean,
  synchronizeVersion: () => Promise<number>,
): Promise<boolean> {
  if (!blocked) return true
  try {
    await synchronizeVersion()
    return true
  } catch {
    return false
  }
}

async function executeAction(action: CoordinatedAction) {
  try {
    await action()
  } catch (error) {
    notifyErrorOnce(error, '操作失败，请重试')
  }
}
