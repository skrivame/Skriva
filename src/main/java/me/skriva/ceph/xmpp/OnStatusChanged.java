package me.skriva.ceph.xmpp;

import me.skriva.ceph.entities.Account;

public interface OnStatusChanged {
	void onStatusChanged(Account account);
}
