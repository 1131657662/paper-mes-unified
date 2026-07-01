import { lazy, Suspense } from 'react'
import type { ReactNode } from 'react'
import { Spin } from 'antd'
import { createBrowserRouter, Navigate } from 'react-router-dom'
import AuthGuard from './AuthGuard'
import BasicLayout from '../layout/BasicLayout'
import PermissionGuard from '../components/PermissionGuard'
import { PERMISSIONS } from '../constants/permissions'

const BackRecordPage = lazy(() => import('../pages/processOrder/BackRecordPage'))
const ConfigFinishPage = lazy(() => import('../pages/processOrder/ConfigFinishPage'))
const CreateOrderPage = lazy(() => import('../pages/processOrder/CreateOrderPage'))
const CustomerDetailPage = lazy(() => import('../pages/customer/CustomerDetailPage'))
const CustomerFormPage = lazy(() => import('../pages/customer/CustomerFormPage'))
const CustomerList = lazy(() => import('../pages/customer/CustomerList'))
const DashboardPage = lazy(() => import('../pages/dashboard/DashboardPage'))
const DeliveryCreatePage = lazy(() => import('../pages/delivery/DeliveryCreatePage'))
const DeliveryDetailPage = lazy(() => import('../pages/delivery/DeliveryDetailPage'))
const DeliveryOrderList = lazy(() => import('../pages/delivery/DeliveryOrderList'))
const LoginPage = lazy(() => import('../pages/login/LoginPage'))
const MachineDetailPage = lazy(() => import('../pages/machine/MachineDetailPage'))
const MachineFormPage = lazy(() => import('../pages/machine/MachineFormPage'))
const MachineList = lazy(() => import('../pages/machine/MachineList'))
const OperationLogPage = lazy(() => import('../pages/operationLog/OperationLogPage'))
const OrderDetailPage = lazy(() => import('../pages/processOrder/OrderDetailPage'))
const PaperDetailPage = lazy(() => import('../pages/paper/PaperDetailPage'))
const PaperFormPage = lazy(() => import('../pages/paper/PaperFormPage'))
const PaperList = lazy(() => import('../pages/paper/PaperList'))
const ProcessOrderList = lazy(() => import('../pages/processOrder/ProcessOrderList'))
const ReportPage = lazy(() => import('../pages/report/ReportPage'))
const SettleCreatePage = lazy(() => import('../pages/settle/SettleCreatePage'))
const SettleDetailPage = lazy(() => import('../pages/settle/SettleDetailPage'))
const SettleOrderList = lazy(() => import('../pages/settle/SettleOrderList'))
const SystemConfigPage = lazy(() => import('../pages/systemConfig/SystemConfigPage'))
const UserDetailPage = lazy(() => import('../pages/user/UserDetailPage'))
const UserFormPage = lazy(() => import('../pages/user/UserFormPage'))
const UserList = lazy(() => import('../pages/user/UserList'))
const WarehouseDetailPage = lazy(() => import('../pages/warehouse/WarehouseDetailPage'))
const WarehouseFormPage = lazy(() => import('../pages/warehouse/WarehouseFormPage'))
const WarehouseList = lazy(() => import('../pages/warehouse/WarehouseList'))

function guarded(element: ReactNode, permissions: string[]) {
  return <PermissionGuard permissions={permissions}>{element}</PermissionGuard>
}

function lazyPage(element: ReactNode) {
  return (
    <Suspense fallback={<RouteLoading />}>
      {element}
    </Suspense>
  )
}

function guardedPage(element: ReactNode, permissions: string[]) {
  return guarded(lazyPage(element), permissions)
}

function RouteLoading() {
  return (
    <div className="mes-route-loading">
      <Spin tip="页面加载中" />
    </div>
  )
}

