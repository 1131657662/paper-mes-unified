import { Alert } from 'antd'

export default function OnSiteCountEditor() {
  return (
    <Alert
      type="info"
      showIcon
      message="实际产出由车间回录"
      description="此处只确定母卷、主工艺、设备和单价；成品与切边数量、门幅、直径、纸芯和重量均按现场结果录入。"
    />
  )
}
