/* Author: Can Chen 100778989
*  ReserveActivity reserves parking lot
*  user picks start date, time, garage
*/

package canchen.spl;

//Widgets and Libraries required for this class
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;

//class must implement interface of Asyncresponse to retrieve result from asyncactivity
//class must implement activity in order to display something on screen
public class ReserveActivity extends Activity implements AsyncResponse{

    //get the calendar of current date
    private Calendar c;
    //define the parameters related to time
    private int hourOfDay, minute, year, monthOfYear, dayOfMonth;
    //Declaration of Buttons and Text fields
    private Button buttonGarage, buttonTime, buttonDate, buttonReserve;
    private TextView tvCancel;
    Button bGarageOne;
    Button bGarageTwo;

    //variable that used to pass information within class
    private String sUsername = "";
    private String sDate = "";
    private String sTime = "";
    private int parkingLot = 0;
    private int parkingID = 0;

    //Local Database of stored User
    UserLocalStore userLocalStore;

    //variable that used to pass information between class
    public String reserveResult = "Pending";

    //Method automatically executes whenever This activity is created
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set the XML Layout of this View
        setContentView(R.layout.activity_reserve);
        //link layout fields with its function
        buttonGarage = (Button)findViewById(R.id.buttonGarage);
        buttonDate = (Button)findViewById(R.id.buttonDate);
        buttonTime = (Button)findViewById(R.id.buttonTime);
        buttonReserve = (Button)findViewById(R.id.buttonReserve);

        //get current calendar, read current time
        c = Calendar.getInstance();
        hourOfDay = c.get(Calendar.HOUR_OF_DAY);
        minute = c.get(Calendar.MINUTE);
        year = c.get(Calendar.YEAR);
        monthOfYear = c.get(Calendar.MONTH);
        dayOfMonth = c.get(Calendar.DAY_OF_MONTH);

        //attach on click listeners to fields
        buttonGarage.setOnClickListener(new OnClickListener() {
            @Override
            //executes when this button is clicked
            public void onClick(View v) {
                //create new garage selector dialog
                //set the XML Layout of this dialog
                final Dialog dialog = new Dialog(ReserveActivity.this);
                dialog.setTitle("Garage Selector");
                dialog.setContentView(R.layout.dialog_garage);
                dialog.show();

                //link layout fields with its function
                bGarageOne = (Button)dialog.findViewById(R.id.bGarageOne);
                bGarageTwo = (Button)dialog.findViewById(R.id.bGarageTwo);
                //since only 2 garage are available, others are disabled
               // Button bGarageThree = (Button)dialog.findViewById(R.id.bGarageThree);
               // Button bGarageFour = (Button)dialog.findViewById(R.id.bGarageFour);
                TextView cancel = (TextView)dialog.findViewById(R.id.tvCancel);

                //attach on click listeners to fields
                bGarageOne.setOnClickListener(new OnClickListener() {
                    @Override
                    //executes when this button is clicked
                    public void onClick(View v) {
                        //change the text on button, set the selected parkinglot ID
                        //then close this dialog
                        buttonGarage.setText("Garage One");
                        parkingLot = 1;
                        dialog.cancel();
                    }
                });
                bGarageTwo.setOnClickListener(new OnClickListener() {
                    @Override
                    //executes when this button is clicked
                    public void onClick(View v) {
                        //change the text on button, set the selected parkinglot ID
                        //then close this dialog
                        buttonGarage.setText("Garage Two");
                        parkingLot = 2;
                        dialog.cancel();
                    }
                });

                /*
                bGarageThree.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        buttonGarage.setText("Garage Three");
                        dialog.cancel();
                    }
                });
                bGarageFour.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        buttonGarage.setText("Garage Four");
                        dialog.cancel();
                    }
                });
                */

                cancel.setOnClickListener(new OnClickListener() {
                    @Override
                    //executes when this button is clicked
                    public void onClick(View v) {
                        //close this dialog
                        dialog.cancel();
                    }
                });

                //update the available lots information
                updateLotStatus();
            }
        });
        buttonDate.setOnClickListener(new OnClickListener() {
            @Override
            //executes when this button is clicked
            public void onClick(View v) {
                //show a date picker dialog that allows user to pick date
                new DatePickerDialog(ReserveActivity.this, dateListener, year, monthOfYear, dayOfMonth).show();
            }
        });
        buttonTime.setOnClickListener(new OnClickListener() {
            @Override
            //executes when this button is clicked
            public void onClick(View v) {
                //show a time picker dialog that allows user to pick time
                new TimePickerDialog(ReserveActivity.this, timeListener, hourOfDay, minute, false).show();
            }
        });
        buttonReserve.setOnClickListener(new OnClickListener() {
            @Override
            //executes when this button is clicked
            public void onClick(View v) {
                //call the function
                reserve(v);
            }
        });

