package me.skriva.ceph.xmpp;

import me.skriva.ceph.entities.Account;

public interface OnAdvancedStreamFeaturesLoaded {
	public void onAdvancedStreamFeaturesAvailable(final Account account);
}
