package in.ureport.fragments;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;

import in.ureport.R;
import in.ureport.helpers.ValueEventListenerAdapter;
import in.ureport.managers.UserManager;
import in.ureport.models.User;
import in.ureport.models.userdata.UserDataResponse;
import in.ureport.network.UserDataApi;
import in.ureport.network.UserServices;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by johncordeiro on 18/09/15.
 */
public class GeneralSettingsFragment extends PreferenceFragmentCompat {

    private static final String PUBLIC_PROFILE_KEY = "pref_key_chat_available";
    private static final String USER_DATA_PREFERENCE = "pref_key_user_data";
    private static final String CHAT_NOTIFICATIONS_KEY = "pref_key_chat_notifications";

    private UserServices userServices;

    private SwitchPreferenceCompat publicProfilePreference;
    private Preference userDataPreference;

    private User user;

    @Override
    public void onCreatePreferences(Bundle bundle, String rootKey) {
        setPreferencesFromResource(R.xml.general_settings_preferences, rootKey);

        setupView();
        setupObjects();
        loadUser();
    }

    private void setupView() {
        publicProfilePreference = (SwitchPreferenceCompat)getPreferenceManager().findPreference(PUBLIC_PROFILE_KEY);
        publicProfilePreference.setOnPreferenceChangeListener(onPublicProfilePreferenceChangeListener);
        userDataPreference = getPreferenceManager().findPreference(USER_DATA_PREFERENCE);
        userDataPreference.setOnPreferenceClickListener(userDataPreferenceClickListener);
    }

    private void setupObjects() {
        userServices = new UserServices();
    }

    private void loadUser() {
        userServices.getUser(UserManager.getUserId(), new ValueEventListenerAdapter() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                super.onDataChange(dataSnapshot);
                user = dataSnapshot.getValue(User.class);
                if (user != null) updateViewForUser(user);
            }
        });
    }

    private void updateViewForUser(User user) {
        publicProfilePreference.setChecked(user.getPublicProfile());
    }

    private UserDataApi getUserDataService() {
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://us-central1-u-report-dev.cloudfunctions.net")
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(UserDataApi.class);
    }

    private void downloadUserData() {
        final ProgressDialog progressDialog = ProgressDialog.show(
                getContext(),
                null,
                getString(R.string.load_message_wait),
                true,
                false
        );
        final UserDataApi userDataService = getUserDataService();
        final Call<UserDataResponse> userData = userDataService.getUserData(UserManager.getUserId());

        userData.enqueue(new Callback<UserDataResponse>() {
            @Override
            public void onResponse(Call<UserDataResponse> call, Response<UserDataResponse> response) {
                progressDialog.dismiss();
                Toast.makeText(getContext(), "User data downloaded", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(Call<UserDataResponse> call, Throwable throwable) {
                progressDialog.dismiss();
                Toast.makeText(getContext(), "User data downloaded", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Preference.OnPreferenceChangeListener onPublicProfilePreferenceChangeListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object object) {
            publicProfilePreference.setChecked(!publicProfilePreference.isChecked());
            if(user != null) {
                userServices.changePublicProfile(user, publicProfilePreference.isChecked(), onSettingsSavedListener);
            } else {
                displayMessage(R.string.error_update_user);
            }
            return false;
        }
    };

    private Preference.OnPreferenceClickListener userDataPreferenceClickListener = preference -> {
        new AlertDialog.Builder(getContext())
                .setMessage(R.string.message_download_user_data)
                .setPositiveButton(R.string.yes, (dialog, i) -> downloadUserData())
                .setNegativeButton(R.string.no, (dialog, i) -> dialog.dismiss())
                .show();
        return true;
    };

    private Firebase.CompletionListener onSettingsSavedListener = new Firebase.CompletionListener() {
        @Override
        public void onComplete(FirebaseError firebaseError, Firebase firebase) {
            if(firebaseError == null) {
                displayMessage(R.string.message_success_user_update);
            } else {
                displayMessage(R.string.error_update_user);
            }
        }
    };

    private void displayMessage(int message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }
}
