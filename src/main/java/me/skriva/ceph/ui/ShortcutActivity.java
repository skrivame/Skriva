package me.skriva.ceph.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.view.inputmethod.InputMethodManager;

import java.util.Collections;
import java.util.List;

import me.skriva.ceph.R;
import me.skriva.ceph.entities.Account;
import me.skriva.ceph.entities.Contact;
import me.skriva.ceph.entities.ListItem;

public class ShortcutActivity extends AbstractSearchableListItemActivity {

    private static final List<String> BLACKLISTED_ACTIVITIES = Collections.singletonList("com.teslacoilsw.launcher.ChooseActionIntentActivity");

    @Override
    protected void refreshUiReal() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getListView().setOnItemClickListener((parent, view, position, id) -> {

            final ComponentName callingActivity = getCallingActivity();

            final InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(getSearchEditText().getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);

            ListItem listItem = getListItems().get(position);
            final boolean legacy = BLACKLISTED_ACTIVITIES.contains(callingActivity == null ? null : callingActivity.getClassName());
            Intent shortcut = xmppConnectionService.getShortcutService().createShortcut(((Contact) listItem), legacy);
            setResult(RESULT_OK,shortcut);
            finish();
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        ActionBar bar = getSupportActionBar();
        if(bar != null){
            bar.setTitle(R.string.create_shortcut);
        }
    }

    @Override
    protected void filterContacts(String needle) {
        getListItems().clear();
        if (xmppConnectionService == null) {
            getListItemAdapter().notifyDataSetChanged();
            return;
        }
        for (final Account account : xmppConnectionService.getAccounts()) {
            if (account.getStatus() != Account.State.DISABLED) {
                for (final Contact contact : account.getRoster().getContacts()) {
                    if (contact.showInContactList()
                            && contact.match(this, needle)) {
                        getListItems().add(contact);
                    }
                }
            }
        }
        Collections.sort(getListItems());
        getListItemAdapter().notifyDataSetChanged();
    }
}
