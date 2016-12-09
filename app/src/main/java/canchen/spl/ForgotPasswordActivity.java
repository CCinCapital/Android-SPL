/* Author: Can Chen 100778989
*  ForgotPasswordActivity allows user to reset their password by providing registered
*  username, and email.
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
import android.os.Handler;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

//class must implement interface of Asyncresponse to retrive result from asyncactivity
//class must implement activity in order to display something on screen
public class ForgotPasswordActivity extends Activity implements AsyncResponse{

    //Declaration of Buttons and Text fields
    TextView tvCancel;
    EditText etUsername;
    EditText etEMail;
    EditText etNewPassword;
    Button bResetPassword;

    //variables that used to pass information within class
    String sUsername;
    String sEMail;
    String sNewPassword;

    //variable that used to pass information between class
    public String passwordResetResult = "Pending";

    //Method automatically executes whenever This activity is created
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //set the XML Layout of this View
        setContentView(R.layout.activity_forgotpassword);
        //link layout fields with its function
        etUsername = (EditText)findViewById(R.id.etUsername);
        etEMail = (EditText)findViewById(R.id.etEMail);
        etNewPassword = (EditText)findViewById(R.id.etNewPassword);
        bResetPassword = (Button)findViewById(R.id.bResetPassword);
        tvCancel = (TextView)findViewById(R.id.tvCancel);


        bResetPassword.setOnClickListener(new OnClickListener() {
            @Override
            //executes when this button is clicked
            public void onClick(View v) {
                //call the function
                forgotPassword(v);
            }
        });

        tvCancel.setOnClickListener(new OnClickListener() {
            @Override
            //executes when this button is clicked
            public void onClick(View v) {
                //call the function
                startActivity(new Intent(v.getContext(), LoginActivity.class));
            }
        });
    }

    //get the user typed information and call the backgroundForgetPassword()
    public void forgotPassword(View v){
        //read the typed information and convert it to string
        sUsername = etUsername.getText().toString();
        sEMail = etEMail.getText().toString();
        sNewPassword = etNewPassword.getText().toString();
        //initiate the BackgroundForgotPassword class
        BackgroundForgotPassword backgroundForgotPassword = new BackgroundForgotPassword(this);
        //setup the interface to retrieve result
        backgroundForgotPassword.delegate = this;
        //execute the class
        backgroundForgotPassword.execute(sUsername, sEMail, sNewPassword);
    }

    //get the response from it's async classes
    public void fetchAsyncResponse(String output){
        passwordResetResult = output;
    }

    //inner class that changes user's password in background
    public class BackgroundForgotPassword extends AsyncTask<String, Integer, String> {
        //variables that keeps the login information that are required when connecting to server
        private String dbURL = "jdbc:mysql://192.168.0.100:3306/parkinglots";
        private String dbUsername = "root";
        private String dbPassword = "";
        //interface that passes result to parent class
        public AsyncResponse delegate = null;
        //context of this class
        Context ctx;
        BackgroundForgotPassword(Context ctx){
            this.ctx = ctx;
        }
        //initiate the progress dialog
        ProgressDialog progressDialog;

        //executes and finishes before doInBackground executes
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            //create a new progress dialog
            //set its parameters
            progressDialog = new ProgressDialog(ctx);
            progressDialog.setTitle("Password Change in Progress");
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
            //username is the first parameter of params, email the second, password third
            String username = params[0];
            String email = params[1];
            String password = params[2];
            //different scenarios
            //username must longer than 2 characters
            //email must have adequate format of xxx@xxx.xxx
            //password also has minimum length requirement
            if(username.length() < 3){
                passwordResetResult = "Inadequate Username";
            }else if(!isValidEmail(email)){
                passwordResetResult = "Inadequate Email";
            }else if(password.length() < 5){
                passwordResetResult = "Inadequate Password";
            }else {
                try {
                    //set up sql connection
                    Class.forName("com.mysql.jdbc.Driver");
                    Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
                    Statement st = conn.createStatement();
                    //get the result of execute statement
                    ResultSet rs = st.executeQuery("SELECT * FROM customers WHERE username='" + username + "'");
                    //update progress dialog
                    publishProgress((int) (1));
                    //if no result found by executing above codes
                    while (!rs.next()) {
                        passwordResetResult = "Username cannot be found in Database";
                        publishProgress((int) (2));
                        return null;
                    }
                    //executes if user is in database
                    //try update user's password
                    int r = st.executeUpdate("UPDATE customers SET password='" + password + "' WHERE username='" + username + "' and email='" + email + "'");
                    //r >= 1 if query ran successfully, r = 0 if query failed to ran
                    if (r > 0) {
                        passwordResetResult = "Password Change Success";
                    }else{
                        passwordResetResult = "Username and Email does not match record";
                    }
                    publishProgress((int) (2));
                    //close connection to database
                    conn.close();
                } catch (ClassNotFoundException | SQLException ex) {
                    //catch exceptions
                    ex.printStackTrace();
                }
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
            delegate.fetchAsyncResponse(passwordResetResult);
            //close the progress dialog
            progressDialog.dismiss();
            //different scenarios
            //pop up dialogs based on result received from doInBackground()
            if(passwordResetResult.equals("Inadequate Username")){
                new AlertDialog.Builder(ctx).setTitle("ERROR: Password Change Failed").setMessage("Inadequate Username Length").setCancelable(true).show();
            }else if(passwordResetResult.equals("Inadequate Password")){
                new AlertDialog.Builder(ctx).setTitle("ERROR: Password Change Failed").setMessage("Inadequate Password Length").setCancelable(true).show();
            }else if(passwordResetResult.equals("Inadequate Email")) {
                new AlertDialog.Builder(ctx).setTitle("ERROR: Password Change Failed").setMessage("Inadequate Email Length").setCancelable(true).show();
            }else if(passwordResetResult.equals("Username cannot be found in Database")){
                new AlertDialog.Builder(ctx).setTitle("ERROR: Password Change Failed").setMessage("Username cannot be found in Database").setCancelable(true).show();
            }else if(passwordResetResult.equals("Username and Email does not match record")){
                new AlertDialog.Builder(ctx).setTitle("ERROR: Password Change Failed").setMessage("Username and Email does not match record").setCancelable(true).show();
            }else if(passwordResetResult.equals("Password Change Success")){
                new AlertDialog.Builder(ctx).setTitle("Password Change Success").setMessage("Password now successfully updated").setCancelable(true).show();
                //close this activity and start the login activity after 2s
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(ForgotPasswordActivity.this, LoginActivity.class));
                        finish();
                    }
                }, 2000);
            }else {
                new AlertDialog.Builder(ctx).setTitle("ERROR").setMessage("Unexpected condition").setCancelable(true).show();
            }
        }

        //test if the character sequence is in email format
        private boolean isValidEmail(CharSequence cs){
            if(cs == null){
                return false;
            }else{
                return Patterns.EMAIL_ADDRESS.matcher(cs).matches();
            }
        }
    }
}
