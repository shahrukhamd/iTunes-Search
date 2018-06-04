package com.sasiddiqui.itunessearch.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.sasiddiqui.itunessearch.R;
import com.sasiddiqui.itunessearch.network.model.ResultModel;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by shahrukhamd on 04/06/18.
 */
public class SearchResultRVAdapter extends RecyclerView.Adapter<SearchResultRVAdapter.ViewHolder> {

    private List<ResultModel> resultModelList;

    class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.music_item_image)
        ImageView musicCoverImage;
        @BindView(R.id.music_item_title_text)
        TextView musicTitleText;
        @BindView(R.id.music_item_author_text)
        TextView musicAuthorText;

        ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        void bindData(ResultModel resultModel) {
            musicTitleText.setText(resultModel.getTrackName());
            musicAuthorText.setText(resultModel.getArtistName());
            Glide.with(musicCoverImage.getContext())
                    .load(resultModel.getArtworkUrl100())
                    .into(musicCoverImage);
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.view_search_result_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bindData(resultModelList.get(position));
    }

    @Override
    public int getItemCount() {
        return resultModelList != null ? resultModelList.size() : 0;
    }

    public void updateResults(List<ResultModel> resultModelList) {
        this.resultModelList = resultModelList;
        notifyDataSetChanged();
    }
}
