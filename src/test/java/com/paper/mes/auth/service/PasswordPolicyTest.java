package com.paper.mes.auth.service;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordPolicyTest {

    @ParameterizedTest
    @ValueSource(strings = {"Paper2026", "A1b2c3d4", "MesAdmin2026"})
    void isStrong_withLettersAndDigits_returnsTrue(String password) {
        assertTrue(PasswordPolicy.isStrong(password));
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345678", "abcdefgh", "abc123", "        "})
    void isStrong_withWeakPassword_returnsFalse(String password) {
        assertFalse(PasswordPolicy.isStrong(password));
    }
}
