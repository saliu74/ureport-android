package in.ureport.fragments;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.support.v7.widget.Toolbar;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import java.util.Date;

import br.com.ilhasoft.support.tool.UnitConverter;
import br.com.ilhasoft.support.utils.KeyboardHandler;
import in.ureport.R;
import in.ureport.helpers.ChildEventListenerAdapter;
import in.ureport.helpers.ImageLoader;
import in.ureport.helpers.SpaceItemDecoration;
import in.ureport.helpers.ValueEventListenerAdapter;
import in.ureport.managers.FcmTopicManager;
import in.ureport.managers.MediaViewer;
import in.ureport.managers.UserManager;
import in.ureport.models.Contribution;
import in.ureport.models.Story;
import in.ureport.models.User;
import in.ureport.network.ContributionServices;
import in.ureport.network.StoryServices;
import in.ureport.network.UserServices;
import in.ureport.tasks.CleanContributionNotificationTask;
import in.ureport.tasks.SendGcmContributionTask;
import in.ureport.tasks.ShareStoryTask;
import in.ureport.views.adapters.ContributionAdapter;
import in.ureport.views.adapters.MediaAdapter;

/**
 * Created by johncordeiro on 7/16/15.
 */
public class StoryViewFragment extends ProgressFragment
        implements ContributionAdapter.OnContributionRemoveListener,
        ContributionAdapter.OnContributionDenounceListener {

    private static final String EXTRA_STORY = "story";
    private static final String EXTRA_USER = "user";
    private static final String EXTRA_IS_LOADED = "loaded";

    private Story story;
    private User user;
    private Boolean isLoaded;

    private ContributionAdapter contributionAdapter;
    private TextView contributions;
    private View contribute;
    private TextView likeCount;
    private EditText contribution;
    private TextView author;
    private ImageView picture;
    private View addContributionContainer;
    private NestedScrollView scrollView;
    private FloatingActionButton shareActionButton;

    private ContributionServices contributionServices;
    private StoryServices storyServices;
    private UserServices userServices;

    private static Firebase.CompletionListener firebaseContributionDenouncedListener;
    private static Firebase.CompletionListener firebaseContributionRemovedListener;

    private MediaViewer mediaViewer;
    private KeyboardHandler keyboardHandler;
    private int storyLikeCount;

    public static StoryViewFragment newInstance(Story story, User user) {
        return newInstance(story, user, true);
    }

    public static StoryViewFragment newInstance(Story story, User user, Boolean isLoaded) {
        StoryViewFragment storyViewFragment = new StoryViewFragment();

        Bundle args = new Bundle();
        args.putParcelable(EXTRA_STORY, story);
        args.putParcelable(EXTRA_USER, user);
        args.putBoolean(EXTRA_IS_LOADED, isLoaded);
        storyViewFragment.setArguments(args);

        return storyViewFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupContextDependencies();
        if (getArguments() != null && getArguments().containsKey(EXTRA_STORY)
                && getArguments().containsKey(EXTRA_USER)) {
            story = getArguments().getParcelable(EXTRA_STORY);
            user = getArguments().getParcelable(EXTRA_USER);
            isLoaded = getArguments().getBoolean(EXTRA_IS_LOADED, true);
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
        setLoadingMessage(getString(R.string.load_message_wait));
        if (isLoaded) {
            setupView(view);
            loadData();
        } else {
            loadStoryAndSetupView(view);
        }
    }

    private void setupContextDependencies() {
        firebaseContributionDenouncedListener = (firebaseError, firebase) -> {
            dismissLoading();
            if (firebaseError == null)
                displayToast(R.string.message_success_denounce);
            else
                displayToast(R.string.error_remove);
        };
        firebaseContributionRemovedListener = (firebaseError, firebase) -> {
            dismissLoading();
            if (firebaseError == null)
                displayToast(R.string.message_success_remove);
            else
                displayToast(R.string.error_remove);
        };
    }

    private void loadStoryAndSetupView(final View view) {
        storyServices.loadStory(story, new ValueEventListenerAdapter() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                super.onDataChange(dataSnapshot);

                if (isAdded()) {
                    story = dataSnapshot.getValue(Story.class);
                    story.setKey(dataSnapshot.getKey());
                    setupView(view);
                    loadData();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        cleanNotification();
    }

    @Override
    public void onPause() {
        super.onPause();
        cleanNotification();
    }

    private void loadData() {
        contributionServices.addChildEventListener(story.getKey(), contributionChildEventListener);
        loadUserIfNeeded();
        checkLikeForUser();
        loadStoryLikesCount();
    }

    private void loadStoryLikesCount() {
        storyServices.loadStoryLikeCount(story, new ValueEventListenerAdapter() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                super.onDataChange(dataSnapshot);
                if (isAdded())
                    updateLikes((int) dataSnapshot.getChildrenCount());
            }
        });
    }

    private void updateLikes(int likesCount) {
        storyLikeCount = likesCount;
        likeCount.setText(getResources().getQuantityString(R.plurals.like_count, storyLikeCount, storyLikeCount));
    }

    private void checkLikeForUser() {
        storyServices.checkLikeForUser(story, new ValueEventListenerAdapter() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                likeCount.setSelected(dataSnapshot.exists());
            }
        });
    }

    private void loadUserIfNeeded() {
        if (user != null) {
            user.setKey(UserManager.getUserId());
        } else if (UserManager.isUserLoggedIn()) {
            userServices.getUser(UserManager.getUserId(), new ValueEventListenerAdapter() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    super.onDataChange(dataSnapshot);
                    user = dataSnapshot.getValue(User.class);
                    user.setKey(dataSnapshot.getKey());
                }
            });
        }
    }

    private void setupObjects() {
        contributionServices = new ContributionServices(ContributionServices.Type.Story);
        userServices = new UserServices();
        storyServices = new StoryServices();
        mediaViewer = new MediaViewer((AppCompatActivity) getActivity());
        keyboardHandler = new KeyboardHandler();
    }

    private void setupView(View view) {
        setHasOptionsMenu(true);
        Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);

        AppCompatActivity activity = ((AppCompatActivity) getActivity());
        activity.setSupportActionBar(toolbar);
        activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        TextView title = (TextView) view.findViewById(R.id.title);
        title.setText(story.getTitle());

        TextView content = (TextView) view.findViewById(R.id.content);
        content.setMovementMethod(LinkMovementMethod.getInstance());
        content.setText(story.getContent());

        scrollView = (NestedScrollView) view.findViewById(R.id.scrollView);
        if (scrollView != null) {
            scrollTo(View.FOCUS_UP);
        }

        TextView markers = (TextView) view.findViewById(R.id.markers);
        setupMarkers(markers);

        author = (TextView) view.findViewById(R.id.tags);
        picture = (ImageView) view.findViewById(R.id.picture);

        setupUser();

        contributions = (TextView) view.findViewById(R.id.contributors);
        contributions.setText(getContributionsText(story));

        addContributionContainer = view.findViewById(R.id.addContributionContainer);

        contribute = view.findViewById(R.id.contribute);
        contribute.setOnClickListener(onContributeClickListener);

        ImageButton addContribution = (ImageButton) view.findViewById(R.id.addContribution);
        addContribution.setOnClickListener(onAddContributionClickListener);

        contribution = (EditText) view.findViewById(R.id.contribution);
        contribution.setOnEditorActionListener(onDescriptionEditorActionListener);

        likeCount = (TextView) view.findViewById(R.id.likeCount);
        likeCount.setOnClickListener(onLikeClickListener);

        RecyclerView contributionList = (RecyclerView) view.findViewById(R.id.contributionList);
        ((SimpleItemAnimator) contributionList.getItemAnimator()).setSupportsChangeAnimations(false);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getContext()
                , LinearLayoutManager.VERTICAL, false);
        linearLayoutManager.setAutoMeasureEnabled(true);
        linearLayoutManager.setStackFromEnd(true);
        contributionList.setLayoutManager(linearLayoutManager);

        contributionAdapter = new ContributionAdapter();
        contributionAdapter.setOnContributionRemoveListener(this);
        contributionAdapter.setOnContributionDenounceListener(this);
        contributionList.setAdapter(contributionAdapter);

        RecyclerView mediaList = (RecyclerView) view.findViewById(R.id.mediaList);
        setupMediaList(mediaList);

        shareActionButton = (FloatingActionButton) view.findViewById(R.id.share);
        if (shareActionButton != null) {
            shareActionButton.setOnClickListener(onShareClickListener);
        }
    }

    private void scrollTo(int direction) {
        scrollView.postDelayed(() -> scrollView.fullScroll(direction), 200);
    }

    private void setupUser() {
        if (story.getUserObject() != null) {
            setupUserView(story.getUserObject());
        } else {
            userServices.getUser(story.getUser(), new ValueEventListenerAdapter() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    super.onDataChange(dataSnapshot);
                    User user = dataSnapshot.getValue(User.class);
                    setupUserView(user);
                }
            });
        }
    }

    private void setupUserView(User user) {
        author.setText(user.getNickname());
        ImageLoader.loadPersonPictureToImageView(picture, user.getPicture());
    }

    private void setupMarkers(TextView markers) {
        if (story.getMarkers() != null && !story.getMarkers().isEmpty()) {
            markers.setText(story.getMarkers());
            markers.setVisibility(View.VISIBLE);
        } else {
            markers.setVisibility(View.GONE);
        }
    }

    private void setupMediaList(RecyclerView mediaList) {
        if (story.getMedias() != null && story.getMedias().size() > 0) {
            mediaList.setVisibility(View.VISIBLE);
            mediaList.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));

            UnitConverter converter = new UnitConverter(getActivity());

            SpaceItemDecoration mediaItemDecoration = new SpaceItemDecoration();
            mediaItemDecoration.setHorizontalSpaceWidth((int) converter.convertDpToPx(10));
            mediaList.addItemDecoration(mediaItemDecoration);

            MediaAdapter adapter = new MediaAdapter(story.getMedias(), false);
            adapter.setOnMediaViewListener(mediaViewer);
            mediaList.setAdapter(adapter);
        } else {
            mediaList.setVisibility(View.GONE);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        final int menuRes;
        if (UserManager.canModerate()) {
            menuRes = R.menu.menu_reject_story;
        } else if (story.getUser().equals(UserManager.getUserId())) {
            menuRes = R.menu.menu_delete_story;
        } else {
            menuRes = R.menu.menu_denounce_story;
        }
        inflater.inflate(menuRes, menu);

        if (shareActionButton == null) {
            MenuItem menuItem = menu.add(Menu.NONE, R.id.share, Menu.NONE, R.string.title_share);
            menuItem.setIcon(R.drawable.ic_share_white_24dp);
            MenuItemCompat.setShowAsAction(menuItem, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.disapproveStory:
                disapproveStory();
                break;
            case R.id.deleteStory:
                deleteStory();
                break;
            case R.id.denounceStory:
                denounceStory();
                break;
            case R.id.share:
                shareStory();
        }
        return super.onOptionsItemSelected(item);
    }

    private void disapproveStory() {
        storyServices.removeStory(story, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError == null) {
                    displayToast(R.string.message_story_disapproved);
                    getActivity().finish();
                } else {
                    displayToast(R.string.error_remove);
                }
            }
        });
    }

    private void deleteStory() {
        storyServices.removeStory(story, (firebaseError, firebase) -> {
            if (firebaseError == null) {
                displayToast(R.string.message_success_remove);
                getActivity().finish();
            } else {
                displayToast(R.string.error_remove);
            }
        });
    }

    private void denounceStory() {
        storyServices.denounceStory(story, new Firebase.CompletionListener() {
            @Override
            public void onComplete(FirebaseError firebaseError, Firebase firebase) {
                if (firebaseError == null) {
                    displayToast(R.string.message_story_denounced);
                } else {
                    displayToast(R.string.error_remove);
                }
            }
        });
    }

    private String getContributionsText(Story story) {
        return String.format(getString(R.string.stories_list_item_contributions), story.getContributions());
    }

    private View.OnClickListener onContributeClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (UserManager.validateKeyAction(getActivity())) {
                updateViewForContribution();
            }
        }
    };

    private View.OnClickListener onShareClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            shareStory();
        }
    };

    private void shareStory() {
        ShareStoryTask shareStoryTask = new ShareStoryTask(StoryViewFragment.this, story);
        shareStoryTask.execute();
    }

    public void addContribution(String content) {
        if (UserManager.validateKeyAction(getActivity())) {
            final Contribution contribution = new Contribution(content, user);
            contribution.setCreatedDate(new Date());

            contributionServices.saveContribution(story.getKey(), contribution, (firebaseError, firebase) -> {
                if (firebaseError == null) {
                    userServices.incrementContributionPoint();

                    resetCommentsView();
                    incrementContributionsText();
                    refreshContribution();
                    addAuthorToTopic();
                    sendNotification(contribution);
                }
            });
        }
    }

    private void resetCommentsView() {
        StoryViewFragment.this.contribution.setText(null);
        if (scrollView != null) {
            scrollTo(View.FOCUS_DOWN);
            keyboardHandler.changeKeyboardVisibility(getActivity(), false);
        }
    }

    private void sendNotification(Contribution contribution) {
        contribution.setAuthor(user);
        SendGcmContributionTask sendGcmContributionTask = new SendGcmContributionTask(getActivity(), story);
        sendGcmContributionTask.execute(contribution);
    }

    private void addAuthorToTopic() {
        FcmTopicManager fcmTopicManager = new FcmTopicManager(getActivity());
        fcmTopicManager.registerToStoryTopic(user, story);
    }

    private void incrementContributionsText() {
        Integer contributions = story.getContributions();
        if (contributions != null) {
            story.setContributions(contributions + 1);
        } else {
            story.setContributions(1);
        }
    }

    private ChildEventListenerAdapter contributionChildEventListener = new ChildEventListenerAdapter() {
        @Override
        public void onChildAdded(DataSnapshot dataSnapshot, String previousChild) {
            super.onChildAdded(dataSnapshot, previousChild);

            Contribution contribution = getContributionFromSnapshot(dataSnapshot);
            loadUserFromContribution(contribution, onAfterLoadUserListener);
        }

        @Override
        public void onChildRemoved(DataSnapshot dataSnapshot) {
            super.onChildRemoved(dataSnapshot);

            Contribution contribution = getContributionFromSnapshot(dataSnapshot);
            contributionAdapter.removeContribution(contribution);
        }
    };

    @NonNull
    private Contribution getContributionFromSnapshot(DataSnapshot dataSnapshot) {
        final Contribution contribution = dataSnapshot.getValue(Contribution.class);
        contribution.setKey(dataSnapshot.getKey());
        return contribution;
    }

    private OnAfterLoadUserListener onAfterLoadUserListener = new OnAfterLoadUserListener() {
        @Override
        public void onAfterLoadUser(Contribution contribution) {
            updateViewForContribution();
            if (contribution.getAuthor() != null) {
                contributionAdapter.addContribution(contribution);
            }
        }
    };

    private void updateViewForContribution() {
        addContributionContainer.setVisibility(View.VISIBLE);
        contribute.setVisibility(View.GONE);
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
            return true;
        }
    };

    private void onAddNewContribution() {
        if (contribution.getText().length() > 0) {
            addContribution(contribution.getText().toString());
        }
    }

    @Override
    public void onContributionRemove(Contribution contribution) {
        showLoading();
        contributionServices.removeContribution(story.getKey(), contribution, (firebaseError, firebase) ->
                firebaseContributionRemovedListener.onComplete(firebaseError, firebase));
    }

    @Override
    public void onContributionDenounce(Contribution contribution) {
        showLoading();
        contributionServices.denounceContribution(story.getKey(), contribution, (firebaseError, firebase) ->
                firebaseContributionDenouncedListener.onComplete(firebaseError, firebase));
    }

    private void cleanNotification() {
        CleanContributionNotificationTask cleanContributionNotificationTask = new CleanContributionNotificationTask(getActivity());
        cleanContributionNotificationTask.execute(story);
    }

    private void displayToast(@StringRes int messageId) {
        Toast.makeText(getContext(), messageId, Toast.LENGTH_SHORT).show();
    }

    private View.OnClickListener onLikeClickListener = view -> {
        if (UserManager.validateKeyAction(getActivity())) {
            boolean selected = view.isSelected();
            view.setSelected(!selected);

            storyLikeCount += selected ? -1 : 1;
            updateLikes(storyLikeCount);

            toggleLike(view, selected);
        }
    };

    private void toggleLike(View view, boolean selected) {
        if (selected) {
            storyServices.removeStoryLike(story, user
                    , (FirebaseError firebaseError, Firebase firebase) -> view.setSelected(false));
        } else {
            storyServices.addStoryLike(story, user
                    , (FirebaseError firebaseError, Firebase firebase) -> view.setSelected(true));
        }
    }

    public interface OnAfterLoadUserListener {
        void onAfterLoadUser(Contribution contribution);
    }
}
