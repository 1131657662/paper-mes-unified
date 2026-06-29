package com.paper.mes.auth.context;

import com.paper.mes.auth.dto.CurrentUser;

public final class AuthContextHolder {

    private static final ThreadLocal<CurrentUser> HOLDER = new ThreadLocal<>();

    private AuthContextHolder() {
    }

    public static CurrentUser getCurrentUser() {
        return HOLDER.get();
    }

    public static String currentDisplayName() {
        CurrentUser user = HOLDER.get();
        if (user == null) {
            return "system";
        }
        return user.getRealName() == null || user.getRealName().isBlank()
                ? user.getUsername()
                : user.getRealName();
    }

    public static void setCurrentUser(CurrentUser user) {
        HOLDER.set(user);
    }

    public static void clear() {
        HOLDER.remove();
    }
}
