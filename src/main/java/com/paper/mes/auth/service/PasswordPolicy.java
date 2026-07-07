package com.paper.mes.auth.service;

public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 32;
    public static final String MESSAGE = "密码需为8-32位，且包含字母和数字";
    public static final String PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).{8,32}$";

    private PasswordPolicy() {
    }

    public static boolean isStrong(String password) {
        return password != null && password.matches(PATTERN);
    }
}
