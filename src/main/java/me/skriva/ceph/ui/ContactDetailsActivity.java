package me.skriva.ceph.ui;

import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import androidx.databinding.DataBindingUtil;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract.Contacts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import me.skriva.ceph.Config;
import me.skriva.ceph.R;
import me.skriva.ceph.crypto.axolotl.AxolotlService;
import me.skriva.ceph.crypto.axolotl.FingerprintStatus;
import me.skriva.ceph.crypto.axolotl.XmppAxolotlSession;
import me.skriva.ceph.databinding.ActivityContactDetailsBinding;
import me.skriva.ceph.entities.Account;
import me.skriva.ceph.entities.Contact;
import me.skriva.ceph.entities.ListItem;
import me.skriva.ceph.services.XmppConnectionService.OnAccountUpdate;
import me.skriva.ceph.services.XmppConnectionService.OnRosterUpdate;
import me.skriva.ceph.ui.adapter.MediaAdapter;
import me.skriva.ceph.ui.interfaces.OnMediaLoaded;
import me.skriva.ceph.ui.util.Attachment;
import me.skriva.ceph.ui.util.AvatarWorkerTask;
import me.skriva.ceph.ui.util.GridManager;
import me.skriva.ceph.ui.util.JidDialog;
import me.skriva.ceph.ui.util.MenuDoubleTabUtil;
import me.skriva.ceph.utils.AccountUtils;
import me.skriva.ceph.utils.Compatibility;
import me.skriva.ceph.utils.IrregularUnicodeDetector;
import me.skriva.ceph.utils.UIHelper;
import me.skriva.ceph.utils.XmppUri;
import me.skriva.ceph.xml.Namespace;
import me.skriva.ceph.xmpp.OnKeyStatusUpdated;
import me.skriva.ceph.xmpp.OnUpdateBlocklist;
import me.skriva.ceph.xmpp.XmppConnection;
import rocks.xmpp.addr.Jid;

public class ContactDetailsActivity extends OmemoActivity implements OnAccountUpdate, OnRosterUpdate, OnUpdateBlocklist, OnKeyStatusUpdated, OnMediaLoaded {
    public static final String ACTION_VIEW_CONTACT = "view_contact";
    private ActivityContactDetailsBinding binding;

    private MediaAdapter mMediaAdapter;

