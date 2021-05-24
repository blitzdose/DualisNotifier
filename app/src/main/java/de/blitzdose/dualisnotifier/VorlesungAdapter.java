package de.blitzdose.dualisnotifier;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class VorlesungAdapter extends RecyclerView.Adapter<VorlesungAdapter.MyViewHolder> {

    private final List<VorlesungModel> dataModelList;
    private final Context mContext;
    private int mExpandedPosition = -1;

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        public TextView titleTextView;
        public TextView subtitleTextView;
        public TextView notenTextview;
        public TextView creditsTextView;
        public TextView endnoteTextView;
        public ImageView materialButton;
        public View view;
        public ConstraintLayout expandLayout;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            view = itemView;
            titleTextView = itemView.findViewById(R.id.card_title);
            subtitleTextView = itemView.findViewById(R.id.card_subtitle);
            notenTextview = itemView.findViewById(R.id.card_grade);
            creditsTextView = itemView.findViewById(R.id.credits);
            endnoteTextView = itemView.findViewById(R.id.endnote);
            materialButton = itemView.findViewById(R.id.expand_image);
            expandLayout = itemView.findViewById(R.id.expanded_layout);
        }

        public void bindData(VorlesungModel vorlesungModel) {
            titleTextView.setText(vorlesungModel.getTitle());
            subtitleTextView.setText(vorlesungModel.getSubtitle());
            notenTextview.setText(vorlesungModel.getNote());
            creditsTextView.setText(vorlesungModel.getCredits());
            endnoteTextView.setText(vorlesungModel.getEndnote());
        }
    }

    public VorlesungAdapter(List<VorlesungModel> modelList, Context context) {
        dataModelList = modelList;
        mContext = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_list_item, parent, false);

        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        holder.bindData(dataModelList.get(position));

        final boolean isExpanded = position == mExpandedPosition;
        holder.expandLayout.setVisibility(isExpanded?View.VISIBLE:View.GONE);
        holder.materialButton.setImageDrawable(isExpanded?AppCompatResources.getDrawable(mContext, R.drawable.ic_baseline_expand_less_24):AppCompatResources.getDrawable(mContext, R.drawable.ic_baseline_expand_more_24));
        holder.itemView.setActivated(isExpanded);
        holder.itemView.setOnClickListener(v -> {
            mExpandedPosition = isExpanded ? -1:position;
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return dataModelList.size();
    }
}