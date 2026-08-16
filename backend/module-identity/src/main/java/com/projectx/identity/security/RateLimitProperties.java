package com.projectx.identity.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "projectx.security.rate-limit")
public class RateLimitProperties {

    private int loginPerMinute = 20;
    private int refreshPerMinute = 30;
    private int driverRegisterPerMinute = 5;
    private int forgotPasswordPerMinute = 5;

    public int getLoginPerMinute() {
        return loginPerMinute;
    }

    public void setLoginPerMinute(int loginPerMinute) {
        this.loginPerMinute = loginPerMinute;
    }

    public int getRefreshPerMinute() {
        return refreshPerMinute;
    }

    public void setRefreshPerMinute(int refreshPerMinute) {
        this.refreshPerMinute = refreshPerMinute;
    }

    public int getDriverRegisterPerMinute() {
        return driverRegisterPerMinute;
    }

    public void setDriverRegisterPerMinute(int driverRegisterPerMinute) {
        this.driverRegisterPerMinute = driverRegisterPerMinute;
    }

    public int getForgotPasswordPerMinute() {
        return forgotPasswordPerMinute;
    }

    public void setForgotPasswordPerMinute(int forgotPasswordPerMinute) {
        this.forgotPasswordPerMinute = forgotPasswordPerMinute;
    }
}
