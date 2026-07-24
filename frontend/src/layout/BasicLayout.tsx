import { useEffect, useRef } from 'react'
import { Alert, Dropdown, Layout, Menu } from 'antd'
import {
  DownOutlined,
  LogoutOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { logout } from '../api/auth'
import AppBreadcrumb from './AppBreadcrumb'
import PageTabs from './PageTabs'
import AppLogoMark from '../components/brand/AppLogoMark'
import GeneratedUserAvatar from '../components/user/GeneratedUserAvatar'
import NotificationBell from './NotificationBell'
import DownloadTaskCenter from './DownloadTaskCenter'
import { findRouteMeta, selectedMenuKey } from '../router/routeMeta'
import { APP_BRAND } from '../config/brand'
import { useAuthActions, useAuthUser } from '../stores/authStore'
import { useDocumentTitle } from '../hooks/useDocumentTitle'
import { useOnlineStatus } from '../hooks/useOnlineStatus'
import { buildMenuItems } from './appMenuItems'
import { isEdgeScrollRoute } from './layoutRouteMode'
import { defaultOpenMenuKeys } from './menuOpenKeys'
import { roleLabel } from './roleLabel'
import '../styles/app-shell.css'

const { Sider, Header, Content } = Layout

export default function BasicLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const contentRef = useRef<HTMLElement | null>(null)
  const user = useAuthUser()
  const { signOut } = useAuthActions()
  const isOnline = useOnlineStatus()
  const menuItems = buildMenuItems(user?.permissions)
  const routeTitle = findRouteMeta(location.pathname)?.label
  const contentClassName = [
    'app-shell__content',
    isEdgeScrollRoute(location.pathname) && 'app-shell__content--edge-scroll',
  ].filter(Boolean).join(' ')

  useDocumentTitle(routeTitle)

  useEffect(() => {
    contentRef.current?.scrollTo({ top: 0, left: 0 })
  }, [location.pathname, location.search])

  return (
    <Layout className="app-shell">
      <Sider theme="dark" collapsible breakpoint="xl" collapsedWidth={80} width={216}>
        <div className="app-shell__brand" aria-label={APP_BRAND.name}>
          <AppLogoMark className="app-shell__brand-mark" title={`${APP_BRAND.name} 图标`} />
          <span className="app-shell__brand-name">{APP_BRAND.name}</span>
        </div>
        <nav className="app-shell__nav" aria-label="主导航">
          <Menu
            theme="dark"
            mode="inline"
            selectedKeys={[selectedMenuKey(location.pathname)]}
            defaultOpenKeys={defaultOpenMenuKeys(location.pathname)}
            items={menuItems}
            onClick={({ key }) => navigate(key)}
          />
        </nav>
      </Sider>
      <Layout className="app-shell__main">
        <Header className="app-shell__header">
          <AppBreadcrumb />
          <div className="app-shell__header-actions">
            <DownloadTaskCenter />
            <NotificationBell />
            <Dropdown
            menu={{
              items: [
                {
                  key: 'profile',
                  icon: <UserOutlined />,
                  label: '个人中心',
                  onClick: () => navigate('/profile'),
                },
                {
                  type: 'divider',
                },
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
            <button type="button" className="app-shell__user" aria-label="用户菜单">
              <GeneratedUserAvatar className="app-shell__user-avatar" size={30} user={user} />
              <span className="app-shell__user-meta">
                <span className="app-shell__user-name">{user?.realName ?? user?.username ?? '未登录'}</span>
                <span className="app-shell__user-role">{roleLabel(user?.roleCode)}</span>
              </span>
              <DownOutlined className="app-shell__user-caret" />
            </button>
            </Dropdown>
          </div>
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
