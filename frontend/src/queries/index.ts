import { mergeQueryKeys } from '@lukemorales/query-key-factory'
import { authKeys } from '../features/auth/queries/authKeys'
import { deliveryKeys } from '../features/delivery/queries/deliveryKeys'
import { deliveryCustomerSpecKeys } from '../features/deliveryCustomerSpec/deliveryCustomerSpecKeys'
import { dataBackupKeys } from '../features/dataBackup/queries/dataBackupKeys'
import { dataHealthKeys } from '../features/dataHealth/queries/dataHealthKeys'
import { dashboardKeys } from '../features/dashboard/queries/dashboardKeys'
import { notificationKeys } from '../features/notification/queries/notificationKeys'
import { exportTaskKeys } from '../features/exportTask/queries/exportTaskKeys'
import { createOrderKeys } from '../features/processOrderCreate/queries/createOrderKeys'
import { processOrderDetailKeys } from '../features/processOrderDetail/queries/processOrderDetailKeys'
import { finishCustomerSpecKeys } from '../features/processOrderCustomerSpec/customerSpecKeys'
import { processCatalogKeys } from '../features/processCatalog/queries/processCatalogKeys'
import { reportKeys } from '../features/report/queries/reportKeys'
import { reportAlertKeys } from '../features/reportAlert/queries/reportAlertKeys'
import { reportSubscriptionKeys } from '../features/reportSubscription/queries/reportSubscriptionKeys'
import { reportSavedViewKeys } from '../features/reportSavedView/queries/reportSavedViewKeys'
import { settleKeys } from '../features/settle/queries/settleKeys'
import { systemConfigKeys } from '../features/systemConfig/queries/systemConfigKeys'
import { userKeys } from '../features/user/queries/userKeys'

export const queries = mergeQueryKeys(
  authKeys,
  dashboardKeys,
  dataBackupKeys,
  dataHealthKeys,
  notificationKeys,
  exportTaskKeys,
  createOrderKeys,
  processOrderDetailKeys,
  finishCustomerSpecKeys,
  processCatalogKeys,
  deliveryKeys,
  deliveryCustomerSpecKeys,
  settleKeys,
  reportKeys,
  reportAlertKeys,
  reportSubscriptionKeys,
  reportSavedViewKeys,
  userKeys,
  systemConfigKeys,
)
