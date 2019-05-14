package me.skriva.ceph.xmpp;


import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Patches {
    public static final List<String> DISCO_EXCEPTIONS = Collections.singletonList(
            "nimbuzz.com"
    );
    public static final List<XmppConnection.Identity> BAD_MUC_REFLECTION = Collections.singletonList(
            XmppConnection.Identity.SLACK
    );
}
