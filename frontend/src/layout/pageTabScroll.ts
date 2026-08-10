export interface TabScrollState {
  canScroll: boolean
  canScrollLeft: boolean
  canScrollRight: boolean
}

export const emptyScrollState: TabScrollState = {
  canScroll: false,
  canScrollLeft: false,
  canScrollRight: false,
}

export function scrollTabs(container: HTMLDivElement | null, deltaX: number, onSettled: () => void) {
  const scroller = container?.querySelector('.ant-tabs-nav-wrap')
  if (!scroller || deltaX === 0) return

  let remaining = Math.abs(deltaX)
  const direction = Math.sign(deltaX)
  const stepSize = 28
  const scrollStep = () => {
    const nextDelta = direction * Math.min(stepSize, remaining)
    scroller.dispatchEvent(new WheelEvent('wheel', { bubbles: true, cancelable: true, deltaX: nextDelta, deltaY: 0 }))
    remaining -= Math.abs(nextDelta)

    if (remaining > 0) {
      requestAnimationFrame(scrollStep)
      return
    }
    onSettled()
  }

  requestAnimationFrame(scrollStep)
}

export function watchTabScrollState(container: HTMLDivElement | null, onChange: (state: TabScrollState) => void) {
  if (!container) return undefined

  const update = () => onChange(readTabScrollState(container))
  const resizeObserver = new ResizeObserver(update)
  const mutationObserver = new MutationObserver(update)
  const targets = getTabScrollTargets(container)

  targets.forEach((target) => resizeObserver.observe(target))
  targets.forEach((target) => mutationObserver.observe(target, { attributes: true, attributeFilter: ['class', 'style'] }))
  window.addEventListener('resize', update)

  const frame = requestAnimationFrame(update)

  return () => {
    cancelAnimationFrame(frame)
    resizeObserver.disconnect()
    mutationObserver.disconnect()
    window.removeEventListener('resize', update)
  }
}

export function readTabScrollState(container: HTMLDivElement | null): TabScrollState {
  const wrap = container?.querySelector('.ant-tabs-nav-wrap')
  const list = container?.querySelector('.ant-tabs-nav-list')
  if (!(wrap instanceof HTMLElement) || !(list instanceof HTMLElement)) return emptyScrollState

  const canScrollLeft = wrap.classList.contains('ant-tabs-nav-wrap-ping-left')
  const canScrollRight = wrap.classList.contains('ant-tabs-nav-wrap-ping-right')
  const canScroll = canScrollLeft || canScrollRight || list.getBoundingClientRect().width > wrap.getBoundingClientRect().width + 1
  return { canScroll, canScrollLeft, canScrollRight }
}

function getTabScrollTargets(container: HTMLDivElement) {
  return [container, ...container.querySelectorAll('.ant-tabs-nav-wrap, .ant-tabs-nav-list')]
}
