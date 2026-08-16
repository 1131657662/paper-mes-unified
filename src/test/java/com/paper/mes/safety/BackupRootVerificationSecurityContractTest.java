package com.paper.mes.safety;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BackupRootVerificationSecurityContractTest {

    @Test
    void restoreWrapper_isRootOnlyAndBoundToAnIsolatedDatabase() throws Exception {
        String wrapper = source("deploy/verify-paper-mes-backup-root.example.sh");
        String restoreEnv = source("deploy/backup-restore.env.example");
        String installer = source("deploy/install-paper-mes-backup-verify-root.example.sh");
        String cron = source("deploy/paper-mes-backup.cron.example");

        assertContainsAll(wrapper, "[ \"$(id -u)\" = 0 ]", "[ \"$#\" = 1 ]",
                "^[0-9]{8}-[0-9]{6}$", "BACKUP_ENV_FILE=", "BACKUP_DIR=",
                "RESTORE_DB_NAME=\"${restore_db}\"", "DROP_AFTER_VERIFY=true",
                "VERIFY_LOCK_FILE=/run/lock/paper-mes-backup-verify.lock",
                "VERIFY_REPORT_FILE=\"${report_tmp}\"", "/usr/bin/env -i",
                "[ -x /usr/sbin/runuser ]", "root_mysql_cnf=/etc/mysql/debian.cnf",
                "SELECT @@GLOBAL.log_bin_trust_function_creators",
                "SET GLOBAL log_bin_trust_function_creators=1",
                "SET GLOBAL log_bin_trust_function_creators=${original_trust}",
                "/usr/sbin/runuser -u \"${service_user}\"");
        assertFalse(wrapper.contains("SHA256SUMS restore-check.txt"),
                "restore-check.txt is created by verification and must not be a backup prerequisite");
        assertContainsAll(wrapper, "status_file=\"${backup_dir}/restore-check.txt\"",
                "status file must not be a symlink");
        assertContainsAll(restoreEnv, "SOURCE_DB_NAME=paper_processing",
                "DB_ADMIN_PASSWORD=CHANGE_ME_RESTORE_PASSWORD");
        assertFalse(restoreEnv.contains("RESTORE_DB_NAME="));
        assertFalse(restoreEnv.contains("DROP_AFTER_VERIFY="));
        assertFalse(restoreEnv.contains("PAPER_MES_DB_PASSWORD"));
        assertContainsAll(installer, "production)", "test)", "verify-paper-mes-backup-root",
                "verify-paper-mes-test-backup-root", "visudo -cf",
                "restore configuration must be root:root 0600");
        assertContainsAll(cron, "/usr/local/sbin/verify-paper-mes-backup-root", "-printf '%f\\n'");
        assertFalse(cron.contains("BACKUP_ENV_FILE=/etc/paper-mes/backup.env BACKUP_DIR="));
        assertContainsAll(source("deploy/verify-backup-restore.example.sh"),
                "--init-command=\"SET SESSION sql_log_bin=0\"");
        assertContainsAll(source("docs/production-backup-verify-wrapper.md"),
                "SESSION_VARIABLES_ADMIN", "instead of `SUPER`");
    }

    @Test
    void sudoersAndTestDeployment_allowOnlyTheValidatedWrapper() throws Exception {
        String sudoers = source("deploy/paper-mes-backup-verify.sudoers.example");
        String testSudoers = source("deploy/paper-mes-test-backup-verify.sudoers.example");
        String deployment = source("deploy/deploy-paper-mes-test.example.sh");

        assertContainsAll(sudoers, "paper-mes ALL=(root) NOPASSWD:",
                "verify-paper-mes-backup-root ????????-??????");
        assertContainsAll(testSudoers, "paper-mes-test ALL=(root) NOPASSWD:",
                "verify-paper-mes-test-backup-root ????????-??????");
        assertContainsAll(deployment, "install_restore_runtime", "verify-paper-mes-test-backup-root",
                "paper-mes-test-backup-verify.sudoers", "visudo -cf",
                "PAPER_MES_BACKUP_VERIFY_WRAPPER");
        assertTrue(deployment.indexOf("install_restore_runtime\n")
                > deployment.indexOf("prepare_runtime_scripts\n"));
    }

    private String source(String path) throws Exception {
        return Files.readString(Path.of(path));
    }

    private void assertContainsAll(String source, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(source.contains(fragment), "Missing backup verification guard: " + fragment);
        }
    }
}
