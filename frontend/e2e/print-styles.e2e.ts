import { expect, test } from '@playwright/test'

const documentPrintStyles = 'src/pages/documentModule.css'
const processOrderPrintStyles = 'src/features/processOrderDetail/components/PrintPreviewSheet.print.css'
const deliveryPrintStyles = 'src/pages/delivery/DeliveryPrintSheet.css'
const deliveryPrintMediaStyles = 'src/pages/delivery/DeliveryPrintSheet.print.css'
const deliveryMarkup = `
  <section class="document-print-area document-print-area--delivery">
    <article class="document-print-sheet delivery-print-sheet">
      <header class="delivery-print-header"><h1>出库单</h1>
        <dl class="delivery-print-info">
          <div class="delivery-print-info__item delivery-print-info__wide"><dt>出库单号</dt><dd>CK202607290001</dd></div>
          <div class="delivery-print-info__item"><dt>出库日期</dt><dd>2026-07-29</dd></div>
          <div class="delivery-print-info__item"><dt>出库仓库</dt><dd>成品仓</dd></div>
          <div class="delivery-print-info__item delivery-print-info__wide"><dt>货主</dt><dd>义乌市挚诚贸易有限公司</dd></div>
          <div class="delivery-print-info__item delivery-print-info__wide"><dt>客户</dt><dd>永丰包装有限公司</dd></div>
          <div class="delivery-print-info__item delivery-print-info__wide"><dt>车牌号</dt><dd>浙A12345</dd></div>
          <div class="delivery-print-info__item delivery-print-info__wide"><dt>柜号</dt><dd>GX-08</dd></div>
        </dl>
      </header>
      <section class="delivery-print-details"><h2>提货明细</h2>
        <table class="document-print-table delivery-print-table">
          <colgroup><col><col><col><col><col><col><col></colgroup>
          <thead><tr><th>序号</th><th>加工单</th><th>卷号</th><th>品名</th><th>规格</th><th>重量/kg</th><th>备注</th></tr></thead>
          <tbody id="delivery-rows"></tbody>
          <tbody class="delivery-print-table__total"><tr><td colspan="5">合计：12 卷</td><td>14.223 t</td><td></td></tr></tbody>
        </table>
      </section>
      <div class="delivery-print-remark"><strong>出库备注</strong><span>装车前核对卷号，确认包装与标签完整。</span></div>
      <footer class="delivery-print-signatures">
        <div><span>提货人</span><strong></strong></div><div><span>仓库复核</span><strong></strong></div>
        <div><span>司机签字</span><strong></strong></div><div><span>签收时间</span><strong></strong></div>
      </footer>
      <div class="delivery-print-page-footer"><span>CK202607290001</span><span>打印时间：2026-07-29 19:43</span></div>
    </article>
  </section>`

test.beforeEach(async ({ page }) => {
  await page.emulateMedia({ media: 'print' })
  await page.setContent('<main id="root"></main>')
  await page.addStyleTag({ path: documentPrintStyles })
  await page.addStyleTag({ path: processOrderPrintStyles })
  await page.addStyleTag({ path: deliveryPrintStyles })
  await page.addStyleTag({ path: deliveryPrintMediaStyles })
})

test('出库打印区不会被加工单打印样式隐藏', async ({ page }) => {
  await page.locator('#root').evaluate((root) => {
    root.innerHTML = `
      <section class="document-print-area">
        <div class="document-print-sheet">出库单正文</div>
      </section>
    `
  })

  await expect(page.getByText('出库单正文')).toBeVisible()
  await expect(page.locator('#root')).not.toHaveCSS('display', 'none')
})

test('加工单打印时仍只显示独立打印根节点', async ({ page }) => {
  await page.setContent(`
    <main id="root">业务页面</main>
    <section class="print-issue-print-root">加工单正文</section>
  `)
  await page.addStyleTag({ path: documentPrintStyles })
  await page.addStyleTag({ path: processOrderPrintStyles })

  await expect(page.locator('#root')).toHaveCSS('display', 'none')
  await expect(page.getByText('加工单正文')).toBeVisible()
})

test('出库单使用 A4 物理宽度和可读字号', async ({ page }, testInfo) => {
  await page.setContent(deliveryMarkup)
  await page.addStyleTag({ path: documentPrintStyles })
  await page.addStyleTag({ path: processOrderPrintStyles })
  await page.addStyleTag({ path: deliveryPrintStyles })
  await page.addStyleTag({ path: deliveryPrintMediaStyles })
  await page.locator('#delivery-rows').evaluate((body) => {
    body.innerHTML = Array.from({ length: 12 }, (_, index) =>
      `<tr><td>${index + 1}</td><td>JG202607250003</td><td>A00062${index}</td><td>WestRock白面牛卡纸75°</td><td>135 g × 900 mm</td><td class="delivery-print-table__weight">1203 kg</td><td>-</td></tr>`,
    ).join('')
  })

  await expect(page.locator('.document-print-area--delivery')).toHaveCSS('width', '733.219px')
  await expect(page.locator('.delivery-print-table')).toHaveCSS('font-size', '13.3333px')
  const firstRow = await page.locator('.delivery-print-table tbody:first-of-type tr').first().boundingBox()
  expect(firstRow?.height).toBeGreaterThanOrEqual(30.2)
  await page.evaluate(() => {
    document.documentElement.style.setProperty('--delivery-print-number', '"CK202607290001"')
    document.documentElement.style.setProperty('--delivery-print-time', '"打印时间：2026-07-29 19:43"')
  })
  const pdf = await page.pdf({ format: 'A4', path: testInfo.outputPath('delivery-print-a4.pdf'), printBackground: true })
  expect(pdf.byteLength).toBeGreaterThan(10_000)
})

test('多页出库单重复表头并保持末尾区域完整', async ({ page }, testInfo) => {
  await page.setContent(deliveryMarkup)
  await page.addStyleTag({ path: documentPrintStyles })
  await page.addStyleTag({ path: processOrderPrintStyles })
  await page.addStyleTag({ path: deliveryPrintStyles })
  await page.addStyleTag({ path: deliveryPrintMediaStyles })
  await page.locator('#delivery-rows').evaluate((body) => {
    body.innerHTML = Array.from({ length: 30 }, (_, index) =>
      `<tr><td>${index + 1}</td><td>JG202607250003</td><td>A00062${index}</td><td>WestRock白面牛卡纸75°</td><td>135 g × 900 mm</td><td class="delivery-print-table__weight">1203 kg</td><td>-</td></tr>`,
    ).join('')
  })
  await page.locator('.delivery-print-table__total td:first-child').evaluate((cell) => {
    cell.textContent = '合计：30 卷'
  })

  await expect(page.locator('.delivery-print-table thead')).toHaveCSS('display', 'table-header-group')
  await expect(page.locator('#delivery-rows tr').first()).toHaveCSS('break-inside', 'avoid')
  await expect(page.locator('.delivery-print-table__total')).toHaveCSS('break-inside', 'avoid')
  await expect(page.locator('.delivery-print-signatures')).toHaveCSS('break-inside', 'avoid')
  await page.evaluate(() => {
    document.documentElement.style.setProperty('--delivery-print-number', '"CK202607290001"')
    document.documentElement.style.setProperty('--delivery-print-time', '"打印时间：2026-07-29 19:43"')
  })

  const pdf = await page.pdf({ format: 'A4', path: testInfo.outputPath('delivery-print-30-rows.pdf'), printBackground: true })
  const pageCount = pdf.toString('latin1').match(/\/Type ?\/Page\b/g)?.length ?? 0
  expect(pageCount).toBe(2)
})
