import { readFileSync } from 'node:fs'
import { describe, expect, it } from 'vitest'

const routerSource = readFileSync(new URL('./index.tsx', import.meta.url), 'utf8')

describe('业务路由错误隔离', () => {
  it('在权限页和懒加载页内部使用页面级错误边界', () => {
    expect(routerSource).toContain("import RouteErrorBoundary from './RouteErrorBoundary'")
    expect(routerSource).toContain('<RouteErrorBoundary>{element}</RouteErrorBoundary>')
    expect(routerSource.indexOf('<RouteErrorBoundary>')).toBeGreaterThan(routerSource.indexOf('<Suspense'))
  })
})
