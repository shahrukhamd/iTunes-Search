package com.sasiddiqui.itunessearch.activity;

import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.sasiddiqui.itunessearch.R;
import com.sasiddiqui.itunessearch.adapter.SearchResultRVAdapter;
import com.sasiddiqui.itunessearch.network.api.SearchService;
import com.sasiddiqui.itunessearch.network.model.ResultModel;
import com.sasiddiqui.itunessearch.network.model.SearchResultModel;
import com.sasiddiqui.itunessearch.network.utils.RetrofitBuilder;

import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

/**
 * Created by shahrukhamd on 04/06/18.
 */
public class MainActivity extends AppCompatActivity implements
        Callback<SearchResultModel> {

    private static final int SEARCH_TIMEOUT_MILLI = 555;
    private static final int SPAN_COUNT_PORT = 3;
    private static final int SPAN_COUNT_LAND = 4;

    @BindView(R.id.main_help_text)
    TextView helpText;
    @BindView(R.id.main_search_edit_text)
    EditText searchEditText;
    @BindView(R.id.main_result_recycler_view)
    RecyclerView resultRecyclerView;

    private SearchResultRVAdapter searchResultRVAdapter;
    private PublishSubject<CharSequence> queryObservable;
    private SearchService searchService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        init();
    }

    private void init() {
        searchService = RetrofitBuilder.getRetrofit().create(SearchService.class);

        queryObservable = PublishSubject.create();
        queryObservable.debounce(SEARCH_TIMEOUT_MILLI, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<CharSequence>() {
                    @Override public void onSubscribe(Disposable d) { }
                    @Override public void onComplete() { }

                    @Override
                    public void onNext(CharSequence s) {
                        if (s.length() > 0) {
                            searchService.getSearchResults(s).enqueue(MainActivity.this);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                    }
                });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                queryObservable.onNext(s);
            }
        });

        StaggeredGridLayoutManager layoutManager;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            layoutManager = new StaggeredGridLayoutManager(SPAN_COUNT_PORT, StaggeredGridLayoutManager.VERTICAL);
        } else {
            layoutManager = new StaggeredGridLayoutManager( SPAN_COUNT_LAND, StaggeredGridLayoutManager.VERTICAL);
        }
        searchResultRVAdapter = new SearchResultRVAdapter();
        resultRecyclerView.setLayoutManager(layoutManager);
        resultRecyclerView.setAdapter(searchResultRVAdapter);
    }

    @Override
    public void onResponse(Call<SearchResultModel> call, Response<SearchResultModel> response) {
        if (response.isSuccessful() && response.body() != null) {
            List<ResultModel> resultModelList = response.body().getResultModels();
            searchResultRVAdapter.updateResults(resultModelList);

            if (resultModelList.size() > 0) {
                resultRecyclerView.setVisibility(View.VISIBLE);
                helpText.setVisibility(View.GONE);
            } else {
                resultRecyclerView.setVisibility(View.GONE);
                helpText.setVisibility(View.VISIBLE);
            }
        } else {
            showError(R.string.message_some_error_occurred);
        }
    }

    @Override
    public void onFailure(Call<SearchResultModel> call, Throwable t) {
        showError(R.string.message_network_error);
    }

    public void showError(int error) {
        resultRecyclerView.setVisibility(View.GONE);
        helpText.setVisibility(View.VISIBLE);
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }
}