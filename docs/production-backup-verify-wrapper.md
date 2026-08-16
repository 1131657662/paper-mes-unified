# Controlled backup verification

The application service account must not receive `SUPER`. MySQL with binary
logging enabled and `log_bin_trust_function_creators=OFF` may require a global
privilege while importing triggers, so isolated verification is launched through
the root-owned wrapper only. The wrapper uses the root-only
`/etc/mysql/debian.cnf` to temporarily set
`log_bin_trust_function_creators=1`, restores its original value in an EXIT
trap, and fails the operation if that restoration fails.

Before installation, create `/etc/paper-mes/backup-restore.env` with owner
`root:root`, mode `0600`, and values from
`deploy/backup-restore.env.example`. The file must use a dedicated restore
account and the fixed database `paper_mes_restore_check`; do not use the MES
application account.

Create the dedicated MySQL account with privileges limited to the fixed
verification schema. Grant `SESSION_VARIABLES_ADMIN` instead of `SUPER` so the
import session can disable its own binary logging without receiving global
database administration rights:

```sql
CREATE USER IF NOT EXISTS 'paper_mes_restore'@'127.0.0.1'
  IDENTIFIED BY 'CHANGE_ME_STRONG_RESTORE_PASSWORD';
GRANT ALL PRIVILEGES ON `paper_mes_restore_check`.*
  TO 'paper_mes_restore'@'127.0.0.1';
GRANT SESSION_VARIABLES_ADMIN ON *.*
  TO 'paper_mes_restore'@'127.0.0.1';
```

The global trust switch is held only for the short restore window. It is not a
permanent MySQL setting and is never written to the application environment.

Install from the checked-out release:

```bash
sudo bash deploy/install-paper-mes-backup-verify-root.example.sh production
```

Set `PAPER_MES_BACKUP_VERIFY_WRAPPER=/usr/local/sbin/verify-paper-mes-backup-root`
in `/etc/paper-mes/paper-mes.env`, restart MES, and confirm the runtime reports
the wrapper as configured. The wrapper accepts only a timestamp-shaped backup
id, always sets `DROP_AFTER_VERIFY=true`, and rejects unsupported invocation
names. It never accepts a database name or arbitrary path from the application.
It also uses a root-owned lock under `/run/lock`, writes the report to a
root-only temporary file, and then publishes the non-sensitive report as the
MES service account. This prevents root from following a service-controlled
status-file symlink.

The test environment uses the `test` mode and its own root-only configuration.
Its fixed schema is `paper_mes_test_restore_check`; grant the same privileges
to that schema for the test-only restore account.
Run the real isolation verification there before installing the production
boundary. The wrapper writes the existing `restore-check.txt` evidence file and
the verification database is removed on success or failure.
