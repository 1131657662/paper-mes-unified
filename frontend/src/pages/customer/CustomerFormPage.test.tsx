import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { renderToStaticMarkup } from 'react-dom/server'
import { createMemoryRouter, RouterProvider } from 'react-router'
import { describe, expect, it } from 'vitest'
import CustomerFormPage from './CustomerFormPage'

describe('customer form page', () => {
  it('renders the form immediately in create mode while the detail query is disabled', () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    })
    const router = createMemoryRouter([
      { path: '/customers/create', element: <CustomerFormPage mode="create" /> },
    ], { initialEntries: ['/customers/create'] })

    const markup = renderToStaticMarkup(
      <QueryClientProvider client={queryClient}>
        <RouterProvider router={router} />
      </QueryClientProvider>,
    )

    expect(markup).toContain('customer-profile-form')
    expect(markup).not.toContain('ant-skeleton')
  })
})
