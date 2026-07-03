import { useMutation } from '@tanstack/react-query'
import { exportProcessOrderDetail } from '../../../api/processOrder'

export function useExportProcessOrder() {
  return useMutation({
    mutationFn: exportProcessOrderDetail,
  })
}
