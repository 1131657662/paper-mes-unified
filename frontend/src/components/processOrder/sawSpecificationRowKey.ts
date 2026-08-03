import type { FinishConfigSpecDTO } from '../../types/processOrder'

export function sawSpecificationRowKey(_spec: FinishConfigSpecDTO, index?: number): string {
  return `saw-spec-${index ?? 'unknown'}`
}
