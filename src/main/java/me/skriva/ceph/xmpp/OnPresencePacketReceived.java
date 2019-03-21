package me.skriva.ceph.xmpp;

import me.skriva.ceph.entities.Account;
import me.skriva.ceph.xmpp.stanzas.PresencePacket;

public interface OnPresencePacketReceived extends PacketReceived {
	public void onPresencePacketReceived(Account account, PresencePacket packet);
}
