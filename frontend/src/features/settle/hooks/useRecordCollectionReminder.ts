import { useMutation, useQueryClient } from '@tanstack/react-query'
import { settleKeys } from '../queries/settleKeys'
import { settleService } from '../services/settleService'

export function useRecordCollectionReminder() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: settleService.recordCollectionReminder,
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: settleKeys.list._def })
      queryClient.invalidateQueries({ queryKey: settleKeys.collectionSummary._def })
      queryClient.invalidateQueries({ queryKey: settleKeys.collectionReminders(variables.uuid).queryKey })
      queryClient.invalidateQueries({ queryKey: settleKeys.operationLogs(variables.uuid).queryKey })
    },
  })
}
