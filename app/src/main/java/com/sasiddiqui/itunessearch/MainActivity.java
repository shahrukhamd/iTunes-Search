package com.sasiddiqui.itunessearch;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;

/**
 * Created by shahrukhamd on 04/06/18.
 */
public class MainActivity extends AppCompatActivity {

    private static final int SEARCH_TIMEOUT_MILLI = 555;

    @BindView(R.id.main_search_edit_text)
    EditText searchEditText;
    private PublishSubject<String> queryObservable;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        init();
    }

    private void init() {
        Observer<String> mQueryObserver = new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onNext(String s) {
                Timber.d(s);
            }

            @Override
            public void onError(Throwable e) {
                Timber.e(e);
            }

            @Override
            public void onComplete() {
                Timber.d("Query observer completed");
            }
        };

        queryObservable = PublishSubject.create();
        queryObservable.debounce(SEARCH_TIMEOUT_MILLI, TimeUnit.MILLISECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(mQueryObserver);

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable s) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                queryObservable.onNext(s.toString());
            }
        });
    }
}