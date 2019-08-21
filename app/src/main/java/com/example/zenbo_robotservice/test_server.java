package com.example.zenbo_robotservice;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.asus.robotframework.API.RobotAPI;
import com.example.zenbo_robotservice.R;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class test_server {

    private static InetAddress myZenbo ;
    private static String myZenboIP , myZenboNAME ;
    private static int myZenboPORT = 8080 ;
    private static RobotAPI zenbo = null ;

    private static ServerSocket phoneControllerSocket ;
    private static Socket phoneController ;
    private static Thread socketConnection ;
    private static String ErrorMsg = null , phoneMsg = null ;


    public static void main(String args[] ) {
        try {
            myZenbo = InetAddress.getLocalHost() ;
            myZenboIP = myZenbo.getHostAddress() ;
            myZenboNAME = myZenbo.getHostName() ;
        }catch ( Exception e ) {
            System.out.println("Get Information error :" + e.toString()) ;
        }
        System.out.println("name = " + myZenboNAME) ;
        System.out.println("IP = " + myZenboIP) ;
        System.out.println("port = " + myZenboPORT) ;
        try {
            phoneControllerSocket = new ServerSocket(myZenboPORT) ;
            System.out.println("Server started") ;
            System.out.println("waiting for controller ... ") ;
            while ( ! phoneControllerSocket.isClosed() ) waitController() ;
        }catch ( Exception e ) {
            System.out.println("ERROR : "+e.toString()) ;
        }
    }

    private static void waitController() {
        try {
            phoneController = phoneControllerSocket.accept() ;
            System.out.println("a phone controller detected") ;
            setConnection(phoneController) ;
        }catch ( Exception e ) {
            System.out.println(e.toString()) ;
        }
    }

    private static void setConnection(final Socket socket ) {
        String str = null ;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream())) ;
            while ( socket.isConnected() ) {
                str = br.readLine() ;

                //new
                String[] command = str.split(" ") ;
                String cmd = "" ;
                for ( int i = 0 ; i < command.length ; i++ ) cmd = command[i] + " " ;
                cmd += '\n' ;

                if ( str == null ) {
                    System.out.println("phone has disconnected");
                    break;
                }
                System.out.println(str) ;
                castMsg(str) ;
            }
        }catch ( Exception e ) {
            System.out.println(e.toString()) ;
        }
    }

     private static void castMsg ( String msg ) {
        try {
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(phoneController.getOutputStream())) ;
            bw.write(msg+'\n');
            bw.flush();
        }catch (Exception e) {
            System.out.println(e.toString()) ;
        }
    }

}
