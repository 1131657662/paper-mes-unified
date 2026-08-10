import { Button, Card, Empty, Form, Skeleton, Space, message } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { useQueryClient } from '@tanstack/react-query'
import { createMachine, updateMachine } from '../../api/machine'
import { isNotFoundError } from '../../api/request'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import MesPageHeader from '../../components/layout/MesPageHeader'
import { useMachineDetail } from '../../features/machine/hooks/useMachineDetail'
import { machineKeys } from '../../features/machine/queries/machineKeys'
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
  const queryClient = useQueryClient()
  const { uuid } = useParams()
  const [submitting, setSubmitting] = useState(false)
  const isEdit = mode === 'edit'
  const { clearDirty, markDirty } = useUnsavedChangesGuard()
  const {
    data: machine,
    error: machineError,
    isError: isMachineError,
    isPending: isLoadingMachine,
    refetch: refetchMachine,
  } = useMachineDetail(isEdit ? uuid : undefined)

  useEffect(() => {
    if (machine) form.setFieldsValue(toFormValues(machine))
  }, [form, machine])

  const submit = async (values: MachineSaveDTO) => {
    setSubmitting(true)
    try {
      const savedUuid = isEdit && uuid ? uuid : await createMachine(values)
      if (isEdit && uuid) await updateMachine(uuid, values)
      if (isEdit && uuid) {
        await queryClient.invalidateQueries({ queryKey: machineKeys.detail(uuid).queryKey })
      }
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
        title={isEdit ? '编辑机台' : '新增机台'}
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
        {isLoadingMachine ? (
          <Skeleton active paragraph={{ rows: 6 }} />
        ) : isMachineError && !isNotFoundError(machineError) ? (
          <QueryLoadErrorAlert
            message="生产资源加载失败"
            description="请检查网络或服务状态后重新加载。"
            onRetry={() => { void refetchMachine() }}
          />
        ) : isMachineError || (isEdit && !machine) ? (
          <Empty description="生产资源不存在" />
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
