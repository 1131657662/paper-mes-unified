export function buildAiPageTemplate(pathname: string): string {
  const segment = pathname.split('/').filter(Boolean)[0] ?? 'dashboard'
  return segment.toLowerCase().replace(/[^a-z0-9-]/g, '-').slice(0, 64) || 'dashboard'
}

export function buildAiContextEpoch(locationKey: string): string {
  return locationKey.replace(/[^a-zA-Z0-9-]/g, '-').slice(0, 64) || 'default'
}
