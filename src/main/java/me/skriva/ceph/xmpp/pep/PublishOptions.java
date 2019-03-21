package me.skriva.ceph.xmpp.pep;

import android.os.Bundle;

import me.skriva.ceph.xml.Element;
import me.skriva.ceph.xml.Namespace;
import me.skriva.ceph.xmpp.stanzas.IqPacket;

public class PublishOptions {

    private PublishOptions() {

    }

    public static Bundle openAccess() {
        final Bundle options = new Bundle();
        options.putString("pubsub#access_model","open");
        return options;
    }

    public static Bundle persistentWhitelistAccess() {
        final Bundle options = new Bundle();
        options.putString("pubsub#persist_items","true");
        options.putString("pubsub#access_model","whitelist");
        return options;
    }

    public static boolean preconditionNotMet(IqPacket response) {
        final Element error = response.getType() == IqPacket.TYPE.ERROR ? response.findChild("error") : null;
        return error != null && error.hasChild("precondition-not-met", Namespace.PUBSUB_ERROR);
    }

}
