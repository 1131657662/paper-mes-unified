import { mergeQueryKeys } from '@lukemorales/query-key-factory'
import { deliveryKeys } from '../features/delivery/queries/deliveryKeys'
import { dashboardKeys } from '../features/dashboard/queries/dashboardKeys'
import { createOrderKeys } from '../features/processOrderCreate/queries/createOrderKeys'
import { processOrderDetailKeys } from '../features/processOrderDetail/queries/processOrderDetailKeys'
import { reportKeys } from '../features/report/queries/reportKeys'
import { settleKeys } from '../features/settle/queries/settleKeys'
import { systemConfigKeys } from '../features/systemConfig/queries/systemConfigKeys'
import { userKeys } from '../features/user/queries/userKeys'

export const queries = mergeQueryKeys(
  dashboardKeys,
  createOrderKeys,
  processOrderDetailKeys,
  deliveryKeys,
  settleKeys,
  reportKeys,
  userKeys,
  systemConfigKeys,
)
