export interface RuntimeVersion {
  backendVersion: string
  frontendVersion: string
  gitSha: string
  buildTime: string
  databaseVersion: string
  expectedDatabaseVersion: string
  databaseReady: boolean
}
