import { mergeQueryKeys } from '@lukemorales/query-key-factory'
import { authKeys } from '../features/auth/queries/authKeys'
import { deliveryKeys } from '../features/delivery/queries/deliveryKeys'
import { dataBackupKeys } from '../features/dataBackup/queries/dataBackupKeys'
import { dataHealthKeys } from '../features/dataHealth/queries/dataHealthKeys'
import { dashboardKeys } from '../features/dashboard/queries/dashboardKeys'
import { notificationKeys } from '../features/notification/queries/notificationKeys'
import { createOrderKeys } from '../features/processOrderCreate/queries/createOrderKeys'
import { processOrderDetailKeys } from '../features/processOrderDetail/queries/processOrderDetailKeys'
import { reportKeys } from '../features/report/queries/reportKeys'
import { settleKeys } from '../features/settle/queries/settleKeys'
import { systemConfigKeys } from '../features/systemConfig/queries/systemConfigKeys'
import { userKeys } from '../features/user/queries/userKeys'

export const queries = mergeQueryKeys(
  authKeys,
  dashboardKeys,
  dataBackupKeys,
  dataHealthKeys,
  notificationKeys,
  createOrderKeys,
  processOrderDetailKeys,
  deliveryKeys,
  settleKeys,
  reportKeys,
  userKeys,
  systemConfigKeys,
)
