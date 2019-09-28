package FSS_experiment;

import java.io.FileNotFoundException;

public class FSSExp {

	private static String TAG = "[FSSExp] ";
    
    public static void main(String args[]) throws FileNotFoundException, InterruptedException {
	
    	TCPInterface tcp_interface = new TCPInterface();
    	tcp_interface.start();
    	tcp_interface.join();
    	System.out.println(TAG + "Me voy");
    	System.exit(0);
    }
}