export const router = createBrowserRouter([
  { path: '/login', element: lazyPage(<LoginPage />) },
  {
    path: '/',
    element: <AuthGuard />,
    children: [
      {
        element: <BasicLayout />,
        children: [
          { index: true, element: <Navigate to="/dashboard" replace /> },
          { path: 'dashboard', element: guardedPage(<DashboardPage />, [PERMISSIONS.reportView]) },
          { path: 'customers', element: guardedPage(<CustomerList />, [PERMISSIONS.baseView]) },
          { path: 'customers/create', element: guardedPage(<CustomerFormPage mode="create" />, [PERMISSIONS.baseManage]) },
          { path: 'customers/:uuid', element: guardedPage(<CustomerDetailPage />, [PERMISSIONS.baseView]) },
          { path: 'customers/:uuid/edit', element: guardedPage(<CustomerFormPage mode="edit" />, [PERMISSIONS.baseManage]) },
          { path: 'papers', element: guardedPage(<PaperList />, [PERMISSIONS.baseView]) },
          { path: 'papers/create', element: guardedPage(<PaperFormPage mode="create" />, [PERMISSIONS.baseManage]) },
          { path: 'papers/:uuid', element: guardedPage(<PaperDetailPage />, [PERMISSIONS.baseView]) },
          { path: 'papers/:uuid/edit', element: guardedPage(<PaperFormPage mode="edit" />, [PERMISSIONS.baseManage]) },
          { path: 'machines', element: guardedPage(<MachineList />, [PERMISSIONS.baseView]) },
          { path: 'machines/create', element: guardedPage(<MachineFormPage mode="create" />, [PERMISSIONS.baseManage]) },
          { path: 'machines/:uuid', element: guardedPage(<MachineDetailPage />, [PERMISSIONS.baseView]) },
          { path: 'machines/:uuid/edit', element: guardedPage(<MachineFormPage mode="edit" />, [PERMISSIONS.baseManage]) },
          { path: 'warehouses', element: guardedPage(<WarehouseList />, [PERMISSIONS.baseView]) },
          { path: 'warehouses/create', element: guardedPage(<WarehouseFormPage mode="create" />, [PERMISSIONS.baseManage]) },
          { path: 'warehouses/:uuid', element: guardedPage(<WarehouseDetailPage />, [PERMISSIONS.baseView]) },
          { path: 'warehouses/:uuid/edit', element: guardedPage(<WarehouseFormPage mode="edit" />, [PERMISSIONS.baseManage]) },
          { path: 'process-orders', element: guardedPage(<ProcessOrderList />, [PERMISSIONS.orderView]) },
          { path: 'process-orders/create', element: guardedPage(<CreateOrderPage />, [PERMISSIONS.orderCreate]) },
          { path: 'process-orders/:uuid', element: guardedPage(<OrderDetailPage />, [PERMISSIONS.orderView]) },
          { path: 'process-orders/:uuid/back-record', element: guardedPage(<BackRecordPage />, [PERMISSIONS.orderBackRecord]) },
          { path: 'process-orders/:uuid/config-finish', element: guardedPage(<ConfigFinishPage />, [PERMISSIONS.orderManage]) },
          { path: 'delivery-orders', element: guardedPage(<DeliveryOrderList />, [PERMISSIONS.deliveryView]) },
          { path: 'delivery-orders/create', element: guardedPage(<DeliveryCreatePage />, [PERMISSIONS.deliveryManage]) },
          { path: 'delivery-orders/:uuid', element: guardedPage(<DeliveryDetailPage />, [PERMISSIONS.deliveryView]) },
          { path: 'settle-orders', element: guardedPage(<SettleOrderList />, [PERMISSIONS.settleView]) },
          { path: 'settle-orders/create', element: guardedPage(<SettleCreatePage />, [PERMISSIONS.settleManage]) },
          { path: 'settle-orders/:uuid', element: guardedPage(<SettleDetailPage />, [PERMISSIONS.settleView]) },
          { path: 'reports', element: guardedPage(<ReportPage />, [PERMISSIONS.reportView]) },
          { path: 'users', element: guardedPage(<UserList />, [PERMISSIONS.userManage]) },
          { path: 'users/create', element: guardedPage(<UserFormPage mode="create" />, [PERMISSIONS.userManage]) },
          { path: 'users/:uuid', element: guardedPage(<UserDetailPage />, [PERMISSIONS.userManage]) },
          { path: 'users/:uuid/edit', element: guardedPage(<UserFormPage mode="edit" />, [PERMISSIONS.userManage]) },
          { path: 'system-config', element: guardedPage(<SystemConfigPage />, [PERMISSIONS.userManage]) },
          { path: 'operation-logs', element: guardedPage(<OperationLogPage />, [PERMISSIONS.systemAudit]) },
        ],
      },
    ],
  },
])
