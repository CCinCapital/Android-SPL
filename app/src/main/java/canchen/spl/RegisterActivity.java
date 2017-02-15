/* Author: Can Chen 100778989
*  RegisterActivity allows user to register there service
*  User registers username, password, and email
*/

package canchen.spl;

//Widgets and Libraries required for this class
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
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
import java.util.regex.Pattern;

//class must implement interface of Asyncresponse to retrieve result from asyncactivity
//class must implement activity in order to display something on screen
public class RegisterActivity extends Activity implements AsyncResponse{

    //Declaration of Buttons and Text fields
    TextView tvCancel;
    EditText etUsername;
    EditText etPassword;
    EditText etEMail;
    Button bRegister;

    //variables that used to pass information within class
    String sUsername;
    String sPassword;
    String sEMail;

    //variable that used to pass information between class
    public String registerResult = "Pending";

    //Method automatically executes whenever This activity is created
    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //set the XML Layout of this View
        setContentView(R.layout.activity_register);
        //link layout fields with its function
        etUsername = (EditText)findViewById(R.id.etUsername);
        etPassword = (EditText)findViewById(R.id.etPassword);
        etEMail = (EditText)findViewById(R.id.etEMail);
        tvCancel = (TextView)findViewById(R.id.tvCancel);

        bRegister = (Button)findViewById(R.id.bRegister);
        bRegister.setOnClickListener(new OnClickListener() {
            @Override
            //executes when this button is clicked
            public void onClick(View v) {
                //call the function
                register();
            }
        });
        tvCancel.setOnClickListener(new OnClickListener() {
            @Override
            //executes when this button is clicked
            public void onClick(View v) {
                //go back to login activity when user taps this field
                startActivity(new Intent(v.getContext(), LoginActivity.class));
            }
        });
    }

    //get the user typed information and call the BackgroundRegister()
    public void register(){
        //read the typed information and convert it to string
        sUsername = etUsername.getText().toString();
        sPassword = etPassword.getText().toString();
        sEMail = etEMail.getText().toString();
        //initiate the BackgroundRegister class
        BackgroundRegister backgroundRegister = new BackgroundRegister(this);
        //setup the interface to retrieve result
        backgroundRegister.delegate = this;
        //execute the class
        backgroundRegister.execute(sUsername, sPassword, sEMail);
    }

    //get the response from it's async classes
    public void fetchAsyncResponse(String output){
        registerResult = output;
    }

    //inner class that register user into the service
    public class BackgroundRegister extends AsyncTask<String, Integer, String> {
        //variables that keeps the login information that are required when connecting to server
        private String dbURL = "jdbc:mysql://localhost.000webhost.com/id522773_sps_test";
        private String dbUsername = "id522773_sps_test";
        private String dbPassword = "d_@Afr7spuDR";
        //true if username present in database, false as opposite
        boolean userFound = false;
        //interface that passes result to parent class
        public AsyncResponse delegate = null;
        //context of this class
        Context ctx;
        BackgroundRegister(Context ctx){this.ctx = ctx;}
        //initiate the progress dialog
        ProgressDialog progressDialog;

        //executes and finishes before doInBackground executes
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
            //create a new progress dialog
            //set its parameters
            progressDialog = new ProgressDialog(ctx);
            progressDialog.setTitle("Registration in Progress");
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
        protected String doInBackground (String... params){
            //username is the first parameter of params, password the second, email third
            String username = params[0];
            String password = params[1];
            String email = params[2];
            //different scenarios
            //username must longer than 2 characters
            //email must have adequate format of xxx@xxx.xxx
            //password also has minimum length requirement
            if(username.length() < 3){
                registerResult = "Inadequate Username";
            }else if(password.length() < 5){
                registerResult = "Inadequate Password";
            }else if(!isValidEmail(email)){
                registerResult = "Inadequate Email";
            }else {
                try {
                    //set up sql connection
                    Class.forName("com.mysql.jdbc.Driver");
                    Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
                    Statement st = conn.createStatement();
                    //get the result of execute statement, find username in database
                    ResultSet rs = st.executeQuery("SELECT * FROM customers WHERE username='" + username + "'");
                    //update progress dialog
                    publishProgress((int) (1));
                    //if username found in database, end this function
                    while (rs.next()) {
                        userFound = true;
                        registerResult = "Username Already Registered";
                        publishProgress((int) (2));
                        return null;
                    }
                    //get the result of execute statement, find email in database
                    rs = st.executeQuery("SELECT * FROM customers WHERE email='" + email + "'");
                    //if email found in database, end this function
                    while (rs.next()) {
                        userFound = true;
                        registerResult = "Email Already Registered";
                        publishProgress((int) (2));
                        return  null;
                    }
                    //if no result found by executing above codes
                    if (!userFound) {
                        //initiate parameters
                        int parkinglot = 0;
                        int parkingid = 0;
                        double balance = 0;
                        //get the result of execute statement, register user into service
                        int r = st.executeUpdate("insert into customers (username, password, email, parkinglot, parkingid, balance) values ('" + username + "','" + password + "','" + email + "','" + parkinglot + "','" + parkingid + "','" + balance + "')" );
                        //r>=1 if statement executed successfully
                        if (r > 0) {
                            registerResult = "Registration Success";
                            //update progress dialog
                            publishProgress((int) (2));
                        }
                    }
                    //close connection
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
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            //initiate the progress to 0
            progressDialog.setProgress(progress[0]);
        }

        //executes after doInBackground() finishes
        @Override
        protected void onPostExecute (String  result){
            //set userFound back to false
            userFound = false;
            //pass results to ForgetPasswordActivity class through interface
            delegate.fetchAsyncResponse(registerResult);
            //close the progress dialog
            progressDialog.dismiss();
            //different scenarios
            //pop up dialogs based on result received from doInBackground()
            if(registerResult.equals("Registration Success")){
                new AlertDialog.Builder(ctx).setTitle("Registration Success").setMessage("Welcome to Smart Parking Services").setCancelable(true).show();
                //close this activity and start the login activity after 2s
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                        finish();
                    }
                }, 2000);
            }else if(registerResult.equals("Inadequate Username")){
                new AlertDialog.Builder(ctx).setTitle("ERROR: Registration Failed").setMessage("Inadequate Username Length").setCancelable(true).show();
            }else if(registerResult.equals("Inadequate Password")){
                new AlertDialog.Builder(ctx).setTitle("ERROR: Registration Failed").setMessage("Inadequate Password Length").setCancelable(true).show();
            }else if(registerResult.equals("Inadequate Email")){
                new AlertDialog.Builder(ctx).setTitle("ERROR: Registration Failed").setMessage("Email Format Incorrect").setCancelable(true).show();
            }else if(registerResult.equals("Username Already Registered")){
                new AlertDialog.Builder(ctx).setTitle("ERROR: Registration Failed").setMessage("Username is taken").setCancelable(true).show();
            }else if(registerResult.equals("Email Already Registered")){
                new AlertDialog.Builder(ctx).setTitle("ERROR: Registration Failed").setMessage("Email is taken").setCancelable(true).show();
            }else{
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
