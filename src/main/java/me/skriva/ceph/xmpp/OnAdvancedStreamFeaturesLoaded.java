package me.skriva.ceph.xmpp;

import me.skriva.ceph.entities.Account;

public interface OnAdvancedStreamFeaturesLoaded {
	void onAdvancedStreamFeaturesAvailable(final Account account);
}
