import type { OptionConfig } from '@ant-design/pro-table/es/components/ToolBar'

type ReloadHandler = () => Promise<void> | void

export function mesProTableOptions(onReload?: ReloadHandler): OptionConfig {
  return {
    density: true,
    reload: onReload ? () => { void onReload() } : true,
    setting: true,
  }
}
