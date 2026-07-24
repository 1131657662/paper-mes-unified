import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { HeaderActions } from './ConfigFinishPage'

describe('成品规格配置页操作区', () => {
  it('详情未加载完成时禁用批量保存并说明原因', () => {
    const markup = renderToStaticMarkup(
      <HeaderActions
        disabledReason="加工单与母卷信息加载中"
        saving={false}
        onCancel={() => undefined}
        onSaveAll={() => undefined}
      />,
    )

    expect(markup).toContain('aria-label="保存全部并完成：加工单与母卷信息加载中"')
    expect(markup).toContain('disabled=""')
  })
})
