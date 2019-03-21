package me.skriva.ceph.xmpp.jingle.stanzas;

import me.skriva.ceph.xml.Element;

public class Reason extends Element {
	private Reason(String name) {
		super(name);
	}

	public Reason() {
		super("reason");
	}
}
