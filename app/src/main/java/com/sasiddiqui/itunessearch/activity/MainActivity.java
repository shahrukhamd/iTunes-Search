package com.sasiddiqui.itunessearch.activity;

import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.sasiddiqui.itunessearch.BuildConfig;
import com.sasiddiqui.itunessearch.R;
import com.sasiddiqui.itunessearch.adapter.SearchResultRVAdapter;
import com.sasiddiqui.itunessearch.network.ResultClickListener;
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
        Callback<SearchResultModel>,
        ResultClickListener {

    private static final int SEARCH_TIMEOUT_MILLI = 555;
    private static final int SPAN_COUNT_PORT = 3;
    private static final int SPAN_COUNT_LAND = 4;

    @BindView(R.id.main_help_text)
    TextView helpText;
    @BindView(R.id.main_search_edit_text)
    EditText searchEditText;
    @BindView(R.id.main_result_recycler_view)
    RecyclerView resultRecyclerView;
    @BindView(R.id.main_progress_bar)
    ProgressBar progressBar;

    private SimpleExoPlayer player;
    private DefaultHttpDataSourceFactory dataSourceFactory;
    private long playbackPosition;
    private String currentMediaUrl;

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

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) initExoPlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23 || player == null)) initExoPlayer();
    }

    /*Before API Level 24 there is no guarantee of onStop being called. So we have to release the
      player as early as possible in onPause. Starting with API Level 24 onStop is guaranteed to be
      called and in the paused mode our activity is eventually still visible. Hence we need to wait
      releasing until onStop.*/

    @Override
    protected void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) releasePlayer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) releasePlayer();
    }

    /**
     * Initializing the views and accompanying components.
     */
    private void init() {
        searchService = RetrofitBuilder.getRetrofit().create(SearchService.class);

        // User query observer for RxJava
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
                            progressBar.setVisibility(View.VISIBLE);
                            player.stop();
                            searchService.getSearchResults(s, SearchService.ENTITY_TYPE_MUSIC_TRACK)
                                    .enqueue(MainActivity.this);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                        player.stop();
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

        // Search result recycler view
        StaggeredGridLayoutManager layoutManager;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            layoutManager = new StaggeredGridLayoutManager(SPAN_COUNT_PORT, StaggeredGridLayoutManager.VERTICAL);
        } else {
            layoutManager = new StaggeredGridLayoutManager( SPAN_COUNT_LAND, StaggeredGridLayoutManager.VERTICAL);
        }
        searchResultRVAdapter = new SearchResultRVAdapter(this);
        resultRecyclerView.setLayoutManager(layoutManager);
        resultRecyclerView.setAdapter(searchResultRVAdapter);
    }

    private void initExoPlayer() {
        dataSourceFactory = new DefaultHttpDataSourceFactory("exoplayer-" + BuildConfig.APPLICATION_ID);
        player = ExoPlayerFactory.newSimpleInstance(
                new DefaultRenderersFactory(this),
                new DefaultTrackSelector(), new DefaultLoadControl());
        player.setPlayWhenReady(true);

        if (currentMediaUrl != null) {
            player.prepare(getMediaSource());
            player.seekTo(playbackPosition);
        }
    }

    @Override
    public void onResponse(Call<SearchResultModel> call, Response<SearchResultModel> response) {
        progressBar.setVisibility(View.GONE);

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
        progressBar.setVisibility(View.GONE);
        showError(R.string.message_network_error);
    }

    /**
     * Show appropriate error message to user.
     */
    public void showError(int error) {
        resultRecyclerView.setVisibility(View.GONE);
        helpText.setVisibility(View.VISIBLE);
        Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onResultItemClick(ResultModel resultModel) {
        player.stop();  // Stop whatever is playing

        if (!resultModel.isPlaying()) {
            Toast.makeText(this, String.format(getString(R.string.message_playing_track), resultModel.getTrackName()), Toast.LENGTH_SHORT).show();
            currentMediaUrl = resultModel.getPreviewUrl();
            player.prepare(getMediaSource(), true, false);
        }

        resultModel.switchIsPlaying();
    }

    private MediaSource getMediaSource() {
        return new ExtractorMediaSource.Factory(dataSourceFactory)
                .createMediaSource(Uri.parse(currentMediaUrl));
    }

    /**
     * Release the resources used by Exo player.
     */
    private void releasePlayer() {
        if (player != null) {
            playbackPosition = player.getCurrentPosition(); // Storing the playback position for resume
            player.release();
            player = null;
        }
    }
}