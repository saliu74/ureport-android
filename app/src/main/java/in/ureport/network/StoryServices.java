package in.ureport.network;

import com.firebase.client.ChildEventListener;
import com.firebase.client.Firebase;
import com.firebase.client.Query;
import com.firebase.client.ValueEventListener;

import in.ureport.managers.UserManager;
import in.ureport.models.Story;
import in.ureport.models.User;

/**
 * Created by johncordeiro on 14/08/15.
 */
public class StoryServices extends ProgramServices {

    private static final String storyPath = "story";
    private static final String storyLikePath = "story_like";
    private static final String storyModeratePath = "story_moderate";
    private static final String storyDisapprovedPath = "story_disapproved";
    private static final String storyDenouncedPath = "story_denounced";

    public void saveStory(final Story story, final Firebase.CompletionListener listener) {
        cleanStory(story);
        story.setUser(UserManager.getUserId());
        getDefaultRoot().child(storyModeratePath).push().setValue(story, listener);
    }

    public void loadStory(final Story story, ValueEventListener listener) {
        getDefaultRoot().child(storyPath).child(story.getKey()).addListenerForSingleValueEvent(listener);
    }

    public void loadStoryLikeCount(final Story story, ValueEventListener listener) {
        getDefaultRoot().child(storyLikePath)
                .child(story.getKey()).addListenerForSingleValueEvent(listener);
    }

    public void addStoryLike(Story story, User user, Firebase.CompletionListener listener) {
        getDefaultRoot().child(storyLikePath).child(story.getKey())
                .child(user.getKey()).setValue(true, listener);
    }

    public void removeStoryLike(Story story, User user, Firebase.CompletionListener listener) {
        getDefaultRoot().child(storyLikePath).child(story.getKey())
                .child(user.getKey()).removeValue(listener);
    }

    public void checkLikeForUser(Story story, ValueEventListener listener) {
        String userId = UserManager.getUserId();
        if(userId != null) {
            getDefaultRoot().child(storyLikePath).child(story.getKey())
                    .child(userId).addListenerForSingleValueEvent(listener);
        }
    }

    public void approveStory(Story story, Firebase.CompletionListener listener) {
        cleanStory(story);
        getDefaultRoot().child(storyModeratePath).child(story.getKey()).removeValue();
        getDefaultRoot().child(storyPath).child(story.getKey()).setValue(story, listener);
    }

    public void removeStoryByModerator(Story story, Firebase.CompletionListener listener) {
        cleanStory(story);
        getDefaultRoot().child(storyPath).child(story.getKey()).removeValue();
        getDefaultRoot().child(storyDisapprovedPath).child(story.getKey()).setValue(story, listener);
    }

    public void removeStory(Story story, Firebase.CompletionListener listener) {
        cleanStory(story);
        getDefaultRoot().child(storyPath).child(story.getKey()).removeValue(listener);
    }

    public void disapprovedStory(Story story, Firebase.CompletionListener listener) {
        cleanStory(story);
        getDefaultRoot().child(storyModeratePath).child(story.getKey()).removeValue();
        getDefaultRoot().child(storyDisapprovedPath).child(story.getKey()).setValue(story, listener);
    }

    public void denounceStory(Story story, Firebase.CompletionListener listener) {
        cleanStory(story);
        getDefaultRoot().child(storyDenouncedPath).child(story.getKey()).setValue(story, listener);
    }

    public void loadStoriesForUser(User user, ValueEventListener listener) {
        getStoryQueryByUser(user).addListenerForSingleValueEvent(listener);
    }

    public void addChildEventListenerForUser(User user, ChildEventListener childEventListener) {
        getStoryQueryByUser(user).addChildEventListener(childEventListener);
    }

    public Query getStoryQueryByUser(User user) {
        return getCountryProgram().child(user.getCountryProgram()).child(storyPath)
                .orderByChild("user").equalTo(user.getKey());
    }

    public void addStoryModerateChildEventListener(ChildEventListener childEventListener) {
        getStoriesModerationQuery().addChildEventListener(childEventListener);
    }

    public Firebase getStoriesModerationQuery() {
        return getDefaultRoot().child(storyModeratePath);
    }

    public Firebase getStoryReference() {
        return getDefaultRoot().child(storyPath);
    }

    public void addChildEventListener(ChildEventListener childEventListener) {
        getStoryReference().addChildEventListener(childEventListener);
    }

    public void removeChildEventListener(ChildEventListener childEventListener) {
        getStoryReference().removeEventListener(childEventListener);
    }

    private void cleanStory(Story story) {
        story.setUserObject(null);
    }
}
