package me.skriva.ceph.xmpp.jingle;

import me.skriva.ceph.entities.Account;
import me.skriva.ceph.xmpp.PacketReceived;
import me.skriva.ceph.xmpp.jingle.stanzas.JinglePacket;

public interface OnJinglePacketReceived extends PacketReceived {
	void onJinglePacketReceived(Account account, JinglePacket packet);
}
