package in.ureport.fragments;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.widget.Toast;

import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.itextpdf.text.DocumentException;

import java.io.File;
import java.io.FileNotFoundException;

import in.ureport.R;
import in.ureport.helpers.PermissionHelper;
import in.ureport.helpers.UserDataDoc;
import in.ureport.helpers.ValueEventListenerAdapter;
import in.ureport.managers.UserManager;
import in.ureport.models.User;
import in.ureport.models.userdata.UserDataResponse;
import in.ureport.network.UserDataServices;
import in.ureport.network.UserServices;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by johncordeiro on 18/09/15.
 */
public class GeneralSettingsFragment extends PreferenceFragmentCompat {

    private static final String PUBLIC_PROFILE_KEY = "pref_key_chat_available";
    private static final String USER_DATA_PREFERENCE = "pref_key_user_data";
    private static final String CHAT_NOTIFICATIONS_KEY = "pref_key_chat_notifications";

    private static final int REQUEST_CODE_WRITE_PDF = 100;

    private UserServices userServices;
    private UserDataServices userDataServices;

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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_WRITE_PDF && PermissionHelper.allPermissionsGranted(grantResults)) {
            downloadUserData();
        }
    }

    private void setupView() {
        publicProfilePreference = (SwitchPreferenceCompat)getPreferenceManager().findPreference(PUBLIC_PROFILE_KEY);
        publicProfilePreference.setOnPreferenceChangeListener(onPublicProfilePreferenceChangeListener);
        userDataPreference = getPreferenceManager().findPreference(USER_DATA_PREFERENCE);
        userDataPreference.setOnPreferenceClickListener(userDataPreferenceClickListener);
    }

    private void setupObjects() {
        userServices = new UserServices();
        userDataServices = new UserDataServices(getContext());
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

    private void downloadUserData() {
        if (!PermissionHelper.isPermissionGranted(
                getContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_WRITE_PDF);
            return;
        }

        final ProgressDialog progressDialog = ProgressDialog.show(
                getContext(),
                null,
                getString(R.string.load_message_wait),
                true,
                false
        );
        userDataServices.getUserData(UserManager.getUserId()).enqueue(new Callback<UserDataResponse>() {
            @Override
            public void onResponse(Call<UserDataResponse> call, Response<UserDataResponse> response) {
                progressDialog.dismiss();

                if (response.code() != 200) {
                    displayMessage(R.string.error_request);
                    return;
                }

                progressDialog.show();
                try {
                    final File pdfFile = UserDataDoc.makeUserDataPdf(getResources(), response.body());
                    final Uri pdfFileUri = FileProvider.getUriForFile(
                            getContext(),
                            "in.ureport.UreportApplication.provider",
                            pdfFile
                    );
                    final Intent intent = new Intent(Intent.ACTION_VIEW)
                            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            .setDataAndType(pdfFileUri, "application/pdf");

                    progressDialog.dismiss();
                    startActivity(intent);
                } catch (FileNotFoundException | DocumentException e) {
                    e.printStackTrace();
                    progressDialog.dismiss();
                    displayMessage(R.string.user_data_pdf_error);
                }
            }

            @Override
            public void onFailure(Call<UserDataResponse> call, Throwable throwable) {
                progressDialog.dismiss();
                displayMessage(R.string.error_request);
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
