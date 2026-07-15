import { Suspense } from 'react'
import type { ReactNode } from 'react'
import { Spin } from 'antd'
import { createBrowserRouter, Navigate } from 'react-router-dom'
import AuthGuard from './AuthGuard'
import BasicLayout from '../layout/BasicLayout'
import PermissionGuard from '../components/PermissionGuard'
import { PERMISSIONS } from '../constants/permissions'
import {
  BackRecordPage,
  ConfigFinishPage,
  CreateOrderPage,
  CustomerDetailPage,
  CustomerFormPage,
  CustomerList,
  DashboardPage,
  DeliveryCreatePage,
  DeliveryDetailPage,
  DeliveryOrderList,
  LoginPage,
  MachineDetailPage,
  MachineFormPage,
  MachineList,
  NotFoundPage,
  OperationLogPage,
  OrderDetailPage,
  PaperDetailPage,
  PaperFormPage,
  PaperList,
  ProcessOrderList,
  ProfilePage,
  ReportPage,
  RouteDesignerPage,
  SettleCreatePage,
  SettleDetailPage,
  SettleOrderList,
  SystemConfigPage,
  UserDetailPage,
  UserFormPage,
  UserList,
  WarehouseDetailPage,
  WarehouseFormPage,
  WarehouseList,
} from './lazyPages'

function guarded(element: ReactNode, permissions: string[]) {
  return <PermissionGuard permissions={permissions}>{element}</PermissionGuard>
}

function lazyPage(element: ReactNode) {
  return (
    <Suspense fallback={routeLoading()}>
      {element}
    </Suspense>
  )
}

function guardedPage(element: ReactNode, permissions: string[]) {
  return guarded(lazyPage(element), permissions)
}

function routeLoading() {
  return (
    <div className="mes-route-loading">
      <Spin />
      <span>页面加载中...</span>
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
          { path: 'process-orders/create/:uuid/routes/:rollUuid', element: guardedPage(<RouteDesignerPage />, [PERMISSIONS.orderCreate]) },
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
          {
            path: 'system-config',
            element: guardedPage(<SystemConfigPage />, [
              PERMISSIONS.systemConfig,
              PERMISSIONS.dataBackup,
              PERMISSIONS.dataHealth,
            ]),
          },
          { path: 'operation-logs', element: guardedPage(<OperationLogPage />, [PERMISSIONS.systemAudit]) },
          { path: 'profile', element: lazyPage(<ProfilePage />) },
          { path: '*', element: lazyPage(<NotFoundPage />) },
        ],
      },
    ],
  },
])
