import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import type { ProcessOrder } from '../../types/processOrder'
import ProcessOrderRowActions from './ProcessOrderRowActions'
import type { ProcessOrderListCapabilities } from './useProcessOrderListCapabilities'

const noCapabilities: ProcessOrderListCapabilities = {
  canBackRecord: false,
  canCreateOrder: false,
  canManageDelivery: false,
  canManageOrder: false,
  canManageSettlement: false,
}

const actions = {
  onBackRecord: () => undefined,
  onChangeStatus: () => undefined,
  onEditDraft: () => undefined,
  onGoDelivery: () => undefined,
  onGoSettle: () => undefined,
  onPrint: () => undefined,
}

describe('加工单列表行内主操作权限', () => {
  it('只读用户不显示需要管理权限的下发操作', () => {
    const markup = renderAction({ orderStatus: 1 }, noCapabilities)

    expect(markup).not.toContain('打印下发')
  })

  it('加工单管理员可以打印下发', () => {
    const markup = renderAction({ orderStatus: 1 }, { ...noCapabilities, canManageOrder: true })

    expect(markup).toContain('打印下发')
  })

  it('只有结算权限时将已完成单据主操作切换为生成结算', () => {
    const markup = renderAction({ orderStatus: 4 }, { ...noCapabilities, canManageSettlement: true })

    expect(markup).toContain('生成结算')
    expect(markup).not.toContain('创建出库')
  })

  it('同时拥有出库和结算权限时展示两个操作', () => {
    const markup = renderAction({ orderStatus: 4 }, {
      ...noCapabilities,
      canManageDelivery: true,
      canManageSettlement: true,
    })

    expect(markup).toContain('创建出库')
    expect(markup).toContain('生成结算')
  })

  it('回录权限独立控制待回录入口', () => {
    const markup = renderAction({ orderStatus: 3 }, { ...noCapabilities, canBackRecord: true })

    expect(markup).toContain('进入回录')
  })
})

function renderAction(order: Partial<ProcessOrder>, capabilities: ProcessOrderListCapabilities) {
  return renderToStaticMarkup(
    <ProcessOrderRowActions
      {...actions}
      capabilities={capabilities}
      record={{ uuid: 'order-1', ...order }}
    />,
  )
}
