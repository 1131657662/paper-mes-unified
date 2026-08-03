import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import { LoginHero } from './LoginHero'

describe('登录页业务范围文案', () => {
  it('不宣传已移出范围的现场协同、机台作业和生产回录', () => {
    const markup = renderToStaticMarkup(<LoginHero />)

    expect(markup).toContain('业务台账')
    expect(markup).not.toContain('现场协同')
    expect(markup).not.toContain('机台作业')
    expect(markup).not.toContain('生产回录')
  })
})
