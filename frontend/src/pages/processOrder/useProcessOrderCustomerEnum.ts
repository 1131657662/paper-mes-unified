import { useMemo } from 'react'
import { useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'

export function useProcessOrderCustomerEnum() {
  const { data: customerPage } = useCustomers()
  return useMemo(() => {
    const customerEnum: Record<string, { text: string }> = {}
    ;(customerPage?.records ?? []).forEach((customer) => {
      customerEnum[customer.uuid] = { text: customer.customerName }
    })
    return customerEnum
  }, [customerPage?.records])
}
