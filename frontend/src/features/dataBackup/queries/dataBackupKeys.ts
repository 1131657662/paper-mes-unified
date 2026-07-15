import { createQueryKeys } from '@lukemorales/query-key-factory'
import { dataBackupService } from '../services/dataBackupService'

export const dataBackupKeys = createQueryKeys('dataBackup', {
  list: {
    queryKey: null,
    queryFn: dataBackupService.list,
  },
  status: {
    queryKey: null,
    queryFn: dataBackupService.status,
  },
  tasks: {
    queryKey: null,
    queryFn: dataBackupService.tasks,
  },
})
