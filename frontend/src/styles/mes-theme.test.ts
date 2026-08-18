import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const css = readFileSync(new URL('./mes-theme.css', import.meta.url), 'utf8')

describe('状态标签无障碍样式', () => {
  it('成功状态使用高对比度文字色', () => {
    expect(css).toContain('.mes-status-tag.ant-tag-success')
    expect(css).toMatch(
      /\.mes-status-tag\.ant-tag-success[\s\S]*?color: var\(--mes-color-success-strong\);/,
    )
  })
})
