package com.sasiddiqui.itunessearch.activity;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.sasiddiqui.itunessearch.R;
import com.sasiddiqui.itunessearch.network.api.SearchService;
import com.sasiddiqui.itunessearch.network.model.SearchResultModel;
import com.sasiddiqui.itunessearch.network.utils.RetrofitBuilder;

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
import retrofit2.Retrofit;
import timber.log.Timber;

/**
 * Created by shahrukhamd on 04/06/18.
 */
public class MainActivity extends AppCompatActivity implements
        Callback<SearchResultModel> {

    private static final int SEARCH_TIMEOUT_MILLI = 555;

    @BindView(R.id.main_search_edit_text)
    EditText searchEditText;

    private PublishSubject<String> queryObservable;
    private Retrofit retrofit;
    private SearchService searchService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        init();
    }

    private void init() {
        retrofit = RetrofitBuilder.getRetrofit();
        searchService = retrofit.create(SearchService.class);

        queryObservable = PublishSubject.create();
        queryObservable.debounce(SEARCH_TIMEOUT_MILLI, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override public void onSubscribe(Disposable d) { }

                    @Override
                    public void onNext(String s) {
                        searchService.getSearchResults(s).enqueue(MainActivity.this);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                    }

                    @Override
                    public void onComplete() {
                        Timber.d("Query observer completed");
                    }
                });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                queryObservable.onNext(s.toString());
            }
        });
    }

    @Override
    public void onResponse(Call<SearchResultModel> call, Response<SearchResultModel> response) {
        Timber.d(response.message());
    }

    @Override
    public void onFailure(Call<SearchResultModel> call, Throwable t) {

    }
}