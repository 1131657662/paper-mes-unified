import { useMutation } from '@tanstack/react-query'
import { settleService } from '../services/settleService'

export function useExportSettle() {
  return useMutation({
    mutationFn: settleService.export,
  })
}
