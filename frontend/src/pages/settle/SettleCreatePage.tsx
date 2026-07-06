import { useState } from 'react'
import { Button, Card, DatePicker, Form, Input, Radio, Select, Space, message } from 'antd'
import type { Dayjs } from 'dayjs'
import dayjs from 'dayjs'
import { useNavigate } from 'react-router-dom'
import MesPageHeader from '../../components/layout/MesPageHeader'
import { useCustomers } from '../../features/processOrderCreate/hooks/useReferenceData'
import SettleCandidateTable from '../../features/settle/components/SettleCandidateTable'
import SettleSelectedSummary from '../../features/settle/components/SettleSelectedSummary'
import { useCreateSettleByMonth } from '../../features/settle/hooks/useCreateSettleByMonth'
import { useCreateSettleByOrders } from '../../features/settle/hooks/useCreateSettleByOrders'
import { useSettleCandidates } from '../../features/settle/hooks/useSettleCandidates'
import { selectedTotals } from '../../features/settle/utils/settleFormatters'
import { DICT_TYPES, invoiceFallbackOptions } from '../../features/systemConfig/configFallbacks'
import { useNumberDictOptions } from '../../features/systemConfig/hooks/useRuntimeDictOptions'
import type { SettleCandidateQuery } from '../../types/settle'
import '../documentModule.css'

interface SettleCreateForm {
  customerUuid?: string
  createMode: 'selected' | 'month'
  isInvoice: number
  period?: [Dayjs, Dayjs] | null
  remark?: string
  settleDate: Dayjs
}

