import { useState } from 'react'
import { message } from 'antd'
import type { ServiceEditorStatus } from '../serviceStepEditorTypes'

interface Options {
  onDirtyChange: (dirty: boolean) => void
  onSynchronizeVersion: () => Promise<void>
}

type CoordinatedAction = () => void | Promise<void>

export function useConfigStepServiceCoordinator(options: Options) {
  const [status, setStatus] = useState<ServiceEditorStatus>()
  const [versionSyncBlocked, setVersionSyncBlocked] = useState(false)
  const [writePending, setWritePending] = useState(false)

  const changeStatus = (next?: ServiceEditorStatus) => {
    setStatus(next)
    options.onDirtyChange(next?.dirty === true || versionSyncBlocked)
  }
  const changeVersionSyncBlocked = (blocked: boolean) => {
    setVersionSyncBlocked(blocked)
    options.onDirtyChange(blocked || status?.dirty === true)
  }
  const synchronizeVersion = async () => {
    await options.onSynchronizeVersion()
    changeVersionSyncBlocked(false)
  }
  const runAfterVersionSync = async (action: CoordinatedAction) => {
    if (writePending) {
      message.info('附加工艺正在保存或删除，请稍候')
      return
    }
    if (!await restoreVersionIfNeeded(versionSyncBlocked, synchronizeVersion)) return
    await action()
  }
  const runNext = async (action: CoordinatedAction) => {
    if (writePending) {
      message.info('附加工艺正在保存或删除，请稍候')
      return
    }
    if (!await restoreVersionIfNeeded(versionSyncBlocked, synchronizeVersion)) return
    if (status?.dirty) {
      message.warning('当前卷附加工艺有未保存修改，请先保存、批量应用或还原修改')
      return
    }
    await action()
  }

  return {
    changeStatus,
    changeVersionSyncBlocked,
    changeWritePending: setWritePending,
    runAfterVersionSync,
    runNext,
    status,
    synchronizeVersion,
    versionSyncBlocked,
    writePending,
  }
}

async function restoreVersionIfNeeded(
  blocked: boolean,
  synchronizeVersion: () => Promise<void>,
): Promise<boolean> {
  if (!blocked) return true
  try {
    await synchronizeVersion()
    return true
  } catch {
    return false
  }
}
