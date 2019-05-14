package me.skriva.ceph.ui;

import android.app.Dialog;
import android.content.Context;
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.View;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import me.skriva.ceph.R;
import me.skriva.ceph.databinding.CreateConferenceDialogBinding;
import me.skriva.ceph.ui.util.DelayedHintHelper;

public class CreatePrivateGroupChatDialog extends DialogFragment {

    private static final String ACCOUNTS_LIST_KEY = "activated_accounts_list";
    private CreateConferenceDialogListener mListener;

    public static CreatePrivateGroupChatDialog newInstance(List<String> accounts) {
        CreatePrivateGroupChatDialog dialog = new CreatePrivateGroupChatDialog();
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(ACCOUNTS_LIST_KEY, (ArrayList<String>) accounts);
        dialog.setArguments(bundle);
        return dialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.create_private_group_chat);
        CreateConferenceDialogBinding binding = DataBindingUtil.inflate(getActivity().getLayoutInflater(), R.layout.create_conference_dialog, null, false);
        ArrayList<String> mActivatedAccounts = getArguments().getStringArrayList(ACCOUNTS_LIST_KEY);
        if (mActivatedAccounts.size() == 1) {
            binding.account.setVisibility(View.GONE);
            binding.yourAccount.setVisibility(View.GONE);
        } else {
            binding.account.setVisibility(View.VISIBLE);
            binding.yourAccount.setVisibility(View.VISIBLE);
        }
        StartConversationActivity.populateAccountSpinner(getActivity(), mActivatedAccounts, binding.account);
        builder.setView(binding.getRoot());
        builder.setPositiveButton(R.string.choose_participants, (dialog, which) -> mListener.onCreateDialogPositiveClick(binding.account, binding.groupChatName.getText().toString().trim()));
        builder.setNegativeButton(R.string.cancel, null);
        DelayedHintHelper.setHint(R.string.providing_a_name_is_optional, binding.groupChatName);
        binding.groupChatName.setOnEditorActionListener((v, actionId, event) -> {
            mListener.onCreateDialogPositiveClick(binding.account, binding.groupChatName.getText().toString().trim());
            return true;
        });
        return builder.create();
    }


    public interface CreateConferenceDialogListener {
        void onCreateDialogPositiveClick(Spinner spinner, String subject);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (CreateConferenceDialogListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement CreateConferenceDialogListener");
        }
    }

    @Override
    public void onDestroyView() {
        Dialog dialog = getDialog();
        if (dialog != null && getRetainInstance()) {
            dialog.setDismissMessage(null);
        }
        super.onDestroyView();
    }
}
