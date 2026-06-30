import { useMutation } from '@tanstack/react-query'
import type { ReportQuery } from '../../../types/report'
import { reportService } from '../services/reportService'

export function useExportReport() {
  return useMutation({
    mutationFn: (query: ReportQuery) => reportService.export(query),
  })
}
