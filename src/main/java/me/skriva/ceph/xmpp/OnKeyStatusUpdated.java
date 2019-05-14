package me.skriva.ceph.xmpp;

import me.skriva.ceph.crypto.axolotl.AxolotlService;

public interface OnKeyStatusUpdated {
	void onKeyStatusUpdated(AxolotlService.FetchStatus report);
}
