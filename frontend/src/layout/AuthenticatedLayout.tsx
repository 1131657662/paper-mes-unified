import { ProConfigProvider } from '@ant-design/pro-provider'
import { zhCNIntl } from '@ant-design/pro-provider/es/intl'
import BasicLayout from './BasicLayout'

export default function AuthenticatedLayout() {
  return (
    <ProConfigProvider intl={zhCNIntl}>
      <BasicLayout />
    </ProConfigProvider>
  )
}
