package me.skriva.ceph.services;

import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.iid.FirebaseInstanceId;

import me.skriva.ceph.Config;
import me.skriva.ceph.R;
import me.skriva.ceph.entities.Account;
import me.skriva.ceph.utils.PhoneHelper;
import me.skriva.ceph.xml.Element;
import me.skriva.ceph.xml.Namespace;
import me.skriva.ceph.xmpp.XmppConnection;
import me.skriva.ceph.xmpp.forms.Data;
import me.skriva.ceph.xmpp.stanzas.IqPacket;
import rocks.xmpp.addr.Jid;

public class PushManagementService {

	protected final XmppConnectionService mXmppConnectionService;

	PushManagementService(XmppConnectionService service) {
		this.mXmppConnectionService = service;
	}

	void registerPushTokenOnServer(final Account account) {
		Log.d(Config.LOGTAG, account.getJid().asBareJid() + ": has push support");
		retrieveFcmInstanceToken(token -> {
			final String androidId = PhoneHelper.getAndroidId(mXmppConnectionService);
			final Jid appServer = Jid.of(mXmppConnectionService.getString(R.string.app_server));
			IqPacket packet = mXmppConnectionService.getIqGenerator().pushTokenToAppServer(appServer, token, androidId);
			mXmppConnectionService.sendIqPacket(account, packet, (a, p) -> {
				Element command = p.findChild("command", "http://jabber.org/protocol/commands");
				if (p.getType() == IqPacket.TYPE.RESULT && command != null) {
					Element x = command.findChild("x", Namespace.DATA);
					if (x != null) {
						Data data = Data.parse(x);
						try {
							String node = data.getValue("node");
							String secret = data.getValue("secret");
							Jid jid = Jid.of(data.getValue("jid"));
							if (node != null && secret != null) {
								enablePushOnServer(a, jid, node, secret);
							}
						} catch (IllegalArgumentException e) {
							e.printStackTrace();
						}
					}
				} else {
					Log.d(Config.LOGTAG, a.getJid().asBareJid() + ": invalid response from app server");
				}
			});
		});
	}

	private void enablePushOnServer(final Account account, final Jid jid, final String node, final String secret) {
		IqPacket enable = mXmppConnectionService.getIqGenerator().enablePush(jid, node, secret);
		mXmppConnectionService.sendIqPacket(account, enable, (a, p) -> {
			if (p.getType() == IqPacket.TYPE.RESULT) {
				Log.d(Config.LOGTAG, a.getJid().asBareJid() + ": successfully enabled push on server");
			} else if (p.getType() == IqPacket.TYPE.ERROR) {
				Log.d(Config.LOGTAG, a.getJid().asBareJid() + ": enabling push on server failed");
			}
		});
	}

	private void retrieveFcmInstanceToken(final OnGcmInstanceTokenRetrieved instanceTokenRetrieved) {
		new Thread(() -> {
			try {
				instanceTokenRetrieved.onGcmInstanceTokenRetrieved(FirebaseInstanceId.getInstance().getToken());
			} catch (Exception e) {
				Log.d(Config.LOGTAG, "unable to get push token",e);
			}
		}).start();

	}


	public boolean available(Account account) {
		final XmppConnection connection = account.getXmppConnection();
		return connection != null
				&& connection.getFeatures().sm()
				&& connection.getFeatures().push()
				&& playServicesAvailable();
	}

	private boolean playServicesAvailable() {
		return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mXmppConnectionService) == ConnectionResult.SUCCESS;
	}

	public boolean isStub() {
		return false;
	}

	interface OnGcmInstanceTokenRetrieved {
		void onGcmInstanceTokenRetrieved(String token);
	}
}
