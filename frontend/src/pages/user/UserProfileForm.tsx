import { Form, Input, Select } from 'antd'
import type { FormInstance } from 'antd'
import type { UserSaveDTO } from '../../types/user'
import { userRoleOptions, userStatusOptions } from './userDisplay'

interface Props {
  editing: boolean
  form: FormInstance<UserSaveDTO>
  onFinish?: (values: UserSaveDTO) => void
}

const userFormDefaults: Partial<UserSaveDTO> = {
  roleCode: 'operator',
  status: 1,
}

export default function UserProfileForm({ editing, form, onFinish }: Props) {
  return (
    <Form
      className="mes-modal-form user-profile-form"
      form={form}
      initialValues={userFormDefaults}
      layout="vertical"
      onFinish={onFinish}
    >
      <section className="user-profile-form__section">
        <h3>账号信息</h3>
        <div className="mes-form-grid">
          <Form.Item
            name="username"
            label="登录账号"
            rules={[{ required: true, message: '请输入登录账号' }]}
          >
            <Input placeholder="如 zhangsan" disabled={editing} />
          </Form.Item>
          <Form.Item
            name="realName"
            label="姓名"
            rules={[{ required: true, message: '请输入姓名' }]}
          >
            <Input placeholder="请输入姓名" />
          </Form.Item>
          {!editing && (
            <Form.Item
              name="password"
              label="初始密码"
              rules={[
                { required: true, message: '请输入初始密码' },
                { min: 6, max: 32, message: '密码长度需为6-32位' },
              ]}
            >
              <Input.Password placeholder="请输入初始密码" />
            </Form.Item>
          )}
        </div>
      </section>

      <section className="user-profile-form__section">
        <h3>权限与状态</h3>
        <div className="mes-form-grid">
          <Form.Item
            name="roleCode"
            label="系统角色"
            rules={[{ required: true, message: '请选择系统角色' }]}
          >
            <Select options={userRoleOptions} placeholder="请选择角色" />
          </Form.Item>
          <Form.Item name="status" label="账号状态">
            <Select options={userStatusOptions} placeholder="请选择状态" />
          </Form.Item>
          <Form.Item className="mes-form-grid__full" name="remark" label="备注">
            <Input.TextArea rows={3} placeholder="岗位、权限说明或交接备注" />
          </Form.Item>
        </div>
      </section>
    </Form>
  )
}
