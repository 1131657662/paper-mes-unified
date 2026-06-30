import { Card, Tabs } from 'antd'
import MesPageHeader from '../../components/layout/MesPageHeader'
import ConfigItemPanel from './ConfigItemPanel'
import DictItemPanel from './DictItemPanel'
import NoRulePanel from './NoRulePanel'
import './SystemConfigPage.css'

export default function SystemConfigPage() {
  return (
    <div className="system-config-page">
      <MesPageHeader
        title="系统配置"
        eyebrow="系统管理"
        description="统一维护业务字典和系统参数。第一阶段只影响配置维护与展示，不自动改写已保存业务单据。"
      />

      <Card className="system-config-card">
        <Tabs
          destroyOnHidden={false}
          items={[
            { key: 'dict', label: '数据字典', children: <DictItemPanel /> },
            { key: 'config', label: '系统参数', children: <ConfigItemPanel /> },
            { key: 'noRule', label: '单号规则', children: <NoRulePanel /> },
          ]}
        />
      </Card>
    </div>
  )
}