        tvCancel = (TextView)findViewById(R.id.tvCancel);
        tvCancel.setOnClickListener(new OnClickListener() {
            @Override
            //executes when this button is clicked
            public void onClick(View v) {
                //return to main activity
                startActivity(new Intent(v.getContext(), MainActivity.class));
            }
        });

        //create new local storage
        userLocalStore = new UserLocalStore(this);
    }

    //reserve a parking lot for the user by calling BackgroundReserve()
    private void reserve(View v){
        //get current user's information
        User user = userLocalStore.getLoggedInUser();
        //read current user's username
        sUsername = user.username;
        //convert reserve information to string
        sDate = buttonDate.getText().toString();
        sTime = buttonTime.getText().toString();
        //initiate the BackgroundReserve class
        BackgroundReserve backgroundReserve = new BackgroundReserve(this);
        //setup the interface to retrieve result
        backgroundReserve.delegate = this;
        //execute the class
        backgroundReserve.execute(sUsername, sDate, sTime);
    }

    //updates the parking lot information by calling BackgroundFetchInformation class
    public void updateLotStatus(){
        //initiate the BackgroundFetchInformation class
        BackgroundFetchInformation backgroundFetchInformation = new BackgroundFetchInformation(this);
        //setup the interface to retrieve result
        backgroundFetchInformation.delegate = this;
        //execute the class
        backgroundFetchInformation.execute();
    }

    //define what the listener do
    private DatePickerDialog.OnDateSetListener dateListener = new DatePickerDialog.OnDateSetListener(){
        @Override
        //when a date is chosen
        public void onDateSet(DatePicker view, int yr, int month, int day) {
            year = yr;
            monthOfYear = month;
            dayOfMonth = day;
            //change the button text corresponding to selected date
            buttonDate.setText(year+"-"+(monthOfYear+1)+"-"+dayOfMonth);
        }
    };

    //define what the listener do
    private TimePickerDialog.OnTimeSetListener timeListener = new TimePickerDialog.OnTimeSetListener(){
        @Override
        //when a time is chosen
        public void onTimeSet(TimePicker view, int h, int m) {
            hourOfDay = h;
            minute = m;
            //change the button text corresponding to selected time
            //add 0 to 0~9 minute, add 0 to 0~9 hour, otherwise it will display 9:3 rather than 09:03
            if(minute < 10 && hourOfDay >= 10){
                buttonTime.setText(hourOfDay + ":0" + minute);
            }else if(minute < 10 && hourOfDay < 10){
                buttonTime.setText("0" + hourOfDay + ":0" + minute);
            }else if(minute >= 10 && hourOfDay < 10){
                buttonTime.setText("0" + hourOfDay + ":" + minute);
            }else{
                buttonTime.setText(hourOfDay + ":" + minute);
            }
        }
    };

    //get the response from it's async classes
    public void fetchAsyncResponse(String output){
        reserveResult = output;
    }

    //inner class that reserves parking lot
    public class BackgroundReserve extends AsyncTask<String, Integer, String> {
        //variables that keeps the login information that are required when connecting to server
        private String dbURL = "jdbc:mysql://192.168.0.100:3306/parkinglots";
        private String dbUsername = "root";
        private String dbPassword = "";
        //true if available parking lot has been found
        boolean foundLot = false;
        //interface that passes result to parent class
        public AsyncResponse delegate = null;
        //context of this class
        Context ctx;
        BackgroundReserve(Context ctx){
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
            progressDialog.setTitle("Reservation in Progress");
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
            //username is the first parameter of params, date the second, time third
            String username = params[0];
            String date = params[1];
            String time = params[2];
            try {
                //set up sql connection
                Class.forName("com.mysql.jdbc.Driver");
                Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
                Statement st = conn.createStatement();
                //update progress dialog
                publishProgress((int) (1));
                //different scenarios
                //every parameters has to be picked before placing reservation request
                if(date.length() == 0){
                    reserveResult = "Please select Date";
                }else if(time.length() == 0){
                    reserveResult = "Please select Time";
                }else if(parkingLot == 0){
                    reserveResult = "Please select Parking Lot";
                }else {
                    //read available parking lot
                    ResultSet rs = st.executeQuery("SELECT * FROM lot" + parkingLot + " WHERE Status = 'Available'");
                    //get the first available parking lot
                    while (rs.next() && !foundLot) {
                        foundLot = true;
                        parkingID = rs.getInt("ParkingID");
                    }
                    //end function if no available parking lot found
                    if (foundLot = false) {
                        reserveResult = "No available Lot Found";
                        return null;
                    }
                    //deposit required for reserving parking lot
                    double deposit = 0.5;
                    //read all information related to user
                    rs = st.executeQuery("SELECT * FROM customers WHERE username='" + username + "'");
                    //get user's balance, if user's balance lower than minimum deposit
                    //refuse to reserve parking lot for user
                    while (rs.next()){
                        double balance = rs.getDouble("Balance");
                        if(balance < deposit){
                            reserveResult = "Insufficient Balance";
                            foundLot = false;
                            //close connection
                            conn.close();
                            return null;
                        }
                    }
                    //reach here if user has sufficient balance and parking lot is available
                    //change user's reservation status corresponding to reserved parking lot
                    //change parking lot information corresponding to reservation time
                    int rUser = st.executeUpdate("UPDATE customers SET ParkingLot='" + parkingLot + "', ParkingID='" + parkingID + "', balance=balance-'" + deposit + "' WHERE username='" + username + "'");
                    int rLot = st.executeUpdate("UPDATE lot" + parkingLot + " SET Status ='Reserved', Date='" + date + "', Time='" + time + ":00' WHERE parkingid='" + parkingID + "'");

                    //if reservation is successful
                    if (rUser > 0 && rLot > 0) {
                        reserveResult = "Reservation Success";
                        publishProgress((int) (2));
                        conn.close();
                        foundLot = false;
                        return null;
                    }
                }
                //close connection
                conn.close();
            } catch (ClassNotFoundException | SQLException ex) {
                //catch exceptions
                ex.printStackTrace();
            }
            //change foundLot back to false
            foundLot = false;
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
            //pass results to ForgetPasswordActivity class through interface
            delegate.fetchAsyncResponse(reserveResult);
            //close the progress dialog
            progressDialog.dismiss();
            //different scenarios
            //pop up dialogs based on result received from doInBackground()
            if(reserveResult.equals("Reservation Success")){
                new AlertDialog.Builder(ctx).setTitle("Reservation Success").setMessage("Parking Lot is now reserved...").setCancelable(false).show();
                //close this activity and start the main activity 2s after successful reservation
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        finish();
                        startActivity(new Intent(ReserveActivity.this, MainActivity.class));
                    }
                }, 2000);
            }else if(reserveResult.equals("No available Lot Found")){
                new AlertDialog.Builder(ctx).setTitle("Reservation Failed").setMessage("No available Lot Found, Please Try Another Garage").setCancelable(true).show();
            }else if(reserveResult.equals("Please select Date")){
                new AlertDialog.Builder(ctx).setTitle("ERROR").setMessage("Please retry").setCancelable(true).show();
            }else if(reserveResult.equals("Please select Time")){
                new AlertDialog.Builder(ctx).setTitle("ERROR").setMessage("Please retry").setCancelable(true).show();
            }else if(reserveResult.equals("Please select Parking Lot")){
                new AlertDialog.Builder(ctx).setTitle("ERROR").setMessage("Please retry").setCancelable(true).show();
            }else if(reserveResult.equals("Insufficient Balance")){
                new AlertDialog.Builder(ctx).setTitle("Insufficient Balance").setMessage("The minimum balance must greater than $0.50 to reserve").setCancelable(true).show();
            }else{
                new AlertDialog.Builder(ctx).setTitle("ERROR").setMessage("Unexpected Error").setCancelable(true).show();
            }
            //change parameters back to its default
            buttonGarage.setText("Select Garage");
            buttonDate.setText("Select Date");
            buttonTime.setText("Select Time");
            parkingLot = 0;
            parkingID = 0;
        }
    }

    //inner class that updates available parking lot number on buttons
    public class BackgroundFetchInformation extends AsyncTask<Void, Void, String> {
        //variables that keeps the login information that are required when connecting to server
        private String dbURL = "jdbc:mysql://192.168.0.100:3306/parkinglots";
        private String dbUsername = "root";
        private String dbPassword = "";
        //interface that passes result to parent class
        public AsyncResponse delegate = null;
        //hold the available spot number of each garage
        private int lotOneSpot = 0;
        private int lotTwoSpot = 0;
        //context of this class
        Context ctx;
        BackgroundFetchInformation(Context ctx) {
            this.ctx = ctx;
        }

        //executes and finishes before doInBackground executes
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        //where to put all the background jobs
        @Override
        protected String doInBackground(Void... params) {
            try {
                //set up sql connection
                Log.d("FETCH", "Connecting...");
                Class.forName("com.mysql.jdbc.Driver");
                Connection conn = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
                Statement st = conn.createStatement();
                //get all the available parking lots from parking lot 1
                ResultSet rsOne = st.executeQuery("SELECT * FROM lot1 WHERE Status = 'Available'");
                //count the available parking lots
                while (rsOne.next()) {
                    lotOneSpot += 1;
                }
                //get all the available parking lots from parking lot 2
                ResultSet rsTwo = st.executeQuery("SELECT * FROM lot2 WHERE Status = 'Available'");
                //count the available parking lots
                while (rsTwo.next()) {
                    lotTwoSpot += 1;
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
            //different scenarios
            //pop up dialogs based on result received from doInBackground()
            if(lotOneSpot == 0){
                bGarageOne.setOnClickListener(new OnClickListener() {
                    @Override
                    //executes when this button is clicked
                    public void onClick(View v) {
                        //pop up dialog tell user this lot is full
                        new AlertDialog.Builder(ctx).setTitle("ERROR").setMessage("Lot Full").setCancelable(true).show();
                    }
                });
            }
            if(lotTwoSpot == 0){
                bGarageTwo.setOnClickListener(new OnClickListener() {
                    @Override
                    //executes when this button is clicked
                    public void onClick(View v) {
                        //pop up dialog tell user this lot is full
                        new AlertDialog.Builder(ctx).setTitle("ERROR").setMessage("Lot Full").setCancelable(true).show();
                    }
                });
            }
            //update available parking lots number on button text
            bGarageOne.setText("Lot 1 [" + lotOneSpot + " Open]");
            bGarageTwo.setText("Lot 2 [" + lotTwoSpot + " Open]");
            //change parameters back to default
            lotOneSpot = 0;
            lotTwoSpot = 0;
        }
    }
}