    private Contact contact;
    private final DialogInterface.OnClickListener removeFromRoster = new DialogInterface.OnClickListener() {

        @Override
        public void onClick(DialogInterface dialog, int which) {
            xmppConnectionService.deleteContactOnServer(contact);
        }
    };
    private final OnCheckedChangeListener mOnSendCheckedChange = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                if (contact.getOption(Contact.Options.PENDING_SUBSCRIPTION_REQUEST)) {
                    xmppConnectionService.stopPresenceUpdatesTo(contact);
                } else {
                    contact.setOption(Contact.Options.PREEMPTIVE_GRANT);
                }
            } else {
                contact.resetOption(Contact.Options.PREEMPTIVE_GRANT);
                xmppConnectionService.sendPresencePacket(contact.getAccount(), xmppConnectionService.getPresenceGenerator().stopPresenceUpdatesTo(contact));
            }
        }
    };
    private final OnCheckedChangeListener mOnReceiveCheckedChange = new OnCheckedChangeListener() {

        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                xmppConnectionService.sendPresencePacket(contact.getAccount(), xmppConnectionService.getPresenceGenerator().requestPresenceUpdatesFrom(contact));
            } else {
                xmppConnectionService.sendPresencePacket(contact.getAccount(), xmppConnectionService.getPresenceGenerator().stopPresenceUpdatesFrom(contact));
            }
        }
    };
    private Jid accountJid;
    private Jid contactJid;
    private boolean showDynamicTags = false;
    private boolean showLastSeen = false;
    private boolean showInactiveOmemo = false;
    private String messageFingerprint;


    private OnClickListener onBadgeClick = new OnClickListener() {

        @Override
        public void onClick(View v) {
            Uri systemAccount = contact.getSystemAccount();
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(systemAccount);
                try {
                    startActivity(intent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(ContactDetailsActivity.this, R.string.no_application_found_to_view_contact, Toast.LENGTH_SHORT).show();
                }
            }
    };

    @Override
    public void onRosterUpdate() {
        refreshUi();
    }

    @Override
    public void onAccountUpdate() {
        refreshUi();
    }

    @Override
    public void OnUpdateBlocklist(final Status status) {
        refreshUi();
    }

    @Override
    protected void refreshUiReal() {
        invalidateOptionsMenu();
        populateView();
    }

    @Override
    protected String getShareableUri(boolean http) {
        if (http) {
            return "https://skriva.me/i/" + XmppUri.lameUrlEncode(contact.getJid().asBareJid().toEscapedString());
        } else {
            return "xmpp:" + contact.getJid().asBareJid().toEscapedString();
        }
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        showInactiveOmemo = savedInstanceState != null && savedInstanceState.getBoolean("show_inactive_omemo", false);
        if (getIntent().getAction().equals(ACTION_VIEW_CONTACT)) {
            try {
                this.accountJid = Jid.of(getIntent().getExtras().getString(EXTRA_ACCOUNT));
            } catch (final IllegalArgumentException ignored) {
            }
            try {
                this.contactJid = Jid.of(getIntent().getExtras().getString("contact"));
            } catch (final IllegalArgumentException ignored) {
            }
        }
        this.messageFingerprint = getIntent().getStringExtra("fingerprint");
        this.binding = DataBindingUtil.setContentView(this, R.layout.activity_contact_details);

        setSupportActionBar((Toolbar) binding.toolbar);
        configureActionBar(getSupportActionBar());
        binding.showInactiveDevices.setOnClickListener(v -> {
            showInactiveOmemo = !showInactiveOmemo;
            populateView();
        });
        binding.addContactButton.setOnClickListener(v -> showAddToRosterDialog(contact));

        mMediaAdapter = new MediaAdapter(this,R.dimen.media_size);
        this.binding.media.setAdapter(mMediaAdapter);
        GridManager.setupLayoutManager(this, this.binding.media, R.dimen.media_size);
    }

    @Override
    public void onSaveInstanceState(final Bundle savedInstanceState) {
        savedInstanceState.putBoolean("show_inactive_omemo", showInactiveOmemo);
        super.onSaveInstanceState(savedInstanceState);
    }

    @Override
    public void onStart() {
        super.onStart();
        final int theme = findTheme();
        if (this.mTheme != theme) {
            recreate();
        } else {
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            this.showDynamicTags = preferences.getBoolean(SettingsActivity.SHOW_DYNAMIC_TAGS, false);
            this.showLastSeen = preferences.getBoolean("last_activity", false);
        }
        binding.mediaWrapper.setVisibility(Compatibility.hasStoragePermission(this) ? View.VISIBLE : View.GONE);
        mMediaAdapter.setAttachments(Collections.emptyList());
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem menuItem) {
        if (MenuDoubleTabUtil.shouldIgnoreTap()) {
            return false;
        }
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setNegativeButton(getString(R.string.cancel), null);
        switch (menuItem.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.action_share_http:
                shareLink(true);
                break;
            case R.id.action_share_uri:
                shareLink(false);
                break;
            case R.id.action_delete_contact:
                builder.setTitle(getString(R.string.action_delete_contact))
                        .setMessage(JidDialog.style(this, R.string.remove_contact_text, contact.getJid().toEscapedString()))
                        .setPositiveButton(getString(R.string.delete),
                                removeFromRoster).create().show();
                break;
            case R.id.action_edit_contact:
                Uri systemAccount = contact.getSystemAccount();
                if (systemAccount == null) {
                    quickEdit(contact.getServerName(), R.string.contact_name, value -> {
                        contact.setServerName(value);
                        ContactDetailsActivity.this.xmppConnectionService.pushContactToServer(contact);
                        populateView();
                        return null;
                    });
                } else {
                    Intent intent = new Intent(Intent.ACTION_EDIT);
                    intent.setDataAndType(systemAccount, Contacts.CONTENT_ITEM_TYPE);
                    intent.putExtra("finishActivityOnSaveCompleted", true);
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(ContactDetailsActivity.this, R.string.no_application_found_to_view_contact, Toast.LENGTH_SHORT).show();
                    }

                }
                break;
            case R.id.action_block:
                BlockContactDialog.show(this, contact);
                break;
            case R.id.action_unblock:
                BlockContactDialog.show(this, contact);
                break;
        }
        return super.onOptionsItemSelected(menuItem);
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.contact_details, menu);
        AccountUtils.showHideMenuItems(menu);
        MenuItem block = menu.findItem(R.id.action_block);
        MenuItem unblock = menu.findItem(R.id.action_unblock);
        MenuItem edit = menu.findItem(R.id.action_edit_contact);
        MenuItem delete = menu.findItem(R.id.action_delete_contact);
        if (contact == null) {
            return true;
        }
        final XmppConnection connection = contact.getAccount().getXmppConnection();
        if (connection != null && connection.getFeatures().blocking()) {
            if (this.contact.isBlocked()) {
                block.setVisible(false);
            } else {
                unblock.setVisible(false);
            }
        } else {
            unblock.setVisible(false);
            block.setVisible(false);
        }
        if (!contact.showInRoster()) {
            edit.setVisible(false);
            delete.setVisible(false);
        }
        return super.onCreateOptionsMenu(menu);
    }

    private void populateView() {
        if (contact == null) {
            return;
        }
        invalidateOptionsMenu();
        setTitle(contact.getDisplayName());
        if (contact.showInRoster()) {
            binding.detailsSendPresence.setVisibility(View.VISIBLE);
            binding.detailsReceivePresence.setVisibility(View.VISIBLE);
            binding.addContactButton.setVisibility(View.GONE);
            binding.detailsSendPresence.setOnCheckedChangeListener(null);
            binding.detailsReceivePresence.setOnCheckedChangeListener(null);

            List<String> statusMessages = contact.getPresences().getStatusMessages();
            if (statusMessages.size() == 0) {
                binding.statusMessage.setVisibility(View.GONE);
            } else {
                StringBuilder builder = new StringBuilder();
                binding.statusMessage.setVisibility(View.VISIBLE);
                int s = statusMessages.size();
                for (int i = 0; i < s; ++i) {
                    if (s > 1) {
                        builder.append("• ");
                    }
                    builder.append(statusMessages.get(i));
                    if (i < s - 1) {
                        builder.append("\n");
                    }
                }
                binding.statusMessage.setText(builder);
            }

            if (contact.getOption(Contact.Options.FROM)) {
                binding.detailsSendPresence.setText(R.string.send_presence_updates);
                binding.detailsSendPresence.setChecked(true);
            } else if (contact.getOption(Contact.Options.PENDING_SUBSCRIPTION_REQUEST)) {
                binding.detailsSendPresence.setChecked(false);
                binding.detailsSendPresence.setText(R.string.send_presence_updates);
            } else {
                binding.detailsSendPresence.setText(R.string.preemptively_grant);
                if (contact.getOption(Contact.Options.PREEMPTIVE_GRANT)) {
                    binding.detailsSendPresence.setChecked(true);
                } else {
                    binding.detailsSendPresence.setChecked(false);
                }
            }
            if (contact.getOption(Contact.Options.TO)) {
                binding.detailsReceivePresence.setText(R.string.receive_presence_updates);
                binding.detailsReceivePresence.setChecked(true);
            } else {
                binding.detailsReceivePresence.setText(R.string.ask_for_presence_updates);
                if (contact.getOption(Contact.Options.ASKING)) {
                    binding.detailsReceivePresence.setChecked(true);
                } else {
                    binding.detailsReceivePresence.setChecked(false);
                }
            }
            if (contact.getAccount().isOnlineAndConnected()) {
                binding.detailsReceivePresence.setEnabled(true);
                binding.detailsSendPresence.setEnabled(true);
            } else {
                binding.detailsReceivePresence.setEnabled(false);
                binding.detailsSendPresence.setEnabled(false);
            }
            binding.detailsSendPresence.setOnCheckedChangeListener(this.mOnSendCheckedChange);
            binding.detailsReceivePresence.setOnCheckedChangeListener(this.mOnReceiveCheckedChange);
        } else {
            binding.addContactButton.setVisibility(View.VISIBLE);
            binding.detailsSendPresence.setVisibility(View.GONE);
            binding.detailsReceivePresence.setVisibility(View.GONE);
            binding.statusMessage.setVisibility(View.GONE);
        }

        if (contact.isBlocked() && !this.showDynamicTags) {
            binding.detailsLastseen.setVisibility(View.VISIBLE);
            binding.detailsLastseen.setText(R.string.contact_blocked);
        } else {
            if (showLastSeen
                    && contact.getLastseen() > 0
                    && contact.getPresences().allOrNonSupport(Namespace.IDLE)) {
                binding.detailsLastseen.setVisibility(View.VISIBLE);
                binding.detailsLastseen.setText(UIHelper.lastseen(getApplicationContext(), contact.isActive(), contact.getLastseen()));
            } else {
                binding.detailsLastseen.setVisibility(View.GONE);
            }
        }

        binding.detailsContactjid.setText(IrregularUnicodeDetector.style(this, contact.getJid()));
        String account;
        if (Config.DOMAIN_LOCK != null) {
            account = contact.getAccount().getJid().getLocal();
        } else {
            account = contact.getAccount().getJid().asBareJid().toString();
        }
        binding.detailsAccount.setText(getString(R.string.using_account, account));
        AvatarWorkerTask.loadAvatar(contact,binding.detailsContactBadge,R.dimen.avatar_big);
        binding.detailsContactKeys.removeAllViews();
        boolean hasKeys = false;
        final LayoutInflater inflater = getLayoutInflater();
        final AxolotlService axolotlService = contact.getAccount().getAxolotlService();
        if (Config.supportOmemo() && axolotlService != null) {
            final Collection<XmppAxolotlSession> sessions = axolotlService.findSessionsForContact(contact);
            boolean anyActive = false;
            for(XmppAxolotlSession session : sessions) {
                anyActive = session.getTrust().isActive();
                if (anyActive) {
                    break;
                }
            }
            boolean skippedInactive = false;
            boolean showsInactive = false;
            for (final XmppAxolotlSession session :sessions) {
                final FingerprintStatus trust = session.getTrust();
                hasKeys |= !trust.isCompromised();
                if (!trust.isActive() && anyActive) {
                    if (showInactiveOmemo) {
                        showsInactive = true;
                    } else {
                        skippedInactive = true;
                        continue;
                    }
                }
                if (!trust.isCompromised()) {
                    boolean highlight = session.getFingerprint().equals(messageFingerprint);
                    addFingerprintRow(binding.detailsContactKeys, session, highlight);
                }
            }
            if (showsInactive || skippedInactive) {
                binding.showInactiveDevices.setText(showsInactive ? R.string.hide_inactive_devices : R.string.show_inactive_devices);
                binding.showInactiveDevices.setVisibility(View.VISIBLE);
            } else {
                binding.showInactiveDevices.setVisibility(View.GONE);
            }
        } else {
            binding.showInactiveDevices.setVisibility(View.GONE);
        }
        binding.scanButton.setVisibility(hasKeys && isCameraFeatureAvailable() ? View.VISIBLE : View.GONE);
        if (hasKeys) {
            binding.scanButton.setOnClickListener((v) -> ScanActivity.scan(this));
        }
        binding.keysWrapper.setVisibility(hasKeys ? View.VISIBLE : View.GONE);

        List<ListItem.Tag> tagList = contact.getTags(this);
        if (tagList.size() == 0 || !this.showDynamicTags) {
            binding.tags.setVisibility(View.GONE);
        } else {
            binding.tags.setVisibility(View.VISIBLE);
            binding.tags.removeAllViewsInLayout();
            for (final ListItem.Tag tag : tagList) {
                final TextView tv = (TextView) inflater.inflate(R.layout.list_item_tag, binding.tags, false);
                tv.setText(tag.getName());
                tv.setBackgroundColor(tag.getColor());
                binding.tags.addView(tv);
            }
        }
    }

    public void onBackendConnected() {
        if (accountJid != null && contactJid != null) {
            Account account = xmppConnectionService.findAccountByJid(accountJid);
            if (account == null) {
                return;
            }
            this.contact = account.getRoster().getContact(contactJid);
            if (mPendingFingerprintVerificationUri != null) {
                processFingerprintVerification(mPendingFingerprintVerificationUri);
                mPendingFingerprintVerificationUri = null;
            }

            if (Compatibility.hasStoragePermission(this)) {
                final int limit = GridManager.getCurrentColumnCount(this.binding.media);
                xmppConnectionService.getAttachments(account, contact.getJid().asBareJid(), limit, this);
                this.binding.showMedia.setOnClickListener((v) -> MediaBrowserActivity.launch(this, contact));
            }
            populateView();
        }
    }

    @Override
    public void onKeyStatusUpdated(AxolotlService.FetchStatus report) {
        refreshUi();
    }

    @Override
    protected void processFingerprintVerification(XmppUri uri) {
        if (contact != null && contact.getJid().asBareJid().equals(uri.getJid()) && uri.hasFingerprints()) {
            if (xmppConnectionService.verifyFingerprints(contact, uri.getFingerprints())) {
                Toast.makeText(this, R.string.verified_fingerprints, Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, R.string.invalid_barcode, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMediaLoaded(List<Attachment> attachments) {
        runOnUiThread(() -> {
            int limit = GridManager.getCurrentColumnCount(binding.media);
            mMediaAdapter.setAttachments(attachments.subList(0, Math.min(limit,attachments.size())));
            binding.mediaWrapper.setVisibility(attachments.size() > 0 ? View.VISIBLE : View.GONE);
        });

    }
}
