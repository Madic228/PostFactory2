package com.example.postfactory2.Generate;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.postfactory2.R;

import java.util.ArrayList;
import java.util.List;

public class SocialNetworkAdapter extends RecyclerView.Adapter<SocialNetworkAdapter.ViewHolder> {

    private final List<String> socialNetworks;
    private final List<String> selectedNetworks;

    public SocialNetworkAdapter(List<String> socialNetworks) {
        this.socialNetworks = socialNetworks;
        this.selectedNetworks = new ArrayList<>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_social_network, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String socialNetwork = socialNetworks.get(position);
        holder.tvSocialNetwork.setText(socialNetwork);

        // Установка чекбокса
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedNetworks.add(socialNetwork);
            } else {
                selectedNetworks.remove(socialNetwork);
            }
        });
    }

    @Override
    public int getItemCount() {
        return socialNetworks.size();
    }

    public List<String> getSelectedNetworks() {
        return selectedNetworks;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvSocialNetwork;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvSocialNetwork = itemView.findViewById(R.id.tvSocialNetwork);
            checkBox = itemView.findViewById(R.id.cbSelectNetwork);
        }
    }
}
