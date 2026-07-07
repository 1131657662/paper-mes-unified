import { Button, Result, Space } from 'antd'
import { useNavigate } from 'react-router-dom'
import '../documentModule.css'

export default function NotFoundPage() {
  const navigate = useNavigate()

  return (
    <div className="document-module-page">
      <Result
        status="404"
        title="页面不存在"
        subTitle="当前地址没有对应的系统页面，请返回首页或回到上一页。"
        extra={(
          <Space wrap>
            <Button type="primary" onClick={() => navigate('/dashboard')}>返回首页</Button>
            <Button onClick={() => navigate(-1)}>返回上一页</Button>
          </Space>
        )}
      />
    </div>
  )
}
