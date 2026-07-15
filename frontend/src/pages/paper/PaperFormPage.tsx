import { Button, Card, Form, Skeleton, Space, message } from 'antd'
import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import { createPaper, getPaper, updatePaper } from '../../api/paper'
import MesPageHeader from '../../components/layout/MesPageHeader'
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
  const { uuid } = useParams()
  const [loading, setLoading] = useState(mode === 'edit')
  const [submitting, setSubmitting] = useState(false)
  const isEdit = mode === 'edit'
  const { clearDirty, markDirty } = useUnsavedChangesGuard()

  useEffect(() => {
    if (!isEdit || !uuid) return
    setLoading(true)
    getPaper(uuid)
      .then((data) => form.setFieldsValue(toFormValues(data)))
      .finally(() => setLoading(false))
  }, [form, isEdit, uuid])

  const submit = async (values: PaperSaveDTO) => {
    setSubmitting(true)
    try {
      const savedUuid = isEdit && uuid ? uuid : await createPaper(values)
      if (isEdit && uuid) await updatePaper(uuid, values)
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
        {loading ? (
          <Skeleton active paragraph={{ rows: 6 }} />
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
