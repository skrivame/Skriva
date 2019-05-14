package me.skriva.ceph.xmpp;

import me.skriva.ceph.entities.Contact;

public interface OnContactStatusChanged {
	void onContactStatusChanged(final Contact contact, final boolean online);
}
