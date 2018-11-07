package in.ureport.network;

import android.content.Context;

import in.ureport.R;
import in.ureport.models.userdata.UserDataResponse;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UserDataServices {

    private final UserDataApi userDataApi;

    public UserDataServices(final Context context) {
        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(context.getString(R.string.user_data_host))
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        userDataApi = retrofit.create(UserDataApi.class);
    }

    public Call<UserDataResponse> getUserData(final String userKey) {
        return userDataApi.getUserData(userKey);
    }

}
