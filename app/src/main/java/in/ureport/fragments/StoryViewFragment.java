package in.ureport.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import br.com.ilhasoft.support.tool.EditTextValidator;
import br.com.ilhasoft.support.tool.UnitConverter;
import in.ureport.R;
import in.ureport.UreportApplication;
import in.ureport.helpers.ValueEventListenerAdapter;
import in.ureport.managers.ImageLoader;
import in.ureport.managers.PrototypeManager;
import in.ureport.models.Contribution;
import in.ureport.models.Media;
import in.ureport.models.Story;
import in.ureport.models.User;
import in.ureport.network.ContributionServices;
import in.ureport.network.UserServices;
import in.ureport.helpers.ChildEventListenerAdapter;
import in.ureport.helpers.SpaceItemDecoration;
import in.ureport.helpers.WrapLinearLayoutManager;
import in.ureport.views.adapters.ContributionAdapter;
import in.ureport.views.adapters.MediaAdapter;

/**
 * Created by johncordeiro on 7/16/15.
 */
public class StoryViewFragment extends Fragment {

    private static final String EXTRA_STORY = "story";
    private static final String EXTRA_USER = "user";

    private Story story;
    private User user;

    private ContributionAdapter contributionAdapter;
    private TextView contributions;
    private Button contribute;
    private EditText contribution;
    private View addContributionContainer;

    private ContributionServices contributionServices;
    private UserServices userServices;

    public static StoryViewFragment newInstance(Story story, User user) {
        StoryViewFragment storyViewFragment = new StoryViewFragment();

        Bundle args = new Bundle();
        args.putParcelable(EXTRA_STORY, story);
        args.putParcelable(EXTRA_USER, user);
        storyViewFragment.setArguments(args);

        return storyViewFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments() != null && getArguments().containsKey(EXTRA_STORY)
        && getArguments().containsKey(EXTRA_USER)) {
            story = getArguments().getParcelable(EXTRA_STORY);
            user = getArguments().getParcelable(EXTRA_USER);
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_story_view, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupObjects();
        setupView(view);
        loadData();
    }

    private void loadData() {
        contributionServices.addChildEventListener(story, contributionChildEventListener);
    }

    private void setupObjects() {
        contributionServices = new ContributionServices();
        userServices = new UserServices();
    }

    private void setupView(View view) {
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);

        AppCompatActivity activity = ((AppCompatActivity) getActivity());
        activity.setSupportActionBar(toolbar);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(story.getTitle());

        TextView content = (TextView) view.findViewById(R.id.content);
        content.setText(story.getContent());

        TextView markers = (TextView) view.findViewById(R.id.markers);
        markers.setText(story.getMarkers());

        TextView author = (TextView) view.findViewById(R.id.author);
        author.setText(story.getUser().getNickname());

        ImageView picture = (ImageView) view.findViewById(R.id.picture);
        ImageLoader.loadPersonPictureToImageView(picture, story.getUser().getPicture());

        contributions = (TextView) view.findViewById(R.id.contributors);
        contributions.setText(getContributionsText(story));

        addContributionContainer = view.findViewById(R.id.addContributionContainer);

        contribute = (Button) view.findViewById(R.id.contribute);
        contribute.setOnClickListener(onContributeClickListener);

        Button addContribution = (Button) view.findViewById(R.id.addContribution);
        addContribution.setOnClickListener(onAddContributionClickListener);

        contribution = (EditText) view.findViewById(R.id.contribution);
        contribution.setOnEditorActionListener(onDescriptionEditorActionListener);

        UnitConverter converter = new UnitConverter(getActivity());

        RecyclerView contributionList = (RecyclerView) view.findViewById(R.id.contributionList);
        contributionList.setLayoutManager(new WrapLinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));

        contributionAdapter = new ContributionAdapter(user);
        contributionList.setAdapter(contributionAdapter);

        RecyclerView mediaList = (RecyclerView) view.findViewById(R.id.mediaList);
        mediaList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));

        SpaceItemDecoration mediaItemDecoration = new SpaceItemDecoration();
        mediaItemDecoration.setHorizontalSpaceWidth((int) converter.convertDpToPx(10));
        mediaList.addItemDecoration(mediaItemDecoration);

        MediaAdapter adapter = new MediaAdapter(new ArrayList<Media>(), false);
        mediaList.setAdapter(adapter);

        FloatingActionButton floatingActionButton = (FloatingActionButton) view.findViewById(R.id.share);
        floatingActionButton.setOnClickListener(onShareClickListener);
    }

    private String getContributionsText(Story story) {
        return String.format(getString(R.string.stories_list_item_contributions), story.getContributions());
    }

    private View.OnClickListener onContributeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if(UreportApplication.validateUserLogin(getActivity())) {
                updateViewForContribution();
            }
        }
    };

    private View.OnClickListener onShareClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            PrototypeManager.showPrototypeAlert(getActivity());
        }
    };

    public void addContribution(String content) {
        final Contribution contribution = new Contribution(content, user);
        contribution.setCreatedDate(new Date());

        contributionServices.saveContribution(story, contribution, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if(firebaseError == null) {
                    StoryViewFragment.this.contribution.setText(null);
                    refreshContribution();
                }
            }
        });
    }

    private ChildEventListenerAdapter contributionChildEventListener = new ChildEventListenerAdapter() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String previousChild) {
            super.onChildAdded(dataSnapshot, previousChild);

            final Contribution contribution = dataSnapshot.getValue(Contribution.class);
            contribution.setKey(dataSnapshot.getKey());
            loadUserFromContribution(contribution, onAfterLoadUserListener);
        }
    };

    private OnAfterLoadUserListener onAfterLoadUserListener = new OnAfterLoadUserListener() {
        @Override
        public void onAfterLoadUser(Contribution contribution) {
            updateViewForContribution();
            contributionAdapter.addContribution(contribution);
        }
    };

    private void updateViewForContribution() {
        if(contribute.getVisibility() == View.VISIBLE) {
            addContributionContainer.setVisibility(View.VISIBLE);
            contribute.setVisibility(View.GONE);
        }
    }

    private void loadUserFromContribution(final Contribution contribution, final OnAfterLoadUserListener listener) {
        userServices.getUser(contribution.getAuthor().getKey(), new ValueEventListenerAdapter() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                User user = dataSnapshot.getValue(User.class);
                contribution.setAuthor(user);
                if (listener != null) listener.onAfterLoadUser(contribution);
            }
        });
    }

    private void refreshContribution() {
        contributions.setText(getContributionsText(story));
    }

    private View.OnClickListener onAddContributionClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            onAddNewContribution();
        }
    };

    private TextView.OnEditorActionListener onDescriptionEditorActionListener = new TextView.OnEditorActionListener() {
        @Override
        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
            onAddNewContribution();
            return false;
        }
    };

    private void onAddNewContribution() {
        if(contribution.getText().length() > 0) {
            addContribution(contribution.getText().toString());
        }
    }

    public interface OnAfterLoadUserListener {
        void onAfterLoadUser(Contribution contribution);
    }
}
