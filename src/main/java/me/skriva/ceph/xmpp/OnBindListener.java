package me.skriva.ceph.xmpp;

import me.skriva.ceph.entities.Account;

public interface OnBindListener {
	public void onBind(Account account);
}
