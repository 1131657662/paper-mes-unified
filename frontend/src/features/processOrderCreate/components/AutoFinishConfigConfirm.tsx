import { Modal } from 'antd'
import AutoFinishConfigSummary from './AutoFinishConfigSummary'
import type { AutoFinishConfigItem } from './AutoFinishConfigSummary'

export type { AutoFinishConfigItem } from './AutoFinishConfigSummary'

export function confirmAutoFinishConfigs(items: AutoFinishConfigItem[]): Promise<boolean> {
  return new Promise((resolve) => {
    Modal.confirm({
      title: '确认保存待处理方案',
      content: <AutoFinishConfigSummary items={items} />,
      width: 760,
      okText: '保存并进入预览确认',
      cancelText: '返回继续检查',
      onOk: () => resolve(true),
      onCancel: () => resolve(false),
    })
  })
}
