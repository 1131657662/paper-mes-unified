import { renderToStaticMarkup } from 'react-dom/server'
import { describe, expect, it } from 'vitest'
import SettleCollectionReminderHistory from './SettleCollectionReminderHistory'

describe('结算催收历史', () => {
  it('显示联系渠道、结果、联系人和下次跟进日期', () => {
    const markup = renderToStaticMarkup(
      <SettleCollectionReminderHistory
        error={false}
        items={[{
          uuid: 'reminder-1',
          reminderChannel: 2,
          reminderResult: 3,
          contactName: '张经理',
          reminderTime: '2026-07-19T09:30:00',
          nextFollowUpDate: '2026-07-21',
          operatorName: '财务员',
          remark: '客户承诺周二安排付款',
        }]}
        loading={false}
        onRetry={() => undefined}
      />,
    )

    expect(markup).toContain('微信')
    expect(markup).toContain('承诺付款')
    expect(markup).toContain('联系人：张经理')
    expect(markup).toContain('下次跟进 2026-07-21')
    expect(markup).toContain('客户承诺周二安排付款')
  })
})
