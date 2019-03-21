package me.skriva.ceph.xmpp;

import me.skriva.ceph.entities.Account;
import me.skriva.ceph.xmpp.stanzas.IqPacket;

public interface OnIqPacketReceived extends PacketReceived {
	void onIqPacketReceived(Account account, IqPacket packet);
}
