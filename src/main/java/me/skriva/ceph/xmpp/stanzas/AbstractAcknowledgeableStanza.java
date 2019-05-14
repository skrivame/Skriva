package me.skriva.ceph.xmpp.stanzas;

import me.skriva.ceph.xml.Element;
import me.skriva.ceph.xmpp.InvalidJid;

abstract public class AbstractAcknowledgeableStanza extends AbstractStanza {

	AbstractAcknowledgeableStanza(String name) {
		super(name);
	}


	public String getId() {
		return this.getAttribute("id");
	}

	public void setId(final String id) {
		setAttribute("id", id);
	}

	public Element getError() {
		Element error = findChild("error");
		if (error != null) {
			for(Element element : error.getChildren()) {
				if (!element.getName().equals("text")) {
					return element;
				}
			}
		}
		return null;
	}

	public boolean valid() {
		return InvalidJid.isValid(getFrom()) && InvalidJid.isValid(getTo());
	}
}
