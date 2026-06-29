import { Breadcrumb } from 'antd'
import { HomeOutlined } from '@ant-design/icons'
import { useLocation, useNavigate } from 'react-router-dom'
import { findRouteMeta } from '../router/routeMeta'

export default function AppBreadcrumb() {
  const location = useLocation()
  const navigate = useNavigate()
  const meta = findRouteMeta(location.pathname)
  const homeItem = {
    title: (
      <button className="app-shell__breadcrumb-home" type="button" onClick={() => navigate('/dashboard')}>
        <HomeOutlined />
        <span>首页</span>
      </button>
    ),
  }
  const items = meta?.parentLabel
    ? [homeItem, { title: meta.parentLabel }, { title: meta.label }]
    : [homeItem, { title: meta?.label ?? '仪表盘' }]

  return <Breadcrumb className="app-shell__breadcrumb" items={items} />
}
