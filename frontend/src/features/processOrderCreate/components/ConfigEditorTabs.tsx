import { Tabs } from 'antd'
import type { ReactNode } from 'react'
import type { ConfigEditorTab } from './configStepWorkspaceTypes'

interface Props {
  active: ConfigEditorTab
  main: ReactNode
  onChange: (tab: ConfigEditorTab) => void
  service: ReactNode
}

export default function ConfigEditorTabs(props: Props) {
  return (
    <Tabs
      activeKey={props.active}
      className="config-editor-tabs"
      items={[
        { key: 'plan', label: '成品方案', children: props.main },
        { key: 'service', label: '附加工艺', children: props.service },
      ]}
      onChange={(key) => {
        if (key === 'plan' || key === 'service') props.onChange(key)
      }}
    />
  )
}
