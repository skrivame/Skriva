package me.skriva.ceph.xmpp;

import me.skriva.ceph.entities.Account;

public interface OnStatusChanged {
	public void onStatusChanged(Account account);
}
