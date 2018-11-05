package in.ureport.models.userdata;

import java.util.Date;
import java.util.List;

import in.ureport.models.Story;
import in.ureport.models.User;

public class UserDataResponse {

    private User user;
    private List<Chat> chats;
    private StoriesResponse stories;
    private ContributionsResponse contributions;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    class Chat {

        private String chatRoomKey;
        private List<String> messages;

        public String getChatRoomKey() {
            return chatRoomKey;
        }

        public void setChatRoomKey(String chatRoomKey) {
            this.chatRoomKey = chatRoomKey;
        }

        public List<String> getMessages() {
            return messages;
        }

        public void setMessages(List<String> messages) {
            this.messages = messages;
        }

    }

    class StoriesResponse {

        private List<Story> publishedStories;
        private List<Story> likedStories;
        private List<Story> storiesInModeration;
        private List<Story> disapprovedStories;

        public List<Story> getPublishedStories() {
            return publishedStories;
        }

        public void setPublishedStories(List<Story> publishedStories) {
            this.publishedStories = publishedStories;
        }

        public List<Story> getLikedStories() {
            return likedStories;
        }

        public void setLikedStories(List<Story> likedStories) {
            this.likedStories = likedStories;
        }

        public List<Story> getStoriesInModeration() {
            return storiesInModeration;
        }

        public void setStoriesInModeration(List<Story> storiesInModeration) {
            this.storiesInModeration = storiesInModeration;
        }

        public List<Story> getDisapprovedStories() {
            return disapprovedStories;
        }

        public void setDisapprovedStories(List<Story> disapprovedStories) {
            this.disapprovedStories = disapprovedStories;
        }

    }

    class ContributionsResponse {

        private List<StoryContributionResponse> storyContributions;
        private List<PollContributionResponse> pollContributions;

        public List<StoryContributionResponse> getStoryContributions() {
            return storyContributions;
        }

        public void setStoryContributions(List<StoryContributionResponse> storyContributions) {
            this.storyContributions = storyContributions;
        }

        public List<PollContributionResponse> getPollContributions() {
            return pollContributions;
        }

        public void setPollContributions(List<PollContributionResponse> pollContributions) {
            this.pollContributions = pollContributions;
        }

    }

    class ContributionResponse {

        private String contribution;
        private Date createdDate;

        public String getContribution() {
            return contribution;
        }

        public void setContribution(String contribution) {
            this.contribution = contribution;
        }

        public Date getCreatedDate() {
            return createdDate;
        }

        public void setCreatedDate(Date createdDate) {
            this.createdDate = createdDate;
        }

    }

    class StoryContributionResponse extends ContributionResponse {

        private String storyTitle;

        public String getStoryTitle() {
            return storyTitle;
        }

        public void setStoryTitle(String storyTitle) {
            this.storyTitle = storyTitle;
        }

    }

    class PollContributionResponse extends ContributionResponse {

        private String pollTitle;

        public String getPollTitle() {
            return pollTitle;
        }

        public void setPollTitle(String pollTitle) {
            this.pollTitle = pollTitle;
        }

    }

}
