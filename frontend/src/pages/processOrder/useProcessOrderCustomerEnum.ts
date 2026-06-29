import { useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'

export function useProcessOrderCustomerEnum() {
  const { data: customerPage } = useCustomers()
  const customerEnum: Record<string, { text: string }> = {}
  ;(customerPage?.records ?? []).forEach((customer) => {
    customerEnum[customer.uuid] = { text: customer.customerName }
  })
  return customerEnum
}
