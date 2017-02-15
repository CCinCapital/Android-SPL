/* Author: Can Chen 100778989
*  MainActivity allows user to make reservation, see their tickets,
*  Navigate, Logout, see tick information, and manage their account
*/

package canchen.spl;

//Widgets and Libraries required for this class
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.Date;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

//class must implement interface of Asyncresponse to retrive result from asyncactivity
//class must implement activity in order to display something on screen
public class MainActivity extends Activity implements AsyncResponse{
    //Initiate imageLoader
    private ImageLoader imageLoader;

    //Partial URL to Public API to generate QR Code
    private final String BASE_QR_URL = "https://api.qrserver.com/v1/create-qr-code/?size=600x600&data=";
    private String fullUrl = BASE_QR_URL;

    //Declaration of useful variables
    public String ticketInformation = "TEST";
    private double balance, topup;
    private boolean navigate = false;

    //Local Database of stored User
    UserLocalStore userLocalStore;

    //Declaration of Buttons and Text fields
    Button bReserve, bNavigate;
    ImageView ivUserIcon,  qrImage;
    TextView qrText, tvBalance;
    private String tvTicket, tvTicketDetail, tvGarage, tvGarageDetail, geoLocation;

    //Method automatically executes whenever This activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set the XML Layout of this View
        setContentView(R.layout.activity_main);

        //Initiate the imageLoader
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        imageLoader = ImageLoader.getInstance();
        imageLoader.init(config);

        //Create local database to store user information
        userLocalStore = new UserLocalStore(this);

        //link layout fields with its function
        qrImage = (ImageView) findViewById(R.id.qrImage);
        qrText = (TextView) findViewById(R.id.qrText);

