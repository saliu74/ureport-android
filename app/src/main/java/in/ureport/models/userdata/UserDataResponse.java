package in.ureport.models.userdata;

import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.List;

import in.ureport.models.Story;
import in.ureport.models.User;

public class UserDataResponse {

    private User user;
    private List<Chat> chats;
    private Stories stories;
    private Contributions contributions;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<Chat> getChats() {
        return chats;
    }

    public void setChats(List<Chat> chats) {
        this.chats = chats;
    }

    public Stories getStories() {
        return stories;
    }

    public void setStories(Stories stories) {
        this.stories = stories;
    }

    public Contributions getContributions() {
        return contributions;
    }

    public void setContributions(Contributions contributions) {
        this.contributions = contributions;
    }

    public class Chat {
        @SerializedName("chatType")
        private String type;
        private List<String> messages;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public List<String> getMessages() {
            return messages;
        }

        public void setMessages(List<String> messages) {
            this.messages = messages;
        }

    }

    public class Stories {

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

    public class Contributions {

        private List<Contribution> storyContributions;
        private List<Contribution> pollContributions;

        public List<Contribution> getStoryContributions() {
            return storyContributions;
        }

        public void setStoryContributions(List<Contribution> storyContributions) {
            this.storyContributions = storyContributions;
        }

        public List<Contribution> getPollContributions() {
            return pollContributions;
        }

        public void setPollContributions(List<Contribution> pollContributions) {
            this.pollContributions = pollContributions;
        }

    }

    public class Contribution {

        @SerializedName(value = "storyTitle", alternate = {"pollTitle"})
        private String title;
        private String contribution;
        private Date createdDate;

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

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

}
