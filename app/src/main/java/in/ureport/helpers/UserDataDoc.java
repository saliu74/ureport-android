package in.ureport.helpers;

import android.content.res.Resources;
import android.os.Environment;
import android.support.annotation.StringRes;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

import in.ureport.R;
import in.ureport.models.Story;
import in.ureport.models.User;
import in.ureport.models.userdata.UserDataResponse;

public class UserDataDoc {

    private static final String rootPath = Environment.getExternalStorageDirectory().getPath();

    public static File makeUserDataPdf(final Resources res, final UserDataResponse userData)
            throws FileNotFoundException, DocumentException {
        final User user = userData.getUser();
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd", Locale.getDefault());
        final Document document = new Document();
        final String filePath =  rootPath + "/ureport-user-data.pdf";

        PdfWriter.getInstance(document, new FileOutputStream(filePath));
        document.open();
        document.setPageSize(PageSize.A4);

        final String userBirthday = dateFormat.format(user.getBirthday());
        final String userGender = user.getGenderAsEnum() == User.Gender.Male
                ? res.getString(R.string.user_gender_male)
                : res.getString(R.string.user_gender_female);

        document.add(new Paragraph(res.getString(R.string.user_data_user).toUpperCase()));
        document.add(new Paragraph("\n"));
        document.add(new Paragraph(res.getString(R.string.user_data_nickname, user.getNickname())));
        document.add(new Paragraph(res.getString(R.string.user_data_picture, user.getPicture())));
        document.add(new Paragraph(res.getString(R.string.user_data_birthday, userBirthday)));
        document.add(new Paragraph(res.getString(R.string.user_data_email, user.getEmail())));
        document.add(new Paragraph(res.getString(R.string.user_data_gender, userGender)));
        document.add(new Paragraph(res.getString(R.string.user_data_country_program, user.getCountryProgram())));
        document.add(new Paragraph(res.getString(R.string.user_data_state, user.getState())));
        if (user.getDistrict() != null) {
            document.add(new Paragraph(res.getString(R.string.user_data_district, user.getDistrict())));
        }

        document.add(new Paragraph("\n"));
        document.add(new Paragraph(res.getString(R.string.user_data_chats).toUpperCase()));
        for (UserDataResponse.Chat chat : userData.getChats()) {
            final StringBuilder chatMessagesBuilder = new StringBuilder();
            for (String message : chat.getMessages()) {
                chatMessagesBuilder.append(message).append("\n");
            }
            document.add(new Paragraph("\n"));
            document.add(new Paragraph(res.getString(R.string.user_data_chat_room, chat.getKey())));
            document.add(new Paragraph("\n"));
            document.add(new Paragraph(chatMessagesBuilder.toString()));
        }

        document.add(new Paragraph("\n"));
        document.add(new Paragraph(res.getString(R.string.user_data_stories).toUpperCase()));
        document.add(new Paragraph("\n"));

        document.add(new Paragraph("\n"));
        document.add(new Paragraph(res.getString(R.string.user_data_published_stories).toUpperCase()));
        document.add(new Paragraph(makeStoriesText(res, userData.getStories().getPublishedStories())));

        document.add(new Paragraph("\n"));
        document.add(new Paragraph(res.getString(R.string.user_data_liked_stories).toUpperCase()));
        document.add(new Paragraph(makeStoriesText(res, userData.getStories().getLikedStories())));

        document.add(new Paragraph("\n"));
        document.add(new Paragraph(res.getString(R.string.user_data_stories_in_moderation).toUpperCase()));
        document.add(new Paragraph(makeStoriesText(res, userData.getStories().getStoriesInModeration())));

        document.add(new Paragraph("\n"));
        document.add(new Paragraph(res.getString(R.string.user_data_disapproved_stories).toUpperCase()));
        document.add(new Paragraph(makeStoriesText(res, userData.getStories().getDisapprovedStories())));

        document.add(new Paragraph("\n"));
        document.add(new Paragraph(res.getString(R.string.user_data_contributions).toUpperCase()));

        document.add(new Paragraph("\n"));
        document.add(new Paragraph(res.getString(R.string.user_data_story_contributions).toUpperCase()));
        document.add(new Paragraph(makeContributionsText(res, userData.getContributions().getStoryContributions(), R.string.user_data_story_title)));

        document.add(new Paragraph("\n"));
        document.add(new Paragraph(res.getString(R.string.user_data_poll_contributions).toUpperCase()));
        document.add(new Paragraph(makeContributionsText(res, userData.getContributions().getPollContributions(), R.string.user_data_poll_title)));

        document.close();

        return new File(filePath);
    }

    public static String makeStoriesText(final Resources res, final List<Story> stories) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd, HH:mm", Locale.getDefault());
        final StringBuilder storiesBuilder = new StringBuilder();
        for (Story story : stories) {
            storiesBuilder
                    .append("\n")
                    .append(res.getString(R.string.user_data_story_title, story.getTitle()))
                    .append("\n")
                    .append(res.getString(R.string.user_data_content, story.getContent()))
                    .append("\n")
                    .append(res.getString(R.string.user_data_markers, story.getMarkers()))
                    .append("\n")
                    .append(res.getString(R.string.user_data_created_date, dateFormat.format(story.getCreatedDate())))
                    .append("\n");
        }
        return storiesBuilder.toString();
    }

    public static String makeContributionsText(final Resources res,
                                                final List<UserDataResponse.Contribution> contributions,
                                                @StringRes final int titleId) {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd, HH:mm", Locale.getDefault());
        final StringBuilder contributionsBuilder = new StringBuilder();

        for (UserDataResponse.Contribution contribution : contributions) {
            contributionsBuilder
                    .append("\n")
                    .append(res.getString(titleId, contribution.getTitle()))
                    .append("\n")
                    .append(res.getString(R.string.user_data_contribution, contribution.getContribution()))
                    .append("\n")
                    .append(res.getString(R.string.user_data_created_date, dateFormat.format(contribution.getCreatedDate())))
                    .append("\n");
        }
        return contributionsBuilder.toString();
    }

}
