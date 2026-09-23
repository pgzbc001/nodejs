package com.mdm.platform.security;

/**
 * 请求级用户上下文（由 PermissionInterceptor 从 X-User-* 头解析填充）。
 */
public class UserContext {

    private static final ThreadLocal<UserContext> HOLDER = new ThreadLocal<>();

    private String userId;
    private String userName;
    private Role role;

    public UserContext(String userId, String userName, Role role) {
        this.userId = userId;
        this.userName = userName;
        this.role = role;
    }

    public static void set(UserContext ctx) {
        HOLDER.set(ctx);
    }

    public static UserContext get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** 操作人显示名（优先姓名）。 */
    public String operator() {
        return userName != null && !userName.isBlank() ? userName : userId;
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public Role getRole() {
        return role;
    }
}
