import { Button, Card, Form, Skeleton, Space, message } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { createMachine, getMachine, updateMachine } from '../../api/machine'
import MesPageHeader from '../../components/layout/MesPageHeader'
import type { Machine, MachineSaveDTO } from '../../types/machine'
import '../documentModule.css'
import MachineProfileForm from './MachineProfileForm'
import './MachineProfile.css'

interface Props {
  mode: 'create' | 'edit'
}

export default function MachineFormPage({ mode }: Props) {
  const [form] = Form.useForm<MachineSaveDTO>()
  const navigate = useNavigate()
  const { uuid } = useParams()
  const [loading, setLoading] = useState(mode === 'edit')
  const [submitting, setSubmitting] = useState(false)
  const isEdit = mode === 'edit'

  useEffect(() => {
    if (!isEdit || !uuid) return
    setLoading(true)
    getMachine(uuid)
      .then((data) => form.setFieldsValue(toFormValues(data)))
      .finally(() => setLoading(false))
  }, [form, isEdit, uuid])

  const submit = async (values: MachineSaveDTO) => {
    setSubmitting(true)
    try {
      const savedUuid = isEdit && uuid ? uuid : await createMachine(values)
      if (isEdit && uuid) await updateMachine(uuid, values)
      message.success(isEdit ? '机台档案已保存' : '机台档案已新增')
      navigate(`/machines/${savedUuid}`)
    } finally {
      setSubmitting(false)
    }
  }

  const backPath = isEdit && uuid ? `/machines/${uuid}` : '/machines'

  return (
    <div className="document-module-page machine-profile-page">
      <MesPageHeader
        title={isEdit ? '编辑机台档案' : '新增机台档案'}
        description="维护锯纸、复卷和通用机台资料。加工单下发和回录时可按机台状态与类型识别可用设备。"
        onBack={() => navigate(backPath)}
        actions={(
          <Space>
            <Button onClick={() => navigate(backPath)}>取消</Button>
            <Button type="primary" loading={submitting} onClick={() => form.submit()}>
              保存机台
            </Button>
          </Space>
        )}
      />

      <Card className="document-module-card machine-profile-card" title="机台资料">
        {loading ? (
          <Skeleton active paragraph={{ rows: 6 }} />
        ) : (
          <MachineProfileForm editing={isEdit} form={form} onFinish={submit} />
        )}
      </Card>
    </div>
  )
}

function toFormValues(machine: Machine): MachineSaveDTO {
  return {
    machineCode: machine.machineCode,
    machineName: machine.machineName,
    machineType: machine.machineType,
    remark: machine.remark,
    status: machine.status,
  }
}
