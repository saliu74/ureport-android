package in.ureport.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import in.ureport.R;
import in.ureport.loader.PollsLoader;
import in.ureport.models.Poll;
import in.ureport.views.adapters.PollAdapter;

/**
 * Created by johncordeiro on 7/13/15.
 */
public class PollsFragment extends Fragment implements LoaderManager.LoaderCallbacks<List<Poll>> {

    private static final String TAG = "PollsFragment";

    private static final int LOADER_ID_POLLS = 20;

    private RecyclerView pollsList;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_polls, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupView(view);
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().initLoader(LOADER_ID_POLLS, null, this).forceLoad();
    }

    private void setupView(View view) {
        pollsList = (RecyclerView) view.findViewById(R.id.pollsList);
        pollsList.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    @Override
    public Loader<List<Poll>> onCreateLoader(int id, Bundle args) {
        return new PollsLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<Poll>> loader, List<Poll> data) {
        PollAdapter adapter = new PollAdapter(data);
        pollsList.setAdapter(adapter);
    }

    @Override
    public void onLoaderReset(Loader<List<Poll>> loader) {}
}
