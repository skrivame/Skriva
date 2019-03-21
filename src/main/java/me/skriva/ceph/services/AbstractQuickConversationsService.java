package me.skriva.ceph.services;

import me.skriva.ceph.BuildConfig;

public abstract class AbstractQuickConversationsService {

    protected final XmppConnectionService service;

    public AbstractQuickConversationsService(XmppConnectionService service) {
        this.service = service;
    }

    public abstract void considerSync();

    public static boolean isConversations() {
        return "skriva".equals(BuildConfig.FLAVOR_mode);
    }

    public abstract void signalAccountStateChange();

    public abstract boolean isSynchronizing();

    public abstract void considerSyncBackground(boolean force);
}
