package FSS_experiment;

import java.io.IOException;

public class FSSExp {

	private static String TAG = "[FSSExp] ";
    
    public static void main(String args[]) throws InterruptedException, IOException 
    {
    	/* This interface is just used to retrieve status and to stop the software */
    	TCPInterface tcp_interface = new TCPInterface();
    	tcp_interface.start();
    	tcp_interface.join();
    	System.out.println(TAG + "Me voy");
    	System.exit(0);
    }
}