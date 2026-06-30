import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { RouterProvider } from 'react-router-dom'
import { QueryClientProvider } from '@tanstack/react-query'
import 'antd/dist/reset.css'
import './styles/app-shell.css'
import './styles/mes-theme.css'
import { router } from './router'
import { ErrorBoundary } from './components/ErrorBoundary'
import { queryClient } from './app/queryClient'

const antdLocale = {
  ...zhCN,
  Pagination: {
    ...zhCN.Pagination,
    items_per_page: '',
  },
}

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <ConfigProvider
          locale={antdLocale}
          theme={{
            token: {
              colorPrimary: '#1677ff',
              colorPrimaryHover: '#4096ff',
              colorText: '#172033',
              colorTextSecondary: '#526579',
              colorBorder: '#d9e2ec',
              colorBgLayout: '#f5f7fa',
              borderRadius: 8,
              fontSize: 14,
            },
            components: {
              Button: {
                defaultShadow: 'none',
                primaryShadow: 'none',
              },
              Card: {
                bodyPadding: 16,
                headerFontSize: 18,
                headerHeight: 48,
              },
              Drawer: {
                footerPaddingBlock: 12,
                footerPaddingInline: 16,
              },
              Modal: {
                titleFontSize: 16,
              },
              Table: {
                borderColor: '#d9e2ec',
                headerBg: '#f7f9fc',
                rowHoverBg: '#f8fbff',
              },
            },
          }}
        >
          <RouterProvider router={router} />
        </ConfigProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  </StrictMode>,
)
