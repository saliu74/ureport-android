package in.ureport.network;

import in.ureport.models.userdata.UserDataResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface UserDataApi {

    @GET("/userData")
    Call<UserDataResponse> getUserData(@Query("userKey") String userKey);

}
