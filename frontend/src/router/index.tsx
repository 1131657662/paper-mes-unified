import { createBrowserRouter, Navigate } from 'react-router-dom'
import AuthGuard from './AuthGuard'
import BasicLayout from '../layout/BasicLayout'
import CustomerList from '../pages/customer/CustomerList'
import PaperList from '../pages/paper/PaperList'
import MachineList from '../pages/machine/MachineList'
import WarehouseList from '../pages/warehouse/WarehouseList'
import ProcessOrderList from '../pages/processOrder/ProcessOrderList'
import CreateOrderPage from '../pages/processOrder/CreateOrderPage'
import ConfigFinishPage from '../pages/processOrder/ConfigFinishPage'
import OrderDetailPage from '../pages/processOrder/OrderDetailPage'
import BackRecordPage from '../pages/processOrder/BackRecordPage'
import DeliveryOrderList from '../pages/delivery/DeliveryOrderList'
import SettleOrderList from '../pages/settle/SettleOrderList'
import ReportPage from '../pages/report/ReportPage'
import OperationLogPage from '../pages/operationLog/OperationLogPage'
import LoginPage from '../pages/login/LoginPage'
import DashboardPage from '../pages/dashboard/DashboardPage'

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
          { path: 'dashboard', element: <DashboardPage /> },
          { path: 'customers', element: <CustomerList /> },
          { path: 'papers', element: <PaperList /> },
          { path: 'machines', element: <MachineList /> },
          { path: 'warehouses', element: <WarehouseList /> },
          { path: 'process-orders', element: <ProcessOrderList /> },
          { path: 'process-orders/create', element: <CreateOrderPage /> },
          { path: 'process-orders/:uuid', element: <OrderDetailPage /> },
          { path: 'process-orders/:uuid/back-record', element: <BackRecordPage /> },
          { path: 'process-orders/:uuid/config-finish', element: <ConfigFinishPage /> },
          { path: 'delivery-orders', element: <DeliveryOrderList /> },
          { path: 'settle-orders', element: <SettleOrderList /> },
          { path: 'reports', element: <ReportPage /> },
          { path: 'operation-logs', element: <OperationLogPage /> },
        ],
      },
    ],
  },
])
