package com.sasiddiqui.itunessearch.network.model;

import java.util.List;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by shahrukhamd on 04/06/18.
 */

public class SearchResultModel {

    @SerializedName("resultCount")
    @Expose
    private int resultCount;
    @SerializedName("results")
    @Expose
    private List<ResultModel> resultModels = null;

    public int getResultCount() {
        return resultCount;
    }

    public void setResultCount(int resultCount) {
        this.resultCount = resultCount;
    }

    public List<ResultModel> getResultModels() {
        return resultModels;
    }

    public void setResultModels(List<ResultModel> resultModels) {
        this.resultModels = resultModels;
    }

}
