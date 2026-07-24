import { expect, test } from '@playwright/test'

const documentPrintStyles = 'src/pages/documentModule.css'
const processOrderPrintStyles = 'src/features/processOrderDetail/components/PrintPreviewSheet.print.css'

test.beforeEach(async ({ page }) => {
  await page.emulateMedia({ media: 'print' })
  await page.setContent('<main id="root"></main>')
  await page.addStyleTag({ path: documentPrintStyles })
  await page.addStyleTag({ path: processOrderPrintStyles })
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
