import { Button, Card, Form, Skeleton, Space, message } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { createCustomer, getCustomer, updateCustomer } from '../../api/customer'
import MesPageHeader from '../../components/layout/MesPageHeader'
import { useUnsavedChangesGuard } from '../../hooks/useUnsavedChangesGuard'
import type { Customer, CustomerSaveDTO } from '../../types/customer'
import CustomerProfileForm from './CustomerProfileForm'
import '../documentModule.css'
import './CustomerProfile.css'

interface Props {
  mode: 'create' | 'edit'
}

export default function CustomerFormPage({ mode }: Props) {
  const [form] = Form.useForm<CustomerSaveDTO>()
  const navigate = useNavigate()
  const { uuid } = useParams()
  const [loading, setLoading] = useState(mode === 'edit')
  const [submitting, setSubmitting] = useState(false)
  const isEdit = mode === 'edit'
  const { clearDirty, markDirty } = useUnsavedChangesGuard()

  useEffect(() => {
    if (!isEdit || !uuid) return
    setLoading(true)
    getCustomer(uuid)
      .then((data) => {
        form.setFieldsValue(toFormValues(data))
      })
      .finally(() => setLoading(false))
  }, [form, isEdit, uuid])

  const submit = async (values: CustomerSaveDTO) => {
    setSubmitting(true)
    try {
      const savedUuid = isEdit && uuid ? uuid : await createCustomer(values)
      if (isEdit && uuid) await updateCustomer(uuid, values)
      clearDirty()
      message.success(isEdit ? '客户资料已保存' : '客户已新增')
      navigate(`/customers/${savedUuid}`)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="document-module-page customer-profile-page">
      <MesPageHeader
        title={isEdit ? '编辑客户档案' : '新增客户档案'}
        onBack={() => navigate(isEdit && uuid ? `/customers/${uuid}` : '/customers')}
        actions={(
          <Space>
            <Button onClick={() => navigate(isEdit && uuid ? `/customers/${uuid}` : '/customers')}>取消</Button>
            <Button type="primary" loading={submitting} onClick={() => form.submit()}>
              保存客户
            </Button>
          </Space>
        )}
      />

      <Card className="document-module-card customer-profile-card" title="客户资料">
        {loading ? (
          <Skeleton active paragraph={{ rows: 8 }} />
        ) : (
          <CustomerProfileForm editing={isEdit} form={form} onFinish={submit} onValuesChange={markDirty} />
        )}
      </Card>
    </div>
  )
}

function toFormValues(customer: Customer): CustomerSaveDTO {
  return {
    bankAccount: customer.bankAccount,
    contact: customer.contact,
    customerCode: customer.customerCode,
    customerLevel: customer.customerLevel,
    customerName: customer.customerName,
    defaultInvoice: customer.defaultInvoice,
    deliveryAddress: customer.deliveryAddress,
    exportTemplate: customer.exportTemplate,
    invoiceAddress: customer.invoiceAddress,
    phone: customer.phone,
    priceIncludeTax: customer.priceIncludeTax,
    remark: customer.remark,
    rewindPrice: customer.rewindPrice,
    sawPrice: customer.sawPrice,
    settleDay: customer.settleDay,
    settleType: customer.settleType,
    taxNo: customer.taxNo,
    taxRate: customer.taxRate,
    processPrices: customer.processPrices?.map((price) => ({
      catalogUuid: price.catalogUuid,
      billingBasis: price.billingBasis,
      price: price.price,
      isDefault: price.defaultOption ? 1 : 0,
    })),
  }
}