export default function SettleCreatePage() {
  const [form] = Form.useForm<SettleCreateForm>()
  const navigate = useNavigate()
  const customersQuery = useCustomers()
  const { options: invoiceOptions } = useNumberDictOptions(DICT_TYPES.invoiceType, invoiceFallbackOptions)
  const createByOrdersMutation = useCreateSettleByOrders()
  const createByMonthMutation = useCreateSettleByMonth()
  const [candidateQuery, setCandidateQuery] = useState<SettleCandidateQuery>({})
  const [selectedRowKeys, setSelectedRowKeys] = useState<React.Key[]>([])
  const createMode = Form.useWatch('createMode', form) ?? 'selected'
  const isMonthMode = createMode === 'month'
  const canLoadCandidates = !isMonthMode
    || Boolean(candidateQuery.customerUuid && candidateQuery.periodStart && candidateQuery.periodEnd)
  const candidatesQuery = useSettleCandidates(candidateQuery, canLoadCandidates)
  const candidates = canLoadCandidates ? candidatesQuery.data ?? [] : []
  const selectedCandidates = candidates.filter((item) => selectedRowKeys.includes(item.orderUuid))
  const previewCandidates = isMonthMode ? candidates : selectedCandidates
  const totals = selectedTotals(previewCandidates)

  const handleValuesChange = (_: Partial<SettleCreateForm>, values: SettleCreateForm) => {
    setCandidateQuery({
      customerUuid: values.customerUuid,
      periodEnd: values.period?.[1]?.format('YYYY-MM-DD'),
      periodStart: values.period?.[0]?.format('YYYY-MM-DD'),
    })
    setSelectedRowKeys([])
  }

  const handleSubmit = async () => {
    const values = await form.validateFields()
    if (values.createMode === 'month') {
      await submitByMonth(values)
      return
    }
    await submitBySelectedOrders(values)
  }

  const submitBySelectedOrders = async (values: SettleCreateForm) => {
    if (selectedCandidates.length === 0) {
      message.warning('请先勾选需要结算的加工单')
      return
    }
    if (selectedCandidates.some((item) => Number(item.totalAmount ?? 0) <= 0)) {
      message.warning('存在待核价加工单，请先完成加工费计算后再结算')
      return
    }
    const uuid = await createByOrdersMutation.mutateAsync({
      isInvoice: values.isInvoice,
      orderUuids: selectedCandidates.map((item) => item.orderUuid),
      periodEnd: values.period?.[1]?.format('YYYY-MM-DD'),
      periodStart: values.period?.[0]?.format('YYYY-MM-DD'),
      remark: values.remark,
      settleDate: values.settleDate.format('YYYY-MM-DD'),
    })
    message.success('结算单已生成')
    navigate(`/settle-orders/${uuid}`)
  }

  const submitByMonth = async (values: SettleCreateForm) => {
    if (!values.customerUuid || !values.period) {
      message.warning('按月结算需要选择客户和账期')
      return
    }
    if (candidates.length === 0) {
      message.warning('该账期暂无可结算加工单')
      return
    }
    if (candidates.some((item) => Number(item.totalAmount ?? 0) <= 0)) {
      message.warning('账期内存在待核价加工单，请先完成加工费计算后再结算')
      return
    }
    const uuid = await createByMonthMutation.mutateAsync({
      customerUuid: values.customerUuid,
      isInvoice: values.isInvoice,
      periodEnd: values.period[1].format('YYYY-MM-DD'),
      periodStart: values.period[0].format('YYYY-MM-DD'),
      remark: values.remark,
      settleDate: values.settleDate.format('YYYY-MM-DD'),
    })
    message.success('月结结算单已生成')
    navigate(`/settle-orders/${uuid}`)
  }

  const isSubmitting = createByOrdersMutation.isPending || createByMonthMutation.isPending

  return (
    <div className="document-module-page">
      <MesPageHeader
        title="新建结算单"
        description="从已完成且未结算的加工单中勾选生成客户结算单，费用仍以后端计算和加工单落库金额为准。"
        onBack={() => navigate('/settle-orders')}
        actions={(
          <Space>
            <Button onClick={() => navigate('/settle-orders')}>取消</Button>
            <Button type="primary" loading={isSubmitting} onClick={handleSubmit}>生成结算单</Button>
          </Space>
        )}
      />

      <Card className="document-module-card" title="基础信息">
        <Form
          form={form}
          layout="vertical"
          initialValues={{ createMode: 'selected', isInvoice: 2, settleDate: dayjs() }}
          onValuesChange={handleValuesChange}
        >
          <div className="document-module-grid">
            <Form.Item name="createMode" label="创建方式">
              <Radio.Group>
                <Radio.Button value="selected">勾选加工单</Radio.Button>
                <Radio.Button value="month">按月自动圈单</Radio.Button>
              </Radio.Group>
            </Form.Item>
            <Form.Item name="customerUuid" label="客户">
              <Select
                allowClear
                showSearch
                loading={customersQuery.isLoading}
                placeholder="全部客户"
                options={(customersQuery.data?.records ?? []).map((item) => ({
                  label: item.customerName,
                  value: item.uuid,
                }))}
                optionFilterProp="label"
              />
            </Form.Item>
            <Form.Item name="period" label="加工日期范围">
              <DatePicker.RangePicker />
            </Form.Item>
            <Form.Item
              name="settleDate"
              label="结算日期"
              rules={[{ required: true, message: '请选择结算日期' }]}
            >
              <DatePicker />
            </Form.Item>
            <Form.Item name="isInvoice" label="是否开票">
              <Radio.Group>
                {invoiceOptions.map((item) => (
                  <Radio.Button key={item.value} value={item.value}>
                    {item.label}
                  </Radio.Button>
                ))}
              </Radio.Group>
            </Form.Item>
            <Form.Item className="document-module-grid__full" name="remark" label="备注">
              <Input.TextArea rows={2} placeholder="结算备注" />
            </Form.Item>
          </div>
        </Form>
      </Card>

      <Card
        className="document-module-card"
        title={isMonthMode ? '账期候选预览' : '选择加工单'}
        extra={<SettleSelectedSummary label={isMonthMode ? '候选' : '已选'} count={totals.orderCount} amount={totals.total} />}
      >
        <div className="document-module-table">
          <SettleCandidateTable
            data={candidates}
            loading={candidatesQuery.isLoading || candidatesQuery.isFetching}
            selectable={!isMonthMode}
            scrollY={460}
            selectedRowKeys={selectedRowKeys}
            onSelectionChange={setSelectedRowKeys}
          />
        </div>
      </Card>
    </div>
  )
}
