interface MutationCommitOptions<T> {
  ensureVersionReady: () => Promise<void>
  markVersionSyncRequired: () => void
  mutate: () => Promise<T>
  synchronizeVersion: () => Promise<void>
}

export async function runVersionSynchronizedMutation<T>(
  options: MutationCommitOptions<T>,
): Promise<T> {
  await options.ensureVersionReady()
  const result = await options.mutate()
  options.markVersionSyncRequired()
  await options.synchronizeVersion()
  return result
}
