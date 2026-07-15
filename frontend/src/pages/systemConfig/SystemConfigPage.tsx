import { useEffect, useState } from 'react'
import { Badge, Card, Modal, Tabs } from 'antd'
import type { TabsProps } from 'antd'
import { useBlocker, useSearchParams } from 'react-router-dom'
import MesPageHeader from '../../components/layout/MesPageHeader'
import { PERMISSIONS } from '../../constants/permissions'
import { useHasPermission } from '../../stores/authStore'
import ConfigItemPanel from './ConfigItemPanel'
import DataBackupPanel from './DataBackupPanel'
import DataHealthPanel from './DataHealthPanel'
import DictItemPanel from './DictItemPanel'
import NoRulePanel from './NoRulePanel'
import './SystemConfigPage.css'

type ConfigSection = 'dict' | 'config' | 'noRule' | 'backup' | 'health'
type EditableSection = Exclude<ConfigSection, 'backup' | 'health'>
type DirtyState = Record<EditableSection, boolean>

const initialDirtyState: DirtyState = { dict: false, config: false, noRule: false }

export default function SystemConfigPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [dirtyState, setDirtyState] = useState<DirtyState>(initialDirtyState)
  const canConfigure = useHasPermission(PERMISSIONS.systemConfig)
  const canBackup = useHasPermission(PERMISSIONS.dataBackup)
  const canInspectHealth = useHasPermission(PERMISSIONS.dataHealth)
  const backupView = searchParams.get('view') === 'tasks' ? 'tasks' : 'records'
  const focusedTaskId = searchParams.get('task') ?? undefined
  const isDirty = Object.values(dirtyState).some(Boolean)
  const blocker = useBlocker(isDirty)

  useEffect(() => {
    if (blocker.state !== 'blocked') return
    const dialog = Modal.confirm({
      title: '存在未保存修改',
      content: '当前系统配置有未保存的修改，确定要离开吗？',
      okText: '离开页面',
      cancelText: '继续编辑',
      onOk: () => blocker.proceed(),
      onCancel: () => blocker.reset(),
    })
    return () => dialog.destroy()
  }, [blocker])

  useEffect(() => {
    if (!isDirty) return
    const handleBeforeUnload = (event: BeforeUnloadEvent) => {
      event.preventDefault()
      event.returnValue = ''
    }
    window.addEventListener('beforeunload', handleBeforeUnload)
    return () => window.removeEventListener('beforeunload', handleBeforeUnload)
  }, [isDirty])

  function updateDirty(section: EditableSection, dirty: boolean) {
    setDirtyState((current) => ({ ...current, [section]: dirty }))
  }

  function changeSection(nextKey: string) {
    const section = nextKey as ConfigSection
    if (section === activeKey) return
    if (isEditableSection(activeKey) && dirtyState[activeKey]) {
      Modal.confirm({
        title: '当前分类有未保存修改',
        content: '切换分类会保留当前编辑窗口，确定继续切换吗？',
        okText: '继续切换',
        cancelText: '返回编辑',
        onOk: () => activateSection(section),
      })
      return
    }
    activateSection(section)
  }

  function activateSection(section: ConfigSection) {
    const next = new URLSearchParams(searchParams)
    if (section === 'dict') next.delete('section')
    else next.set('section', section)
    if (section !== 'backup') {
      next.delete('view')
      next.delete('task')
    }
    setSearchParams(next)
  }

  function changeBackupView(view: 'records' | 'tasks') {
    const next = new URLSearchParams(searchParams)
    next.set('section', 'backup')
    if (view === 'records') next.delete('view')
    else next.set('view', view)
    next.delete('task')
    setSearchParams(next)
  }

  const tabItems = buildTabItems({
    backupView,
    canBackup,
    canConfigure,
    canInspectHealth,
    dirtyState,
    focusedTaskId,
    onBackupViewChange: changeBackupView,
    onDirtyChange: updateDirty,
  })
  const requestedSection = configSection(searchParams.get('section'))
  const activeKey = tabItems.some((item) => item.key === requestedSection)
    ? requestedSection
    : tabItems[0]?.key as ConfigSection

  return (
    <div className="system-config-page">
      <MesPageHeader title="系统配置" eyebrow="系统管理" />
      <Card className="system-config-card">
        <Tabs
          activeKey={activeKey}
          className="system-config-tabs"
          destroyOnHidden={false}
          tabPosition="left"
          onChange={changeSection}
          items={tabItems}
        />
      </Card>
    </div>
  )
}

interface BuildTabItemsOptions {
  backupView: 'records' | 'tasks'
  canBackup: boolean
  canConfigure: boolean
  canInspectHealth: boolean
  dirtyState: DirtyState
  focusedTaskId?: string
  onBackupViewChange: (view: 'records' | 'tasks') => void
  onDirtyChange: (section: EditableSection, dirty: boolean) => void
}

function buildTabItems(options: BuildTabItemsOptions): NonNullable<TabsProps['items']> {
  const items: NonNullable<TabsProps['items']> = []
  if (options.canConfigure) items.push(...configurationTabs(options))
  if (options.canBackup) items.push({
    key: 'backup',
    label: <SectionLabel title="数据安全" dirty={false} />,
    children: <DataBackupPanel activeView={options.backupView} focusedTaskId={options.focusedTaskId} onViewChange={options.onBackupViewChange} />,
  })
  if (options.canInspectHealth) items.push({
    key: 'health',
    label: <SectionLabel title="数据巡检" dirty={false} />,
    children: <DataHealthPanel />,
  })
  return items
}

function configurationTabs(options: BuildTabItemsOptions): NonNullable<TabsProps['items']> {
  return [
    {
      key: 'dict',
      label: <SectionLabel title="数据字典" dirty={options.dirtyState.dict} />,
      children: <DictItemPanel onDirtyChange={(dirty) => options.onDirtyChange('dict', dirty)} />,
    },
    {
      key: 'config',
      label: <SectionLabel title="系统参数" dirty={options.dirtyState.config} />,
      children: <ConfigItemPanel onDirtyChange={(dirty) => options.onDirtyChange('config', dirty)} />,
    },
    {
      key: 'noRule',
      label: <SectionLabel title="单号规则" dirty={options.dirtyState.noRule} />,
      children: <NoRulePanel onDirtyChange={(dirty) => options.onDirtyChange('noRule', dirty)} />,
    },
  ]
}

function configSection(value: string | null): ConfigSection {
  if (value === 'config' || value === 'noRule' || value === 'backup' || value === 'health') return value
  return 'dict'
}

function isEditableSection(section: ConfigSection): section is EditableSection {
  return section === 'dict' || section === 'config' || section === 'noRule'
}

function SectionLabel({ title, dirty }: { title: string; dirty: boolean }) {
  return (
    <span className="system-config-section-label">
      <span>{title}</span>
      {dirty && <Badge status="error" />}
    </span>
  )
}
