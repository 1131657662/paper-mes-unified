import { useMutation } from '@tanstack/react-query'
import { createOrderService } from '../services/createOrderService'

export function useImportPreview() {
  return useMutation({ mutationFn: createOrderService.importRolls })
}
