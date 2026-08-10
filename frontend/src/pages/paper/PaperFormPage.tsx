import { Button, Card, Empty, Form, Skeleton, Space, message } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router'
import { useQueryClient } from '@tanstack/react-query'
import { createPaper, updatePaper } from '../../api/paper'
import { isNotFoundError } from '../../api/request'
import QueryLoadErrorAlert from '../../components/feedback/QueryLoadErrorAlert'
import MesPageHeader from '../../components/layout/MesPageHeader'
import { usePaperDetail } from '../../features/paper/hooks/usePaperDetail'
import { paperKeys } from '../../features/paper/queries/paperKeys'
import { useUnsavedChangesGuard } from '../../hooks/useUnsavedChangesGuard'
import type { Paper, PaperSaveDTO } from '../../types/paper'
import '../documentModule.css'
import PaperProfileForm from './PaperProfileForm'
import './PaperProfile.css'

interface Props {
  mode: 'create' | 'edit'
}

export default function PaperFormPage({ mode }: Props) {
  const [form] = Form.useForm<PaperSaveDTO>()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const { uuid } = useParams()
  const [submitting, setSubmitting] = useState(false)
  const isEdit = mode === 'edit'
  const { clearDirty, markDirty } = useUnsavedChangesGuard()
  const {
    data: paper,
    error: paperError,
    isError: isPaperError,
    isPending: isLoadingPaper,
    refetch: refetchPaper,
  } = usePaperDetail(isEdit ? uuid : undefined)

  useEffect(() => {
    if (paper) form.setFieldsValue(toFormValues(paper))
  }, [form, paper])

  const submit = async (values: PaperSaveDTO) => {
    setSubmitting(true)
    try {
      const savedUuid = isEdit && uuid ? uuid : await createPaper(values)
      if (isEdit && uuid) await updatePaper(uuid, values)
      if (isEdit && uuid) {
        await queryClient.invalidateQueries({ queryKey: paperKeys.detail(uuid).queryKey })
      }
      clearDirty()
      message.success(isEdit ? '纸张档案已保存' : '纸张档案已新增')
      navigate(`/papers/${savedUuid}`)
    } finally {
      setSubmitting(false)
    }
  }

  const backPath = isEdit && uuid ? `/papers/${uuid}` : '/papers'

  return (
    <div className="document-module-page paper-profile-page">
      <MesPageHeader
        title={isEdit ? '编辑纸张档案' : '新增纸张档案'}
        onBack={() => navigate(backPath)}
        actions={(
          <Space>
            <Button onClick={() => navigate(backPath)}>取消</Button>
            <Button type="primary" loading={submitting} onClick={() => form.submit()}>
              保存纸张
            </Button>
          </Space>
        )}
      />

      <Card className="document-module-card paper-profile-card" title="纸张资料">
        {isLoadingPaper ? (
          <Skeleton active paragraph={{ rows: 6 }} />
        ) : isPaperError && !isNotFoundError(paperError) ? (
          <QueryLoadErrorAlert
            message="纸张档案加载失败"
            description="请检查网络或服务状态后重新加载。"
            onRetry={() => { void refetchPaper() }}
          />
        ) : isPaperError || (isEdit && !paper) ? (
          <Empty description="纸张档案不存在" />
        ) : (
          <PaperProfileForm editing={isEdit} form={form} onFinish={submit} onValuesChange={markDirty} />
        )}
      </Card>
    </div>
  )
}

function toFormValues(paper: Paper): PaperSaveDTO {
  return {
    gramWeight: paper.gramWeight,
    paperCode: paper.paperCode,
    paperName: paper.paperName,
    paperType: paper.paperType,
    remark: paper.remark,
  }
}
