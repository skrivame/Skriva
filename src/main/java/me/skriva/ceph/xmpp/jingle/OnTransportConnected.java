package me.skriva.ceph.xmpp.jingle;

public interface OnTransportConnected {
	public void failed();

	public void established();
}
