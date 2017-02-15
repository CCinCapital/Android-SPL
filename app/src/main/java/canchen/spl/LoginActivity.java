/* Author: Can Chen 100778989
*  LoginActivity logs user into the service
*  User has to provide username and password
*/

package canchen.spl;

//Widgets and Libraries required for this class
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

//class must implement interface of Asyncresponse to retrieve result from asyncactivity
//class must implement activity in order to display something on screen
//HAS ISSUE WHEN INTERNET IS NOT AVAILABLE, NEED TO SET A EXPIRE TIMER.
//ADD LOCAL STORE TO KEEP USERNAME AND PASSWORD IN TEXT BOX
public class LoginActivity extends Activity implements AsyncResponse{

    //Declaration of Buttons and Text fields
    Button bLogin;
    EditText etUsername, etPassword;
    TextView tvRegisterLink, tvForgotPassword;
    CheckBox cbRemember;

    //Local Database of stored User
    UserLocalStore userLocalStore;

    //variable that used to pass information between class
    public String loginResult = "Pending";

    //Method automatically executes whenever This activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set the XML Layout of this View
        setContentView(R.layout.activity_login);
        //link layout fields with its function
        etUsername = (EditText) findViewById(R.id.etUsername);
        etPassword = (EditText) findViewById(R.id.etPassword);
        bLogin = (Button) findViewById(R.id.bLogin);
        tvRegisterLink = (TextView) findViewById(R.id.tvRegisterLink);
        tvForgotPassword = (TextView) findViewById(R.id.tvForgotPassword);
        cbRemember = (CheckBox) findViewById(R.id.cbRemember);

        bLogin.setOnClickListener(new OnClickListener() {
            @Override
            //executes when this button is clicked
            public void onClick(View v) {
                //call the function
                authenticate();
            }
        });

        tvRegisterLink.setOnClickListener(new OnClickListener() {
            @Override
            //executes when this button is clicked
            public void onClick(View v) {
                //start register window
                startActivity(new Intent(v.getContext(), RegisterActivity.class));
            }
        });

        tvForgotPassword.setOnClickListener(new OnClickListener() {
            @Override
            //executes when this button is clicked
            public void onClick(View v) {
                //display forget password window
                startActivity(new Intent(v.getContext(), ForgotPasswordActivity.class));
            }
        });

        cbRemember.setOnClickListener(new OnClickListener() {
            @Override
            //executes when this button is clicked
            public void onClick(View v) {
                if(cbRemember.isChecked()){
                    //not implemented yet
                    Log.d("LOGIN","Check Box Checked");
                }
            }
        });

        //create new local storage
        userLocalStore = new UserLocalStore(this);
    }

    //store user information and change the login status of user
    private void logUserIn(User returnedUser) {
        userLocalStore.storeUserData(returnedUser);
        userLocalStore.setUserLoggedIn(true);
    }

    //get the user typed information and call the BackgroundLogin()
    private void authenticate() {
        //read the typed information and convert it to string
        String username = etUsername.getText().toString();
        String password = etPassword.getText().toString();
        //create a new user with provided information
        User user = new User(username, password);
        //initiate the BackgroundRegister class
        BackgroundLogin backgroundLogin = new BackgroundLogin(this);
        //setup the interface to retrieve result
        backgroundLogin.delegate = this;
        //execute the class
        backgroundLogin.execute(username, password);
        //store user information and change the login status of user
        logUserIn(user);
    }

    //get the response from it's async classes
    public void fetchAsyncResponse(String output){
        loginResult = output;
    }

    //inner class that log user into the service
    private class BackgroundLogin extends AsyncTask<String, Integer, String> {
        //variables that keeps the login information that are required when connecting to server
        private String dbURL = "jdbc:mysql://localhost.000webhost.com/id522773_sps_test";
        private String dbUsername = "id522773_sps_test";
        private String dbPassword = "d_@Afr7spuDR";
        //interface that passes result to parent class
        public AsyncResponse delegate = null;
        //true if username present in database, false as opposite
        boolean userFound = false;
        //context of this class
        Context ctx;
        BackgroundLogin(Context ctx){this.ctx = ctx;}
        //initiate the progress dialog
        ProgressDialog progressDialog;

        //executes and finishes before doInBackground executes
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            //create a new progress dialog
            //set its parameters
            progressDialog = new ProgressDialog(ctx);
            progressDialog.setTitle("Login in Progress");
            progressDialog.setMessage("Please wait ...");
            progressDialog.setIndeterminate(false);
            //the progress will be 0, 1, and 2
            progressDialog.setMax(2);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            //user cannot cancel the dialog
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        //where to put all the background jobs
        @Override
        protected String doInBackground(String... params) {
            //username is the first parameter of params, password the second
            String username = params[0];
            String password = params[1];
            //different scenarios
            //username must longer than 2 characters
            //password also has minimum length requirement
            if(username.length() < 3){
                loginResult = "Inadequate Username";
            }else if(password.length() < 5){
                loginResult = "Inadequate Password";
            }else{
                try {
                    //set up sql connection
                    Class.forName("com.mysql.jdbc.Driver");
                    Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
                    Statement st = conn.createStatement();
                    //get the result of execute statement, find username in database
                    ResultSet rs = st.executeQuery("SELECT * FROM customers WHERE username='" + username + "'");
                    //update progress dialog
                    publishProgress((int) (1));
                    //if username found in database, do things
                    while (rs.next()) {
                        userFound = true;
                        //test if recorded password matches provided password
                        if (rs.getString("password").equals(password)) {
                            loginResult = "Login Success";
                        } else {
                            loginResult = "Login Failed";
                        }
                        publishProgress((int) (2));
                    }
                    //if username not found in database
                    if (!userFound) {
                        loginResult = "User Not Found";
                        publishProgress((int) (2));
                    }
                    //close connection
                    conn.close();
                } catch (ClassNotFoundException | SQLException ex) {
                    //catch exceptions
                    ex.printStackTrace();
                }
                //change userFound back to false
                userFound = false;
            }
            return null;
        }

        //executes during doInBackground() executes
        @Override
        protected void onProgressUpdate (Integer... progress){
            super.onProgressUpdate(progress);
            //initiate the progress to 0
            progressDialog.setProgress(progress[0]);
        }

        //executes after doInBackground() finishes
        @Override
        protected void onPostExecute (String result){
            //pass results to ForgetPasswordActivity class through interface
            delegate.fetchAsyncResponse(loginResult);
            //close the progress dialog
            progressDialog.dismiss();
            //different scenarios
            //pop up dialogs based on result received from doInBackground()
            if(loginResult.equals("Login Failed")){
                new AlertDialog.Builder(ctx).setTitle("ERROR: Login Failed").setMessage("Password does not match Username").setCancelable(true).show();
            }else if(loginResult.equals("User Not Found")){
                new AlertDialog.Builder(ctx).setTitle("ERROR: Login Failed").setMessage("Username not Found in Database").setCancelable(true).show();
            }else if(loginResult.equals("Inadequate Username")){
                new AlertDialog.Builder(ctx).setTitle("ERROR: Login Failed").setMessage("Inadequate Username Length").setCancelable(true).show();
            }else if(loginResult.equals("Inadequate Password")){
                new AlertDialog.Builder(ctx).setTitle("ERROR: Login Failed").setMessage("Inadequate Password Length").setCancelable(true).show();
            }else if(loginResult.equals("Login Success")){
                //start the main activity after successful login
                startActivity(new Intent(LoginActivity.this, MainActivity.class));
            }
        }
    }
}
