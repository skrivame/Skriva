package me.skriva.ceph.xmpp;

import me.skriva.ceph.entities.Account;

public interface OnMessageAcknowledged {
	boolean onMessageAcknowledged(Account account, String id);
}
