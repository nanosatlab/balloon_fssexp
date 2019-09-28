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
import java.io.OutputStream;
//import com.fazecast.jSerialComm.SerialPort;

/* Internal imports */
import Common.Constants;


/***********************************************************************************************//**
 * Test to verify that from Java it is possible to interact with a UART interface.
 **************************************************************************************************/
public class UARTtestTX {

    public static void main(String args[]) {
       
        /*SerialPort port = SerialPort.getCommPort("/dev/pts/4");
        port.setBaudRate(Constants.uart_bps);
        if(port.openPort()) {
            System.out.println("Port reached " + port.getDescriptivePortName());
            System.out.println("UART port with the following parameters:");
            System.out.println("- Baud Rate: " + port.getBaudRate());
            System.out.println("- Port description: " + port.getPortDescription());
            System.out.println("- System Port name: " + port.getSystemPortName());
        } else {
            System.out.println("ERROR by reaching the port");
        }
        
        
        /* getInputStream and getOutputStream */
        
        /*String message = "TEST OF WORKING TRANSMISSION";
        byte[] buffer = message.getBytes();
        OutputStream output = port.getOutputStream();
        System.out.println("Sending message ... " + message + " with size " + message.length());
        //port.writeBytes(buffer, message.length());
        try {
            output.write(buffer);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("Message sent!");
        port.closePort();*/
    }
}
