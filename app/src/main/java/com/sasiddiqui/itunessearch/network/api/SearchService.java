package com.sasiddiqui.itunessearch.network.api;

import com.sasiddiqui.itunessearch.network.model.SearchResultModel;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/**
 * Created by shahrukhamd on 04/06/18.
 */
public interface SearchService {

    @GET("search")
    Call<SearchResultModel> getSearchResults(@Query("term") CharSequence searchTerm);
}
