import { Alert, Typography } from 'antd'
import './ProcessOrderShared.css'

export default function DirectShipInfo() {
  return (
    <div>
      <Alert
        message="不加工直发"
        description="不加工直发模式，原纸将直接入库，无需配置成品。回录时可标记为直发状态，自动生成直发成品记录。"
        type="info"
        showIcon
      />

      <div className="direct-ship-info__guide">
        <Typography.Paragraph>
          <Typography.Text strong>流程说明：</Typography.Text>
        </Typography.Paragraph>
        <Typography.Paragraph className="direct-ship-info__steps">
          1. 此原纸将不经过加工环节，直接作为成品入库
          <br />
          2. 打印加工单时，该原纸将标记为直发
          <br />
          3. 回录时，系统自动为该原纸生成直发成品记录
          <br />
          4. 直发成品卷号将复用原纸的母卷号
        </Typography.Paragraph>
      </div>
    </div>
  )
}
