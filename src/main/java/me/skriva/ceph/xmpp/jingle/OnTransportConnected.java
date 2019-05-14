package me.skriva.ceph.xmpp.jingle;

interface OnTransportConnected {
	void failed();

	void established();
}
