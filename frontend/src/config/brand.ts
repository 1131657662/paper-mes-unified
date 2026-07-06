export const APP_BRAND = {
  name: '纸品智造 MES',
  productLine: 'PaperFlow SaaS',
  tagline: '卷筒纸加工数字化工作台',
} as const

export function buildDocumentTitle(pageTitle?: string): string {
  return pageTitle ? `${pageTitle} - ${APP_BRAND.name}` : APP_BRAND.name
}
