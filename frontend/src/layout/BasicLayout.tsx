import { Avatar, Dropdown, Layout, Menu, Space, Tag } from 'antd'
import {
  ExportOutlined,
  AccountBookOutlined,
  BarChartOutlined,
  ContainerOutlined,
  DashboardOutlined,
  FileTextOutlined,
  LogoutOutlined,
  ProfileOutlined,
  UserOutlined,
} from '@ant-design/icons'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { logout } from '../api/auth'
import AppBreadcrumb from './AppBreadcrumb'
import PageTabs from './PageTabs'
import { selectedMenuKey } from '../router/routeMeta'
import { useAuthActions, useAuthUser } from '../stores/authStore'
import '../styles/app-shell.css'

const { Sider, Header, Content } = Layout

/** 侧边菜单：基础档案分组已接路由，其余为后续页面占位（disabled，避免死链）。 */
const menuItems = [
  { key: '/dashboard', icon: <DashboardOutlined />, label: '仪表盘' },
  { key: '/process-orders', icon: <ContainerOutlined />, label: '加工单' },
  { key: '/delivery-orders', icon: <ExportOutlined />, label: '出库管理' },
  { key: '/settle-orders', icon: <AccountBookOutlined />, label: '结算管理' },
  { key: '/reports', icon: <BarChartOutlined />, label: '统计报表' },
  {
    key: 'base',
    icon: <ProfileOutlined />,
    label: '基础档案',
    children: [
      { key: '/customers', label: '客户管理' },
      { key: '/papers', label: '纸张档案' },
      { key: '/machines', label: '机台档案' },
      { key: '/warehouses', label: '仓库档案' },
    ],
  },
  { key: '/operation-logs', icon: <FileTextOutlined />, label: '操作日志' },
]

export default function BasicLayout() {
  const navigate = useNavigate()
  const location = useLocation()
  const user = useAuthUser()
  const { signOut } = useAuthActions()

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
          defaultOpenKeys={['base']}
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
        <Content className="app-shell__content">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

function roleLabel(roleCode?: string) {
  if (roleCode === 'admin') return '管理员'
  if (roleCode === 'operator') return '录单员'
  if (roleCode === 'finance') return '财务'
  if (roleCode === 'warehouse') return '仓库'
  return '访客'
}
