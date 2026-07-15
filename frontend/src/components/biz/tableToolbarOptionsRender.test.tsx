import { createElement, isValidElement } from 'react'
import { describe, expect, it } from 'vitest'
import { mesProTableOptions } from './mesProTableOptions'
import { renderCompatibleTableOptions } from './tableToolbarOptionsRender'

describe('表格工具栏兼容渲染', () => {
  it('关闭 ProTable 存在兼容警告的默认密度入口', () => {
    expect(mesProTableOptions().density).toBe(false)
  })

  it('使用支持 React 严格模式的密度入口替代默认入口', () => {
    const options = renderCompatibleTableOptions(undefined, [createElement('span', { key: 'reload' })])

    expect(options).toHaveLength(2)
    expect(isValidElement(options[0]) && options[0].key).toBe('density')
  })
})
