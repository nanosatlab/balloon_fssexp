package Tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import Common.TimeUtils;
import Housekeeping.RFISLHousekeepingItem;

public class GroundSegment {

	public static void main(String args[]) throws IOException
    {
		System.out.println("");
        System.out.println("  ######   ########   #######  ##     ## ##    ## ########      ######  ########    ###    ######## ####  #######  ##    ## ");
        System.out.println(" ##    ##  ##     ## ##     ## ##     ## ###   ## ##     ##    ##    ##    ##      ## ##      ##     ##  ##     ## ###   ## ");
        System.out.println(" ##        ##     ## ##     ## ##     ## ####  ## ##     ##    ##          ##     ##   ##     ##     ##  ##     ## ####  ## ");
        System.out.println(" ##   #### ########  ##     ## ##     ## ## ## ## ##     ##     ######     ##    ##     ##    ##     ##  ##     ## ## ## ## ");
        System.out.println(" ##    ##  ##   ##   ##     ## ##     ## ##  #### ##     ##          ##    ##    #########    ##     ##  ##     ## ##  #### ");
        System.out.println(" ##    ##  ##    ##  ##     ## ##     ## ##   ### ##     ##    ##    ##    ##    ##     ##    ##     ##  ##     ## ##   ### ");
        System.out.println("  ######   ##     ##  #######   #######  ##    ## ########      ######     ##    ##     ##    ##    ####  #######  ##    ## ");
        System.out.println("");
        
        System.out.println("Welcome to the Balloon-FSSExp Ground Station. It allows sending telecommands and receive data.");
        TimeUtils timer = new TimeUtils();
        GroundStation ground_station = new GroundStation(timer);
        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
        boolean exit = false;
        int remote_sat;
        String[] words; 
        RFISLHousekeepingItem item;
        
        /* Start Ground Station */
        ground_station.start();
        System.out.println("[" + timer.getTimeMillis() + "] Ground Station thread started");
        
        /* Verify communication with RF ISL Module */
        if(ground_station.isTransceiverConnected() == true) {
        	System.out.println("[" + timer.getTimeMillis() + "] Connection with RF ISL Module is established");
        } else {
        	System.out.println("[" + timer.getTimeMillis() + "][ERROR] Impossible to communicate with RF ISL Module");
        	System.out.println("[" + timer.getTimeMillis() + "] Shutting down the ground station");
        	System.exit(-1);
        }
        
        /* Main Loop */
        System.out.println("");
    	System.out.println("");
        System.out.println("------------------------------------- START -------------------------------------");
        System.out.println("Please, insert the telecommand to be sent:");
        System.out.println("    - HELLO::address");
        System.out.println("    - CLOSE");
        System.out.println("    - CHECK-CONNECTION");
        System.out.println("    - CHECK-TRANSCEIVER");
        System.out.println("    - HK-TRANSCEIVER");
        System.out.println("    - BYE - Close the Ground Station");
        while(exit == false) {
            System.out.println("Please, insert the telecommand to be sent: HELLO::address | CLOSE | CHECK-CONNECTION | CHECK-TRANSCEIVER | HK-TRANSCEIVER | BYE");
            System.out.print("Telecommand to send: ");
            String s = bufferRead.readLine().toUpperCase();
            if(s.contains("HELLO")) {
            	words = s.split("::");
            	if(words.length == 2) {
            		remote_sat = Integer.parseInt(words[1]);
            		if(remote_sat % 2 == 0) {
	            		ground_station.remote_sat = remote_sat; 
	            		if(ground_station.connectWithBalloon(ground_station.remote_sat) == true) {
	            			System.out.println("[" + timer.getTimeMillis() + "] Downlink connection is established with balloon " + ground_station.remote_sat);
	            		} else {
	            			System.out.println("[" + timer.getTimeMillis() + "] Impossible to establish connection with balloon " + remote_sat);
	            		}
            		} else {
            			System.out.println("[" + timer.getTimeMillis() + "][ERROR] Bad balloon address, it has to be 2^n");
            		}
            	} else {
            		System.out.println("[" + timer.getTimeMillis() + "][ERROR] Bad format for a telecommands. Please, insert: HELLO::address");
            	}
            } else if(s.contains("CLOSE")) {
            	remote_sat = ground_station.remote_sat;
            	if(ground_station.disconnectWithBalloon() == true) {
            		System.out.println("[" + timer.getTimeMillis() + "] Downlink connection is closed with balloon " + remote_sat);
            	} else {
            		System.out.println("[" + timer.getTimeMillis() + "] Impossible to correctly close the connection with balloon " + remote_sat);
            	}
            } else if(s.contains("CHECK-CONNECTION")) {
            	remote_sat = ground_station.remote_sat;
            	if(remote_sat != -1) {
	            	System.out.println("[" + timer.getTimeMillis() + "] The Ground Station is connected to balloon " + remote_sat);
            	} else {
            		System.out.println("[" + timer.getTimeMillis() + "] No downlink connection is established with any balloon");
            	}
        	} else if(s.contains("CHECK-TRANSCEIVER")) {
            	if(ground_station.checkTransceiver() == true) {
            		System.out.println("[" + timer.getTimeMillis() + "] The RF ISL Module is connected correctly");
            	} else {
            		System.out.println("[" + timer.getTimeMillis() + "][ERROR] Impossible to communicate with RF ISL Module");
            	}
        	} else if(s.contains("HK-TRANSCEIVER")) {
        		item = ground_station.requestTelemetry();
        		System.out.println("[" + timer.getTimeMillis() + "] " + item.toString());
        	} else if(s.contains("BYE")) {
        		ground_station.controlledStop();
        		while(ground_station.getState() != Thread.State.TERMINATED) {
        			/* Check again */
        			// TODO:
        		}
            	exit = true;
            	System.out.println("[" + timer.getTimeMillis() + "] Closing the Ground Station");
            } else {
            	System.out.println("[" + timer.getTimeMillis() + "][WARNING] Unknown inserted telecommand - Not transmitted");
            }
        }
        System.out.println("------------------------------------- END -------------------------------------");
        
        System.out.println("[" + timer.getTimeMillis() + "] Sayonara baby.");
    }
}
