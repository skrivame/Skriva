package me.skriva.ceph.ui.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Locale;

import me.skriva.ceph.R;
import me.skriva.ceph.databinding.SearchResultItemBinding;
import me.skriva.ceph.http.services.MuclumbusService;
import me.skriva.ceph.ui.util.AvatarWorkerTask;

public class ChannelSearchResultAdapter extends ListAdapter<MuclumbusService.Room, ChannelSearchResultAdapter.ViewHolder> {

    private OnChannelSearchResultSelected listener;

    private static final DiffUtil.ItemCallback<MuclumbusService.Room> DIFF = new DiffUtil.ItemCallback<MuclumbusService.Room>() {
        @Override
        public boolean areItemsTheSame(@NonNull MuclumbusService.Room a, @NonNull MuclumbusService.Room b) {
            return a.address != null && a.address.equals(b.address);
        }

        @Override
        public boolean areContentsTheSame(@NonNull MuclumbusService.Room a, @NonNull MuclumbusService.Room b) {
            return a.equals(b);
        }
    };

    public ChannelSearchResultAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new ViewHolder(DataBindingUtil.inflate(LayoutInflater.from(viewGroup.getContext()), R.layout.search_result_item, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        final MuclumbusService.Room searchResult = getItem(position);
        viewHolder.binding.name.setText(searchResult.getName());
        final String description = searchResult.getDescription();
        final String language = searchResult.getLanguage();
        if (TextUtils.isEmpty(description)) {
            viewHolder.binding.description.setVisibility(View.GONE);
        } else {
            viewHolder.binding.description.setText(description);
            viewHolder.binding.description.setVisibility(View.VISIBLE);
        }
        if (language == null || language.length() != 2) {
            viewHolder.binding.language.setVisibility(View.GONE);
        } else {
            viewHolder.binding.language.setText(language.toUpperCase(Locale.ENGLISH));
            viewHolder.binding.language.setVisibility(View.VISIBLE);
        }
        viewHolder.binding.room.setText(searchResult.getRoom().asBareJid().toString());
        AvatarWorkerTask.loadAvatar(searchResult, viewHolder.binding.avatar, R.dimen.avatar);
        viewHolder.binding.getRoot().setOnClickListener(v -> listener.onChannelSearchResult(searchResult));
    }

    public void setOnChannelSearchResultSelectedListener(OnChannelSearchResultSelected listener) {
        this.listener = listener;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final SearchResultItemBinding binding;

        private ViewHolder(SearchResultItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    public interface OnChannelSearchResultSelected {
        void onChannelSearchResult(MuclumbusService.Room result);
    }
}
