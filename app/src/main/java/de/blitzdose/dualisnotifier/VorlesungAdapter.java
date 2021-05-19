package de.blitzdose.dualisnotifier;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class VorlesungAdapter extends RecyclerView.Adapter<VorlesungAdapter.MyViewHolder> {

    private List<VorlesungModel> dataModelList;
    private Context mContext;

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTextView;
        public TextView subtitleTextView;
        public TextView notenTextview;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            titleTextView = itemView.findViewById(R.id.card_title);
            subtitleTextView = itemView.findViewById(R.id.card_subtitle);
            notenTextview = itemView.findViewById(R.id.card_grade);
        }

        public void bindData(VorlesungModel vorlesungModel, Context context) {
            titleTextView.setText(vorlesungModel.getTitle());
            subtitleTextView.setText(vorlesungModel.getSubtitle());
            notenTextview.setText(vorlesungModel.getNote());
        }
    }

    public VorlesungAdapter(List<VorlesungModel> modelList, Context context) {
        dataModelList = modelList;
        mContext = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate out card list item

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_list_item, parent, false);
        // Return a new view holder

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        // Bind data for the item at position

        holder.bindData(dataModelList.get(position), mContext);
    }

    @Override
    public int getItemCount() {
        // Return the total number of items

        return dataModelList.size();
    }
}