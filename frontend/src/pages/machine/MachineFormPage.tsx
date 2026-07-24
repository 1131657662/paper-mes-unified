import { Button, Card, Form, Skeleton, Space, message } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { createMachine, getMachine, updateMachine } from '../../api/machine'
import MesPageHeader from '../../components/layout/MesPageHeader'
import { useUnsavedChangesGuard } from '../../hooks/useUnsavedChangesGuard'
import type { Machine, MachineSaveDTO } from '../../types/machine'
import '../documentModule.css'
import MachineProfileForm from './MachineProfileForm'
import { capabilitiesToForm } from './machineCapabilityModel'
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
  const { clearDirty, markDirty } = useUnsavedChangesGuard()

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
      clearDirty()
      message.success(isEdit ? '生产资源已保存' : '生产资源已新增')
      navigate(`/machines/${savedUuid}`)
    } finally {
      setSubmitting(false)
    }
  }

  const backPath = isEdit && uuid ? `/machines/${uuid}` : '/machines'

  return (
    <div className="document-module-page machine-profile-page">
      <MesPageHeader
        title={isEdit ? '编辑机台 / 工位' : '新增机台 / 工位'}
        onBack={() => navigate(backPath)}
        actions={(
          <Space>
            <Button onClick={() => navigate(backPath)}>取消</Button>
            <Button type="primary" loading={submitting} onClick={() => form.submit()}>
              保存资源
            </Button>
          </Space>
        )}
      />

      <Card className="document-module-card machine-profile-card" title="生产资源资料">
        {loading ? (
          <Skeleton active paragraph={{ rows: 6 }} />
        ) : (
          <MachineProfileForm editing={isEdit} form={form} onFinish={submit} onValuesChange={markDirty} />
        )}
      </Card>
    </div>
  )
}

function toFormValues(machine: Machine): MachineSaveDTO {
  return {
    machineCode: machine.machineCode,
    machineName: machine.machineName,
    resourceKind: machine.resourceKind ?? 'MACHINE',
    capabilities: capabilitiesToForm(machine.capabilities),
    remark: machine.remark,
    status: machine.status,
  }
}
