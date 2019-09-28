/***************************************************************************************************
*  File:        DataGenerator.java                                                                 *
*  Authors:     Joan Adrià Ruiz de Azúa (JARA), <joan.adria@tsc.upc.edu>                           *
*  Creation:    2018-jun-18                                                                        *
*  Description: Class that is the manager of the FSS experiment. It controles the state of the     *
*               experiment and interacts with the platform.                                        *
*                                                                                                  *
*  This file is part of a project developed by Nano-Satellite and Payload Laboratory (NanoSat Lab) *
*  at Technical University of Catalonia - UPC BarcelonaTech.                                       *
* ------------------------------------------------------------------------------------------------ *
*  Changelog:                                                                                      *
*  v#   Date            Author  Description                                                        *
*  0.1  2018-jun-18     JARA    Skeleton creation                                                  *
***************************************************************************************************/

/* Own package */
package Tests;

/* External imports */
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

//import com.fazecast.jSerialComm.SerialPort;

/* Internal imports */
import Common.Constants;


/***********************************************************************************************//**
 * Test to verify that from Java it is possible to interact with a UART interface.
 **************************************************************************************************/
public class UARTtestRX {

    public static void main(String args[]) {
       
        /*SerialPort port = SerialPort.getCommPort("/dev/pts/11");
        port.setBaudRate(Constants.uart_bps);
        port.setComPortTimeouts(SerialPort.TIMEOUT_NONBLOCKING, 0, 0);
        if(port.openPort()) {
            System.out.println("Port reached " + port.getDescriptivePortName());
            System.out.println("UART port with the following parameters:");
            System.out.println("- Baud Rate: " + port.getBaudRate());
            System.out.println("- Port description: " + port.getPortDescription());
            System.out.println("- System Port name: " + port.getSystemPortName());
        } else {
            System.out.println("ERROR by reaching the port");
        }*/
        
        
        /* getInputStream and getOutputStream */
        
        
        
        /*String message;
        
        InputStream input = port.getInputStream();
        
        while(port.bytesAvailable() == 0) {
            try {
                System.out.println("Nothing received");
                Thread.sleep(500);
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        
        byte[] buffer = new byte[port.bytesAvailable()];
        System.out.println("Receiving message ... ");
        try {
            input.read(buffer);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
            
        //message = new String(buffer, StandardCharsets.UTF_8);
        message = new String(buffer);
        System.out.println("Message received: " + message + " with size " + message.length());
        port.closePort();*/
    }
}
