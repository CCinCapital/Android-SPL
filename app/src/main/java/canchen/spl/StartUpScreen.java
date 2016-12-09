/* Author: Can Chen 100778989
 * This display the startup screen for 3s when app is open
 */

package canchen.spl;

//Widgets and Libraries required for this class
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

//class must implement activity in order to display something on screen
public class StartUpScreen extends Activity {
    //parameter defines duration
    private static int TIME_OUT = 3000;

    //Method automatically executes whenever This activity is created
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        //set the XML Layout of this View
        setContentView(R.layout.activity_startupdisplay);

        //close this activity and start login activity after TIME_OUT
        new Handler().postDelayed(new Runnable(){
            @Override
                    public void run(){
                Intent intent = new Intent(StartUpScreen.this, LoginActivity.class);
                startActivity(intent);
                finish();
            }
        }, TIME_OUT);
    }
}
