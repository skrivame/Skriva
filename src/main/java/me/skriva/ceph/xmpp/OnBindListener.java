package me.skriva.ceph.xmpp;

import me.skriva.ceph.entities.Account;

public interface OnBindListener {
	void onBind(Account account);
}
