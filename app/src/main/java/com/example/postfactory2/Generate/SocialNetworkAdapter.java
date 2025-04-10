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

    private List<String> socialNetworks;
    private List<Boolean> selectedStates;

    public SocialNetworkAdapter(List<String> socialNetworks) {
        this.socialNetworks = socialNetworks;
        this.selectedStates = new ArrayList<>();
        for (int i = 0; i < socialNetworks.size(); i++) {
            selectedStates.add(false);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_social_network, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String network = socialNetworks.get(position);
        holder.networkName.setText(network);
        holder.checkBox.setChecked(selectedStates.get(position));

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            selectedStates.set(position, isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return socialNetworks.size();
    }

    public String[] getSelectedNetworks() {
        List<String> selected = new ArrayList<>();
        for (int i = 0; i < socialNetworks.size(); i++) {
            if (selectedStates.get(i)) {
                selected.add(socialNetworks.get(i));
            }
        }
        return selected.toArray(new String[0]);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView networkName;
        CheckBox checkBox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            networkName = itemView.findViewById(R.id.tvSocialNetwork);
            checkBox = itemView.findViewById(R.id.cbSelectNetwork);
        }
    }
}
