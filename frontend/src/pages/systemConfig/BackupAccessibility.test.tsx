import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { BackupStatus } from '../../types/dataBackup'
import BackupOverview from './BackupOverview'

const status: BackupStatus = {
  automaticConsecutiveFailures: 0,
  automaticEnabled: true,
  automaticExecutionTime: '02:35',
  backupCount: 1,
  configured: true,
  enabled: true,
  message: '可用',
  missingComponents: [],
  offsiteStatus: 'NOT_CONFIGURED',
  platform: 'WINDOWS',
  retentionDays: 90,
  runner: 'POWERSHELL',
  running: false,
  totalSpaceBytes: 2 * 1024 ** 3,
  usableSpaceBytes: 1024 ** 3,
}

describe('备份控制区可访问名称', () => {
  it('为开关、执行时间和保留天数提供业务名称', () => {
    const markup = renderToStaticMarkup(
      <BackupOverview
        actions={{
          backup: () => undefined,
          cleanup: () => undefined,
          refresh: () => undefined,
          saveAutomatic: () => undefined,
          saveRetention: () => undefined,
          toggle: () => undefined,
        }}
        busy={false}
        loading={false}
        status={status}
        unavailable={false}
      />,
    )

    expect(markup).toContain('aria-label="启用管理端备份"')
    expect(markup).toContain('aria-label="启用自动备份"')
    expect(markup).toContain('aria-label="自动备份执行时间"')
    expect(markup).toContain('aria-label="本地备份保留天数"')
  })
})
