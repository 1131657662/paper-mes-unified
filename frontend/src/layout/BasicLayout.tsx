import { useEffect, useRef } from 'react'
import { Alert, Avatar, Dropdown, Layout, Menu, Space, Tag } from 'antd'
import type { MenuProps } from 'antd'
import {
  ExportOutlined,
  AccountBookOutlined,
  BarChartOutlined,
  ContainerOutlined,
  DashboardOutlined,
  FileTextOutlined,
  ControlOutlined,
  LogoutOutlined,
  ProfileOutlined,
  SettingOutlined,
  TeamOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { logout } from '../api/auth'
import AppBreadcrumb from './AppBreadcrumb'
import PageTabs from './PageTabs'
import { selectedMenuKey } from '../router/routeMeta'
import { useAuthActions, useAuthUser } from '../stores/authStore'
import { PERMISSIONS } from '../constants/permissions'
import { useOnlineStatus } from '../hooks/useOnlineStatus'
import { hasAnyPermission } from '../utils/permission'
import '../styles/app-shell.css'

const { Sider, Header, Content } = Layout

const profileModules = ['customers', 'papers', 'machines', 'warehouses', 'users']

export default function BasicLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const contentRef = useRef<HTMLElement | null>(null)
  const user = useAuthUser()
  const { signOut } = useAuthActions()
  const isOnline = useOnlineStatus()
  const menuItems = buildMenuItems(user?.permissions)
  const contentClassName = [
    'app-shell__content',
    isEdgeScrollRoute(location.pathname) && 'app-shell__content--edge-scroll',
  ].filter(Boolean).join(' ')

  useEffect(() => {
    contentRef.current?.scrollTo({ top: 0, left: 0 })
  }, [location.pathname, location.search])

  return (
    <Layout className="app-shell">
      <Sider theme="dark" collapsible>
        <div className="app-shell__brand">
          纸品加工 MES
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[selectedMenuKey(location.pathname)]}
          defaultOpenKeys={['base', 'system']}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout className="app-shell__main">
        <Header className="app-shell__header">
          <AppBreadcrumb />
          <Dropdown
            menu={{
              items: [
                {
                  key: 'logout',
                  icon: <LogoutOutlined />,
                  label: '退出登录',
                  onClick: async () => {
                    try {
                      await logout()
                    } finally {
                      signOut()
                      navigate('/login')
                    }
                  },
                },
              ],
            }}
            trigger={['click']}
          >
            <Space className="app-shell__user">
              <Avatar size={30} icon={<UserOutlined />} />
              <span>{user?.realName ?? user?.username ?? '未登录'}</span>
              <Tag color={user?.roleCode === 'admin' ? 'blue' : 'default'}>
                {roleLabel(user?.roleCode)}
              </Tag>
            </Space>
          </Dropdown>
        </Header>
        <PageTabs />
        {!isOnline && (
          <Alert
            className="app-shell__offline"
            banner
            showIcon
            type="warning"
            message="当前网络已断开，页面数据可能无法保存或刷新，请检查网络连接。"
          />
        )}
        <Content ref={contentRef} className={contentClassName}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

type MenuItem = Required<MenuProps>['items'][number]

function buildMenuItems(permissions?: string[]): MenuProps['items'] {
  const can = (items: string[]) => hasAnyPermission(permissions, items)
  const baseChildren = [
    can([PERMISSIONS.baseView]) && { key: '/customers', label: '客户管理' },
    can([PERMISSIONS.baseView]) && { key: '/papers', label: '纸张档案' },
    can([PERMISSIONS.baseView]) && { key: '/machines', label: '机台档案' },
    can([PERMISSIONS.baseView]) && { key: '/warehouses', label: '仓库档案' },
  ].filter(Boolean) as MenuItem[]
  const systemChildren = can([PERMISSIONS.userManage])
    ? [
        { key: '/users', icon: <TeamOutlined />, label: '用户权限' },
        { key: '/system-config', icon: <ControlOutlined />, label: '系统配置' },
        { key: '/operation-logs', icon: <FileTextOutlined />, label: '操作日志' },
      ]
    : can([PERMISSIONS.systemAudit]) ? [
        { key: '/operation-logs', icon: <FileTextOutlined />, label: '操作日志' },
      ] : []

  return [
    can([PERMISSIONS.reportView]) && { key: '/dashboard', icon: <DashboardOutlined />, label: '仪表盘' },
    can([PERMISSIONS.orderView]) && { key: '/process-orders', icon: <ContainerOutlined />, label: '加工单' },
    can([PERMISSIONS.deliveryView]) && { key: '/delivery-orders', icon: <ExportOutlined />, label: '出库管理' },
    can([PERMISSIONS.settleView]) && { key: '/settle-orders', icon: <AccountBookOutlined />, label: '结算管理' },
    can([PERMISSIONS.reportView]) && { key: '/reports', icon: <BarChartOutlined />, label: '统计报表' },
    baseChildren.length > 0 && {
      key: 'base',
      icon: <ProfileOutlined />,
      label: '基础档案',
      children: baseChildren,
    },
    systemChildren.length > 0 && {
      key: 'system',
      icon: <SettingOutlined />,
      label: '系统管理',
      children: systemChildren,
    },
  ].filter(Boolean) as MenuProps['items']
}

function isEdgeScrollRoute(pathname: string) {
  return pathname === '/dashboard'
    || pathname === '/reports'
    || pathname === '/system-config'
    || pathname === '/operation-logs'
    || pathname === '/process-orders/create'
    || isProcessOrderDetailRoute(pathname)
    || isProcessOrderWorkbenchRoute(pathname)
    || isDocumentDetailRoute(pathname, 'delivery-orders')
    || isDocumentDetailRoute(pathname, 'settle-orders')
    || isProfileRoute(pathname)
}

function isProcessOrderDetailRoute(pathname: string) {
  return /^\/process-orders\/[^/]+$/.test(pathname) && pathname !== '/process-orders/create'
}

function isProcessOrderWorkbenchRoute(pathname: string) {
  return /^\/process-orders\/[^/]+\/(?:back-record|config-finish)$/.test(pathname)
}

function isDocumentDetailRoute(pathname: string, modulePath: string) {
  return pathname === `/${modulePath}/create` || new RegExp(`^/${modulePath}/[^/]+$`).test(pathname)
}

function isProfileRoute(pathname: string) {
  return profileModules.some((modulePath) => (
    pathname === `/${modulePath}/create`
    || new RegExp(`^/${modulePath}/[^/]+(?:/edit)?$`).test(pathname)
  ))
}

function roleLabel(roleCode?: string) {
  if (roleCode === 'admin') return '管理员'
  if (roleCode === 'operator') return '录单员'
  if (roleCode === 'finance') return '财务'
  if (roleCode === 'warehouse') return '仓库'
  return '访客'
}
