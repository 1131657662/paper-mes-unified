export type BackRecordWorkspaceViewState = 'loading' | 'error' | 'empty' | 'ready'

interface WorkspaceQueryState {
  hasDetail: boolean
  isDetailError: boolean
  isLoadingDetail: boolean
}

export function getBackRecordWorkspaceViewState({
  hasDetail,
  isDetailError,
  isLoadingDetail,
}: WorkspaceQueryState): BackRecordWorkspaceViewState {
  if (isLoadingDetail) return 'loading'
  if (isDetailError) return 'error'
  if (hasDetail) return 'ready'
  return 'empty'
}
