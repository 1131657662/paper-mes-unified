import { renderToStaticMarkup } from 'react-dom/server'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import type { AvailableFinishVO } from '../../types/delivery'
import DeliveryCreateTable from './DeliveryCreateTable'

describe('出库成品选择表', () => {
  beforeEach(() => {
    vi.stubGlobal('localStorage', memoryStorage())
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('预选成品在异步数据尚未返回时不参与树形联动', () => {
    expect(() => renderToStaticMarkup(
      <DeliveryCreateTable
        data={[]}
        edits={{}}
        emptyText="暂无可出库成品"
        loading
        scope="product"
        selectedRowKeys={['finish-1']}
        onEditChange={() => undefined}
        onSelectionChange={() => undefined}
      />,
    )).not.toThrow()
  })

  it('分组行禁用默认复选框并保留成品选择', () => {
    const markup = renderToStaticMarkup(
      <DeliveryCreateTable
        data={[finish()]}
        edits={{}}
        emptyText="暂无可出库成品"
        loading={false}
        scope="product"
        selectedRowKeys={['finish-1']}
        onEditChange={() => undefined}
        onSelectionChange={() => undefined}
      />,
    )

    expect(markup).toContain('disabled=""')
    expect(markup).toContain('F-001')
  })
})

function memoryStorage() {
  return {
    clear: () => undefined,
    getItem: () => null,
    key: () => null,
    length: 0,
    removeItem: () => undefined,
    setItem: () => undefined,
  }
}

function finish(): AvailableFinishVO {
  return {
    actualWeight: 100,
    finishRollNo: 'F-001',
    finishStatus: 2,
    finishUuid: 'finish-1',
    orderNo: 'JG-001',
    orderUuid: 'order-1',
    paperName: '白卡纸',
    sourceType: 1,
  }
}