        bReserve = (Button) findViewById(R.id.bReserve);
        bReserve.setOnClickListener(new OnClickListener() {
            @Override
            //executes when this button is clicked
            public void onClick(View v) {
                //different scenarios
                if(bReserve.getText().equals("New Reservation")){
                    //show the reserve screen...with main screen hidden
                    startActivity(new Intent(v.getContext(), ReserveActivity.class));
                }else if(bReserve.getText().equals("Ticket Details")){
                    //show ticket details...with main screen as background
                    //create a dialog
                    final Dialog dialog = new Dialog(MainActivity.this);
                    dialog.setContentView(R.layout.dialog_ticket);
                    dialog.setTitle("Ticket Details");
                    dialog.show();

                    //set what to display
                    TextView ticket = (TextView)dialog.findViewById(R.id.tvTicket);
                    ticket.setText(tvTicket);
                    TextView ticketDetail = (TextView)dialog.findViewById(R.id.tvTicketDetail);
                    ticketDetail.setText(tvTicketDetail);
                    TextView garage = (TextView)dialog.findViewById(R.id.tvGarage);
                    garage.setText(tvGarage);
                    TextView garageDetail = (TextView)dialog.findViewById(R.id.tvGarageDetail);
                    garageDetail.setText(tvGarageDetail);

                    //button to cancel the dialog
                    Button bDismiss = (Button) dialog.findViewById(R.id.bDismiss);
                    bDismiss.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {dialog.cancel();
                        }
                    });
                }
            }
        });

        bNavigate = (Button) findViewById(R.id.bNavigate);
        bNavigate.setOnClickListener(new OnClickListener() {
            @Override
            //executes when this button is clicked
            public void onClick(View v) {
                //different scenarios
                if(navigate) {
                    //pass preset geolocation to other map app, and start the map app
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(geoLocation));
                    Intent chooser = Intent.createChooser(intent, "Launch Maps");
                    startActivity(chooser);
                }else {
                    //warning if no reservation found
                    new AlertDialog.Builder(MainActivity.this).setTitle("ERROR").setMessage("Navigation only available with active reservation").setCancelable(true).show();
                }
            }
        });

        ivUserIcon = (ImageView)findViewById(R.id.ivUserIcon);
        ivUserIcon.setOnClickListener(new OnClickListener() {
            @Override
            //executes when this button is clicked
            public void onClick(View v) {
                //show user management window...with main screen as background
                //create a dialog
                final Dialog dialog = new Dialog(MainActivity.this);
                //assign its layout
                dialog.setContentView(R.layout.dialog_user);
                //set location where the dialog will be shown on screen
                WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
                params.y = 50; params.x = 00;
                params.gravity = Gravity.TOP | Gravity.LEFT;
                dialog.getWindow().setAttributes(params);
                dialog.setTitle("User Account");
                dialog.show();
                //link the fields to its functions
                tvBalance = (TextView) dialog.findViewById(R.id.tvBalance);
                //method to show the account balance of user
                getBalance();

                Button bTopUp = (Button)dialog.findViewById(R.id.bTopUp);
                bTopUp.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //show topup window...with main screen as background
                        //create a dialog
                        final Dialog dialog = new Dialog(MainActivity.this);
                        dialog.setContentView(R.layout.dialog_topup);
                        dialog.setTitle("Top Up My Account");
                        dialog.show();
                        //link the fields to its functions
                        Button bTwenty = (Button)dialog.findViewById(R.id.bTwenty);
                        bTwenty.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //set the topup amount, topup the account, then close the topup dialog
                                topup = 20;
                                topUp();
                                dialog.cancel();
                            }
                        });

                        Button bFifty = (Button)dialog.findViewById(R.id.bFifty);
                        bFifty.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //set the topup amount, topup the account, then close the topup dialog
                                topup = 50;
                                topUp();
                                dialog.cancel();
                            }
                        });

                        Button bOneHundred = (Button)dialog.findViewById(R.id.bOneHundred);
                        bOneHundred.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //set the topup amount, topup the account, then close the topup dialog
                                topup = 100;
                                topUp();
                                dialog.cancel();
                            }
                        });

                        Button bCancel = (Button)dialog.findViewById(R.id.bCancelTopUp);
                        bCancel.setOnClickListener(new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                //close the topup dialog
                                dialog.cancel();
                            }
                        });
                    }
                });


                Button bProfile = (Button) dialog.findViewById(R.id.bProfile);
                bProfile.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AlertDialog.Builder(MainActivity.this).setTitle("SORRY").setMessage("NOT IMPLEMENTED YET").setCancelable(true).show();
                    }
                });

                Button bLogout = (Button) dialog.findViewById(R.id.bLogout);
                bLogout.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //clear the local data of current user, and logout the user by showing the login window
                        userLocalStore.clearUserData();
                        userLocalStore.setUserLoggedIn(false);
                        dialog.cancel();
                        startActivity(new Intent(v.getContext(), LoginActivity.class));
                    }
                });
            }
        });

        //Display the User's current ticket if device is connected to internet
        if (isNetworkConnected()) {
            try {
                //Fetch user information to BackgroundGetTicketInformation class
                getTicketInformation();
                //Wait until the onPostExecute() has finished
                while(ticketInformation.equals("TEST")){
                    try { Thread.sleep(100); }
                    catch (InterruptedException e) { e.printStackTrace(); }
                }
                //different scenarios
                if(ticketInformation.equals("ERROR") | ticketInformation.equals("No Reservation Found")){
                    //Display the ticket information in allocated location
                    qrText.setText(ticketInformation);
                    //change the function of reserve button
                    bReserve.setText("New Reservation");
                }else {
                    //Display the ticket information in allocated location
                    qrText.setText(ticketInformation);
                    //encode the Ticket information
                    fullUrl = fullUrl + URLEncoder.encode(ticketInformation, "UTF-8");
                    // Display the QR Code.
                    // QR Code is generated by calling the fullUrl, the format of QR Code is PNG.
                    // QR Code is placed in the location of qrImage
                    imageLoader.displayImage(fullUrl, qrImage);
                    //change the function of reserve button
                    bReserve.setText("Ticket Details");
                }
            } catch (UnsupportedEncodingException e) {
                //catch any unexpected encoding exception
                e.printStackTrace();
            }
        }
    }

    //onStart() executes after onCreate()
    @Override
    protected void onStart(){
        super.onStart();
        //welcome user if user successfully login or switch back from background
        if (authenticate() == true) {
            welcomeUserBack();
        }
    }

    //display welcome message
    private void  welcomeUserBack(){
        //get the information of current logged in user
        User user = userLocalStore.getLoggedInUser();
        //display a toast message
        Toast.makeText(MainActivity.this, "Welcome Back " + user.username,Toast.LENGTH_SHORT).show();
    }

    //authenticate user
    private boolean authenticate(){
        //if no user logged in, start login window
        if (userLocalStore.getLoggedInUser() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            return false;
        }
        return true;
    }

    //test network connectivity
    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm.getActiveNetworkInfo() != null);
    }

    //get the user's ticket information
    private void getTicketInformation (){
        User user = userLocalStore.getLoggedInUser();
        String username = user.username;
        //initiate the backgroundGetTickInformation class
        BackgroundGetTicketInformation backgroundGetTicketInformation = new BackgroundGetTicketInformation(this);
        //setup the interface to retrieve result
        backgroundGetTicketInformation.delegate = this;
        //execute the class
        backgroundGetTicketInformation.execute(username);
    }

    //get the user's account balance
    private void getBalance(){
        User user = userLocalStore.getLoggedInUser();
        String username = user.username;
        //initiate the backgroundGetBalance class
        BackgroundGetBalance backgroundGetBalance = new BackgroundGetBalance(this);
        //setup the interface to retrieve result
        backgroundGetBalance.delegate = this;
        //execute the class
        backgroundGetBalance.execute(username);
    }

    //topup the user's wallet
    private void topUp(){
        User user = userLocalStore.getLoggedInUser();
        String username = user.username;
        //initiate the backgroundYopUp class
        BackgroundTopUp backgroundTopUp = new BackgroundTopUp(this);
        //set the interface to retrieve result
        backgroundTopUp.delegate = this;
        //execute the class
        backgroundTopUp.execute(username);
    }

    //get the response from it's async classes
    public void fetchAsyncResponse(String output){
        ticketInformation = output;
    }

    //inner class that gets ticket information in background
    private class BackgroundGetTicketInformation extends AsyncTask<String, Void, String> {
        //variables that keeps the login information that are required when connecting to server
        private String dbURL = "jdbc:mysql://localhost.000webhost.com/id522773_sps_test";
        private String dbUsername = "id522773_sps_test";
        private String dbPassword = "d_@Afr7spuDR";
        //interface that passes result to parent class
        public AsyncResponse delegate = null;
        //context of this class
        Context ctx;
        BackgroundGetTicketInformation(Context ctx){
            this.ctx = ctx;
        }

        //executes and finishes before doInBackground executes
        @Override
        protected void onPreExecute(){
            super.onPreExecute();
        }

        //where to put all the background jobs
        @Override
        protected String doInBackground(String... params) {
            //username is the first parameter of params
            String username = params[0];
            //all connection codes
            try {
                //set up sql connection
                Class.forName("com.mysql.jdbc.Driver");
                Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
                Statement st = conn.createStatement();
                //reads database 'customers', all results matches 'username'
                ResultSet rs = st.executeQuery("Select * from customers WHERE username='" + username + "'");
                //find variables within resultset
                while(rs.next()){
                    //different scenarios
                    //if username found in database, find user's reservation information
                    if(rs.getString("username").equals(username)) {
                        Date date = null;
                        Time time = null;
                        //get user's parkingID and lotID
                        int lotID = rs.getInt("ParkingLot");
                        int parkingID = rs.getInt("ParkingID");
                        //since current available parking lot are 1 and 2, any other ID are discarded
                        if(lotID < 1| lotID > 2){
                            ticketInformation = "No Reservation Found";
                            conn.close();
                            return null;
                        }
                        //if the user has acceptable lotID, then find user's parking lot information
                        ResultSet rsLot = st.executeQuery("Select * from lot" + lotID + " where parkingid='" + parkingID + "'");
                        //read the start date and time of reservation
                        while (rsLot.next()){
                            date =  rsLot.getDate("Date");
                            time = rsLot.getTime("Time");
                        }
                        //encode ticket's detailed information
                        tvTicket = "Ticket Active.";
                        tvTicketDetail = "Start Time: " + time.toString() + "\nStart Date: " + date.toString();
                        tvGarage = "Garage #" + Integer.toString(lotID);
                        //set the geolocation of parking garages
                        if(lotID == 1){
                            tvGarageDetail = "Location: \nSPS Parking Lot 1, Carleton University, Ottawa";
                            geoLocation = "geo:45.383390,-75.697943";
                        }else{
                            tvGarageDetail = "Location: \nSPS Parking Lot 2, Carleton University, Ottawa";
                            geoLocation = "geo:45.384786,-75.697314";
                        }
                        //enable the navigation button
                        navigate = true;
                        //encode the ticket information that will be used to generate QR Code
                        ticketInformation = username + "_" + Integer.toString(lotID) + "_" + Integer.toString(parkingID) + "_" + date.toString() + "_" + time.toString();
                    }else{
                        //no user found
                        ticketInformation = "ERROR";
                    }
                    //close connection to database
                    conn.close();
                    return null;
                }
                //code should never reach here, except of unexpected error
                conn.close();
            } catch (ClassNotFoundException | SQLException ex) {
                //catch the exceptions
                ex.printStackTrace();
            }
            return null;
        }

        //in the progress of doInBackground(), this method provides updates of progress
        @Override
        protected void onProgressUpdate (Void... values) {
            super.onProgressUpdate(values);
        }

        //executes after doInBackground() finishes
        @Override
        protected void onPostExecute (String result){
            //pass result to parent class
            delegate.fetchAsyncResponse(ticketInformation);
            //display the toast message on screen
            Toast.makeText(MainActivity.this, "Reservation Updated Successfully",Toast.LENGTH_SHORT).show();
        }
    }

    //inner class that reads user's account balance
    public class BackgroundGetBalance extends AsyncTask<String, Void, String> {
        //variables that keeps the login information that are required when connecting to server
        private String dbURL = "jdbc:mysql://192.168.0.100:3306/parkinglots";
        private String dbUsername = "root";
        private String dbPassword = "";
        //interface that passes result to parent class
        public AsyncResponse delegate = null;
        //context of this class
        Context ctx;
        BackgroundGetBalance(Context ctx) {
            this.ctx = ctx;
        }

        //executes and finishes before doInBackground executes
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        //where to put all the background jobs
        @Override
        protected String doInBackground(String... params) {
            //username is the first parameter of params
            String username = params[0];
            try {
                //set up sql connection
                Class.forName("com.mysql.jdbc.Driver");
                Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
                Statement st = conn.createStatement();
                //reads database 'customers', all results matches 'username'
                ResultSet rs = st.executeQuery("SELECT * FROM customers WHERE username = '" + username + "'");
                //find variables within resultset
                while (rs.next()) {
                    //get the balance of user
                    balance = rs.getDouble("Balance");
                }
                //close connection
                conn.close();
            } catch (ClassNotFoundException | SQLException ex) {
                //catch exceptions
                ex.printStackTrace();
            }
            return null;
        }

        //executes during doInBackground() executes
        @Override
        protected void onProgressUpdate(Void... progress) {
            super.onProgressUpdate();
        }

        //executes after doInBackground() finishes
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //show the balance in textview tvBalance
            tvBalance.setText("Balance Remaining: $" + balance + "0");
        }
    }

    //inner class that topup user's wallet
    public class BackgroundTopUp extends AsyncTask<String, Integer, String> {
        //variables that keeps the login information that are required when connecting to server
        private String dbURL = "jdbc:mysql://192.168.0.100:3306/parkinglots";
        private String dbUsername = "root";
        private String dbPassword = "";
        //interface that passes result to parent class
        public AsyncResponse delegate = null;
        //context of this class
        Context ctx;
        BackgroundTopUp(Context ctx) {
            this.ctx = ctx;
        }
        //hold the result within class
        String topUpResult = "Pending";
        //initiate the progress dialog
        ProgressDialog progressDialog;

        //executes and finishes before doInBackground executes
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //create a new progress dialog
            //set its parameters
            progressDialog = new ProgressDialog(ctx);
            progressDialog.setTitle("Topping Up");
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
            //username is the first parameter of params
            String username = params[0];
            try {
                //set up sql connection
                Class.forName("com.mysql.jdbc.Driver");
                Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
                Statement st = conn.createStatement();
                //update progress bar
                publishProgress((int) (1));
                //get the result of execute statement
                int rTopUp = st.executeUpdate("UPDATE customers SET balance=balance+'" + topup + "' WHERE username='" + username + "'");
                //result will greater than 1 if statement executed successfully
                if(rTopUp > 0){
                    //change topupresult to success
                    topUpResult = "SUCCESS";
                    //update progress bar
                    publishProgress((int) (2));
                    //close connection
                    conn.close();
                    return null;
                }
                //close connection
                conn.close();
            } catch (ClassNotFoundException | SQLException ex) {
                //catch exceptions
                ex.printStackTrace();
            }
            //update progress bar
            publishProgress((int) (2));
            topUpResult = "ERROR";
            return null;
        }

        //executes during doInBackground() executes
        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate();
            //initiate the progress to 0
            progressDialog.setProgress(progress[0]);
        }

        //executes after doInBackground() finishes
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //different scenarios
            if(topUpResult.equals("SUCCESS")){
                //tells user topup is successful by pop up a un-cancellable dialog
                new AlertDialog.Builder(ctx).setTitle("Success").setMessage("You have added $" + topup + " to your Account").setCancelable(false).show();
                //reload main screen after 1 second
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                        startActivity(new Intent(MainActivity.this, MainActivity.class));
                    }
                }, 1000);
            }else{
                //tells user the action failed
                new AlertDialog.Builder(ctx).setTitle("ERROR").setMessage("Top Up Failed").setCancelable(true).show();
            }
            //reset the topup value back to 0
            topup = 0;
        }
    }
}

