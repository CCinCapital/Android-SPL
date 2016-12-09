/* Author: Can Chen 100778989
 * This class enables the local storage of user information
 */

package canchen.spl;

//Widgets and Libraries required for this class
import android.content.Context;
import android.content.SharedPreferences;

public class UserLocalStore {

    //A SharedPreferences instance pointing to the file
    //that contains the values of preferences that are managed by this.
    public  static  final  String SP_NAME = "userDetails";
    SharedPreferences userLocalDatabase;

    public UserLocalStore(Context context){
        userLocalDatabase = context.getSharedPreferences(SP_NAME, 0);
    }

    //method that stores user data
    public void storeUserData(User user){
        SharedPreferences.Editor spEditor = userLocalDatabase.edit();
        spEditor.putString("username", user.username);
        spEditor.putString("password", user.password);
        spEditor.commit();
    }

    //method that get user login status
    //return false is no user has logged in
    //return user information if user had logged in
    public User getLoggedInUser(){
        if (userLocalDatabase.getBoolean("loggedIn", false) == false) {
            return null;
        }
        String username = userLocalDatabase.getString("username", "");
        String password = userLocalDatabase.getString("password", "");
        User user = new User(username, password);
        return user;
    }

    //change user login status to the boolean of loggedIn
    public void setUserLoggedIn(boolean loggedIn){
        SharedPreferences.Editor userLocalDatabaseEditor = userLocalDatabase.edit();
        userLocalDatabaseEditor.putBoolean("loggedIn", loggedIn);
        userLocalDatabaseEditor.commit();
    }

    //clear user's data
    public void clearUserData(){
        SharedPreferences.Editor userLocalDatabaseEditor = userLocalDatabase.edit();
        userLocalDatabaseEditor.clear();
        userLocalDatabaseEditor.commit();
    }
}
