package com.example.zenbo_robotservice;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Enumeration;

import com.asus.robotframework.API.MotionControl;
import com.asus.robotframework.API.RobotAPI;
import com.asus.robotframework.API.RobotFace;
import com.asus.robotframework.API.WheelLights ;

public class MainActivity extends AppCompatActivity {

    private InetAddress myZenbo ;
    private String myZenboIP , myZenboNAME ;
    private int myZenboPORT = 8080 ;

    private RobotAPI zenbo = null ;
    private MotionControl.SpeedLevel.Body default_speed ;

    private TextView showZenboNAME , showZenboIP , showZenboPORT , dialog ;

    private ServerSocket phoneControllerSocket ;
    private Socket phoneController ;
    private Thread socketConnection ;
    private String ErrorMsg = null , phoneMsg = null ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        socketConnection = new Thread(connectToPhone) ;
        socketConnection.start() ;

        showZenboIP = findViewById(R.id.show_myZenboIp) ;
        showZenboNAME = findViewById(R.id.show_myZenboName) ;
        showZenboPORT = findViewById(R.id.show_myZenboPort) ;
        dialog = findViewById(R.id.dialog) ;

        zenbo = new RobotAPI(getApplicationContext(),null) ;
        default_speed = MotionControl.SpeedLevel.Body.L7 ;
    }

    private Runnable showZenboInfo = new Runnable() {
        @Override
        public void run() {
            showZenboIP.setText(myZenboIP) ;
            showZenboNAME.setText(myZenboNAME) ;
            showZenboPORT.setText(Integer.toString(myZenboPORT)) ;
        }
    } ;

    private Runnable connectToPhone = new Runnable() {
        @Override
        public void run() {
            try {
                Enumeration<NetworkInterface> enumNetworkInterface = NetworkInterface.getNetworkInterfaces() ;
                while ( enumNetworkInterface.hasMoreElements() ) {
                    NetworkInterface networkInterface = enumNetworkInterface.nextElement() ;
                    Enumeration<InetAddress> enumInetAddress = networkInterface.getInetAddresses() ;
                    while ( enumInetAddress.hasMoreElements() ) {
                        InetAddress inetAddress = enumInetAddress.nextElement() ;
                        if ( inetAddress.isSiteLocalAddress() ) myZenboIP = inetAddress.getHostAddress() ;
                    }
                }

                myZenbo = InetAddress.getLocalHost();
                myZenboNAME = myZenbo.getHostName() ;
            }catch ( Exception e ) {
                Log.e("Get Information error :" , e.toString()) ;
            }
            runOnUiThread(showZenboInfo);
            try {
                phoneControllerSocket = new ServerSocket(myZenboPORT) ;
                runOnUiThread(serverStartInfo);
                waitController() ;
            }catch ( Exception e ) {
                Log.e("ERROR : ",e.toString()) ;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        dialog.append("Controller disconnect") ;
                    }
                });
            }
        }
    } ;

    private Runnable serverStartInfo = new Runnable() {
        @Override
        public void run() {
            dialog.append("Server Started"+'\n') ;
            dialog.append("Waiting for controller..."+'\n') ;
        }
    } ;

    private void waitController() {
        try {
            phoneController = phoneControllerSocket.accept() ;
            phoneMsg = "a phone controller detected" + '\n' ;
            runOnUiThread(updateDialog) ;
            setConnection(phoneController) ;
        }catch ( Exception e ) {
            ErrorMsg = e.toString() ;
            runOnUiThread(updateDialog) ;
        }
    }

    private Runnable updateDialog = new Runnable() {
        @Override
        public void run() {
            if ( ErrorMsg != null ) {
                dialog.append(ErrorMsg + '\n');
                ErrorMsg = null;
            }
            if ( phoneMsg != null ) {
                dialog.append(phoneMsg+'\n');
                phoneMsg = null ;
            }
        }
    } ;

    private void setConnection(final Socket socket ) {
        String str ;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream())) ;
            Toast.makeText(this, "listening to socket", Toast.LENGTH_LONG).show();
            while ( socket.isConnected() ) {
                phoneMsg = br.readLine() ;
                str = phoneMsg ;
                if ( phoneMsg == null ) {
                    ErrorMsg = "phone has disconnected" + '\n';
                    runOnUiThread(updateDialog);
                    break;
                }

                //deal with zenbo command here
                switch ( phoneMsg ) {

                    case "move forward" : {
                        zenbo.motion.stopMoving() ;
                        zenbo.motion.moveBody(0.5f,0f,0f,default_speed) ;
                        break ;
                    }

                    case "say" : {
                        zenbo.robot.speak("fuck") ;
                        break ;
                    }

                    case "smile" : {
                        zenbo.robot.setExpression(RobotFace.HAPPY) ;
                        break ;
                    }

                    case "look up" : {
                        zenbo.motion.moveHead(0,50,MotionControl.SpeedLevel.Head.L3) ;
                        break ;
                    }

                    default : {
                        castMsg("NO such command exist");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dialog.append("NO such command exist") ;
                            }
                        });
                    }
                }

                runOnUiThread(updateDialog);
                castMsg(str) ;
            }
        }catch ( Exception e ) {
            ErrorMsg = e.toString() ;
            runOnUiThread(updateDialog) ;
        }
    }

    private void castMsg ( String msg ) {
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(phoneController.getOutputStream())) ;
            bw.write(msg+'\n');
            bw.flush();
        }catch (Exception e) {
            ErrorMsg = e.toString() ;
            runOnUiThread(updateDialog) ;
        }
    }

}
