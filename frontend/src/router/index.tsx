import { createBrowserRouter, Navigate } from 'react-router-dom'
import type { ReactNode } from 'react'
import AuthGuard from './AuthGuard'
import BasicLayout from '../layout/BasicLayout'
import CustomerList from '../pages/customer/CustomerList'
import CustomerFormPage from '../pages/customer/CustomerFormPage'
import CustomerDetailPage from '../pages/customer/CustomerDetailPage'
import PaperList from '../pages/paper/PaperList'
import PaperFormPage from '../pages/paper/PaperFormPage'
import PaperDetailPage from '../pages/paper/PaperDetailPage'
import MachineList from '../pages/machine/MachineList'
import MachineFormPage from '../pages/machine/MachineFormPage'
import MachineDetailPage from '../pages/machine/MachineDetailPage'
import WarehouseList from '../pages/warehouse/WarehouseList'
import WarehouseFormPage from '../pages/warehouse/WarehouseFormPage'
import WarehouseDetailPage from '../pages/warehouse/WarehouseDetailPage'
import ProcessOrderList from '../pages/processOrder/ProcessOrderList'
import CreateOrderPage from '../pages/processOrder/CreateOrderPage'
import ConfigFinishPage from '../pages/processOrder/ConfigFinishPage'
import OrderDetailPage from '../pages/processOrder/OrderDetailPage'
import BackRecordPage from '../pages/processOrder/BackRecordPage'
import DeliveryOrderList from '../pages/delivery/DeliveryOrderList'
import DeliveryCreatePage from '../pages/delivery/DeliveryCreatePage'
import DeliveryDetailPage from '../pages/delivery/DeliveryDetailPage'
import SettleOrderList from '../pages/settle/SettleOrderList'
import SettleCreatePage from '../pages/settle/SettleCreatePage'
import SettleDetailPage from '../pages/settle/SettleDetailPage'
import ReportPage from '../pages/report/ReportPage'
import OperationLogPage from '../pages/operationLog/OperationLogPage'
import LoginPage from '../pages/login/LoginPage'
import DashboardPage from '../pages/dashboard/DashboardPage'
import UserList from '../pages/user/UserList'
import UserFormPage from '../pages/user/UserFormPage'
import UserDetailPage from '../pages/user/UserDetailPage'
import SystemConfigPage from '../pages/systemConfig/SystemConfigPage'
import PermissionGuard from '../components/PermissionGuard'
import { PERMISSIONS } from '../constants/permissions'

function guarded(element: ReactNode, permissions: string[]) {
  return <PermissionGuard permissions={permissions}>{element}</PermissionGuard>
}

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  {
    path: '/',
    element: <AuthGuard />,
    children: [
      {
        element: <BasicLayout />,
        children: [
          { index: true, element: <Navigate to="/dashboard" replace /> },
          { path: 'dashboard', element: guarded(<DashboardPage />, [PERMISSIONS.reportView]) },
          { path: 'customers', element: guarded(<CustomerList />, [PERMISSIONS.baseView]) },
          { path: 'customers/create', element: guarded(<CustomerFormPage mode="create" />, [PERMISSIONS.baseManage]) },
          { path: 'customers/:uuid', element: guarded(<CustomerDetailPage />, [PERMISSIONS.baseView]) },
          { path: 'customers/:uuid/edit', element: guarded(<CustomerFormPage mode="edit" />, [PERMISSIONS.baseManage]) },
          { path: 'papers', element: guarded(<PaperList />, [PERMISSIONS.baseView]) },
          { path: 'papers/create', element: guarded(<PaperFormPage mode="create" />, [PERMISSIONS.baseManage]) },
          { path: 'papers/:uuid', element: guarded(<PaperDetailPage />, [PERMISSIONS.baseView]) },
          { path: 'papers/:uuid/edit', element: guarded(<PaperFormPage mode="edit" />, [PERMISSIONS.baseManage]) },
          { path: 'machines', element: guarded(<MachineList />, [PERMISSIONS.baseView]) },
          { path: 'machines/create', element: guarded(<MachineFormPage mode="create" />, [PERMISSIONS.baseManage]) },
          { path: 'machines/:uuid', element: guarded(<MachineDetailPage />, [PERMISSIONS.baseView]) },
          { path: 'machines/:uuid/edit', element: guarded(<MachineFormPage mode="edit" />, [PERMISSIONS.baseManage]) },
          { path: 'warehouses', element: guarded(<WarehouseList />, [PERMISSIONS.baseView]) },
          { path: 'warehouses/create', element: guarded(<WarehouseFormPage mode="create" />, [PERMISSIONS.baseManage]) },
          { path: 'warehouses/:uuid', element: guarded(<WarehouseDetailPage />, [PERMISSIONS.baseView]) },
          { path: 'warehouses/:uuid/edit', element: guarded(<WarehouseFormPage mode="edit" />, [PERMISSIONS.baseManage]) },
          { path: 'process-orders', element: guarded(<ProcessOrderList />, [PERMISSIONS.orderView]) },
          { path: 'process-orders/create', element: guarded(<CreateOrderPage />, [PERMISSIONS.orderCreate]) },
          { path: 'process-orders/:uuid', element: guarded(<OrderDetailPage />, [PERMISSIONS.orderView]) },
          { path: 'process-orders/:uuid/back-record', element: guarded(<BackRecordPage />, [PERMISSIONS.orderBackRecord]) },
          { path: 'process-orders/:uuid/config-finish', element: guarded(<ConfigFinishPage />, [PERMISSIONS.orderManage]) },
          { path: 'delivery-orders', element: guarded(<DeliveryOrderList />, [PERMISSIONS.deliveryView]) },
          { path: 'delivery-orders/create', element: guarded(<DeliveryCreatePage />, [PERMISSIONS.deliveryManage]) },
          { path: 'delivery-orders/:uuid', element: guarded(<DeliveryDetailPage />, [PERMISSIONS.deliveryView]) },
          { path: 'settle-orders', element: guarded(<SettleOrderList />, [PERMISSIONS.settleView]) },
          { path: 'settle-orders/create', element: guarded(<SettleCreatePage />, [PERMISSIONS.settleManage]) },
          { path: 'settle-orders/:uuid', element: guarded(<SettleDetailPage />, [PERMISSIONS.settleView]) },
          { path: 'reports', element: guarded(<ReportPage />, [PERMISSIONS.reportView]) },
          { path: 'users', element: guarded(<UserList />, [PERMISSIONS.userManage]) },
          { path: 'users/create', element: guarded(<UserFormPage mode="create" />, [PERMISSIONS.userManage]) },
          { path: 'users/:uuid', element: guarded(<UserDetailPage />, [PERMISSIONS.userManage]) },
          { path: 'users/:uuid/edit', element: guarded(<UserFormPage mode="edit" />, [PERMISSIONS.userManage]) },
          { path: 'system-config', element: guarded(<SystemConfigPage />, [PERMISSIONS.userManage]) },
          { path: 'operation-logs', element: guarded(<OperationLogPage />, [PERMISSIONS.systemAudit]) },
        ],
      },
    ],
  },
])
