package me.skriva.ceph.xmpp;

import me.skriva.ceph.entities.Account;
import me.skriva.ceph.xmpp.stanzas.MessagePacket;

public interface OnMessagePacketReceived extends PacketReceived {
	public void onMessagePacketReceived(Account account, MessagePacket packet);
}
