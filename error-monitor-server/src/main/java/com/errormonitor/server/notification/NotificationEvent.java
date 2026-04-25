package com.errormonitor.server.notification;

import com.errormonitor.server.error.group.ErrorGroup;

public class NotificationEvent {

    public enum Type {
        NEW_ERROR,
        REGRESSED
    }

    private final ErrorGroup errorGroup;
    private final Type type;

    public NotificationEvent(ErrorGroup errorGroup, Type type) {
        this.errorGroup = errorGroup;
        this.type = type;
    }

    public ErrorGroup getErrorGroup() {
        return errorGroup;
    }

    public Type getType() {
        return type;
    }
}
