package Tools;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;

import CBOR.CborDecoder;
import CBOR.CborEncoder;


public class SpacecraftEmulator {

    public static void main(String args[]) throws IOException {
        
        Socket socket = null;
        String host = "127.0.0.1";
        short port = 4444;
        
        System.out.println("Welcome to the Platform Emulator!");
        System.out.println("This emulator will help you to interact with FSS Experiment.");
        
        System.out.println("First, let's try to connect at TCP level ... ");
        socket = new Socket(host, port);
        System.out.println("Socket Connected!");

        
        ByteArrayOutputStream intermediate_output_stream = new ByteArrayOutputStream();
        byte[] input_buffer = new byte[0]; 
        ByteArrayInputStream intermediate_input_stream = new ByteArrayInputStream(input_buffer);
        CborEncoder cbor_encoder = new CborEncoder(intermediate_output_stream);
        CborDecoder cbor_decoder = new CborDecoder(intermediate_input_stream);
        
        BufferedReader bufferRead = new BufferedReader(new InputStreamReader(System.in));
        boolean exit = false;
        int reply;
        ByteBuffer length = ByteBuffer.allocate(2);
        byte[] reply_length = new byte[2];
        short L;
        
        while(exit == false) {
            System.out.println("-------------------------------------");
            System.out.println("Please, insert the action to perform:");
            System.out.println("    - START");
            System.out.println("    - STOP");
            System.out.println("    - STATUS");
            System.out.println("    - EXIT");
            System.out.println("    - WRONG");
            System.out.println("    - BYE - Exists of this simulator");
            System.out.println("");
            System.out.print("Command to send: ");
            
            String s = bufferRead.readLine().toUpperCase();

            if(s.equals("START")) {
                /* Start the FSS Experiment execution */
                System.out.println("Sending START command ..");
                
                /* command: START; timestamp: Integer */
                intermediate_output_stream.reset();
                cbor_encoder.writeMapStart(2);
                cbor_encoder.writeTextString("command");
                cbor_encoder.writeTextString(s);
                cbor_encoder.writeTextString("timestamp");
                System.out.println("Timestamp : " + ((int)(System.currentTimeMillis() / 1000) & 0xFFFFFFFF));
                cbor_encoder.writeInt32(((int)(System.currentTimeMillis()  / 1000) & 0xFFFFFFFF));
                byte[] content = intermediate_output_stream.toByteArray();
                System.out.println("Packet length " + content.length);
                System.out.println("Length sent " + (content.length & 0xFFFF));
                length.clear();
                length.putShort((short)(content.length));
                socket.getOutputStream().write(length.array());
                socket.getOutputStream().write(content);
                
                /* Reply */
                System.out.println("Waiting Reply ... ");
                socket.getInputStream().read(reply_length);
                length.clear();
                length.put(reply_length);
                length.rewind();
                L = length.getShort();
                System.out.println("Reply received");
                System.out.println(" - Reply length: " + L);
                input_buffer = new byte[L];
                socket.getInputStream().read(input_buffer);
                intermediate_input_stream = new ByteArrayInputStream(input_buffer);
                cbor_decoder = new CborDecoder(intermediate_input_stream);
                System.out.println(" - Map items: " + cbor_decoder.readMapLength());
                System.out.println(" - Command item: (" + cbor_decoder.readTextString() + ", " + cbor_decoder.readTextString() + ")");
                System.out.println(" - ACK item: (" + cbor_decoder.readTextString() + ", " + cbor_decoder.readTextString() + ")");
                
            } else if(s.equals("STOP")) {
                /* Stop the FSS Experiment execution */
                System.out.println("Sending STOP command");
                /* command: START; timestamp: Integer */
                intermediate_output_stream.reset();
                cbor_encoder.writeMapStart(2);
                cbor_encoder.writeTextString("command");
                cbor_encoder.writeTextString(s);
                cbor_encoder.writeTextString("timestamp");
                System.out.println("Timestamp : " + ((int)(System.currentTimeMillis() / 1000) & 0xFFFFFFFF));
                
                cbor_encoder.writeInt32(((int)(System.currentTimeMillis() / 1000) & 0xFFFFFFFF));
                byte[] content = intermediate_output_stream.toByteArray();
                System.out.println("Packet length " + content.length);
                System.out.println("Length sent " + (content.length & 0xFFFF));
                length.clear();
                length.putShort((short)(content.length));
                socket.getOutputStream().write(length.array());
                socket.getOutputStream().write(content);
                
                /* Reply */
                System.out.println("Waiting Reply ... ");
                socket.getInputStream().read(reply_length);
                length.clear();
                length.put(reply_length);
                length.rewind();
                L = length.getShort();
                System.out.println("Reply received");
                System.out.println(" - Reply length: " + L);
                input_buffer = new byte[L];
                socket.getInputStream().read(input_buffer);
                intermediate_input_stream = new ByteArrayInputStream(input_buffer);
                cbor_decoder = new CborDecoder(intermediate_input_stream);
                System.out.println(" - Map items: " + cbor_decoder.readMapLength());
                System.out.println(" - Command item: (" + cbor_decoder.readTextString() + ", " + cbor_decoder.readTextString() + ")");
                System.out.println(" - ACK item: (" + cbor_decoder.readTextString() + ", " + cbor_decoder.readTextString() + ")");
                
                
            } else if(s.equals("STATUS")) {
                /* Request status */
                System.out.println("Sending STATUS command");
                /* command: STATUS; timestamp: Integer */
                intermediate_output_stream.reset();
                cbor_encoder.writeMapStart(2);
                cbor_encoder.writeTextString("command");
                cbor_encoder.writeTextString(s);
                cbor_encoder.writeTextString("timestamp");
                System.out.println("Timestamp : " + ((int)(System.currentTimeMillis() / 1000) & 0xFFFFFFFF));
                
                cbor_encoder.writeInt32(((int)(System.currentTimeMillis() / 1000) & 0xFFFFFFFF));
                byte[] content = intermediate_output_stream.toByteArray();
                System.out.println("Packet length " + content.length);
                System.out.println("Length sent " + (content.length & 0xFFFF));
                length.clear();
                length.putShort((short)(content.length));
                socket.getOutputStream().write(length.array());
                socket.getOutputStream().write(content);
                
                /* Reply */
                System.out.println("Waiting Reply ... ");
                socket.getInputStream().read(reply_length);
                length.clear();
                length.put(reply_length);
                length.rewind();
                L = length.getShort();
                System.out.println("Reply received");
                System.out.println(" - Reply length: " + L);
                input_buffer = new byte[L];
                socket.getInputStream().read(input_buffer);
                intermediate_input_stream = new ByteArrayInputStream(input_buffer);
                cbor_decoder = new CborDecoder(intermediate_input_stream);
                long map_length = cbor_decoder.readMapLength();
                System.out.println(" - Map items: " + map_length);
                System.out.println(" - Command item: (" + cbor_decoder.readTextString() + ", " + cbor_decoder.readTextString() + ")");
                System.out.println(" - ACK item: (" + cbor_decoder.readTextString() + ", " + cbor_decoder.readTextString() + ")");
                //System.out.println(" - Mode item: (" + cbor_decoder.readTextString() + ")");
                System.out.println(" - Mode item: (" + cbor_decoder.readTextString() + ", " + cbor_decoder.readTextString() + ")");
                if(map_length > 3) {
                	System.out.println(" - Message item: (" + cbor_decoder.readTextString() + ", " + cbor_decoder.readTextString() + ")");
                } 
            } else if(s.equals("EXIT")) {
                /* Request status */
                System.out.println("Sending EXIT command");
                /* command: START; timestamp: Integer */
                intermediate_output_stream.reset();
                cbor_encoder.writeMapStart(2);
                cbor_encoder.writeTextString("command");
                cbor_encoder.writeTextString(s);
                cbor_encoder.writeTextString("timestamp");
                System.out.println("Timestamp : " + ((int)(System.currentTimeMillis() / 1000) & 0xFFFFFFFF));
                cbor_encoder.writeInt32(((int)(System.currentTimeMillis()  / 1000) & 0xFFFFFFFF));
                //cbor_encoder.writeInt((int)(System.currentTimeMillis() & 0xFFFFFFFF));
                byte[] content = intermediate_output_stream.toByteArray();
                System.out.println("Packet length " + content.length);
                System.out.println("Length sent " + (content.length & 0xFFFF));
                length.clear();
                length.putShort((short)(content.length));
                socket.getOutputStream().write(length.array());
                socket.getOutputStream().write(content);
                
                /* Reply */
                System.out.println("Waiting Reply ... ");
                socket.getInputStream().read(reply_length);
                length.clear();
                length.put(reply_length);
                length.rewind();
                L = length.getShort();
                System.out.println("Reply received");
                System.out.println(" - Reply length: " + L);
                input_buffer = new byte[L];
                socket.getInputStream().read(input_buffer);
                intermediate_input_stream = new ByteArrayInputStream(input_buffer);
                cbor_decoder = new CborDecoder(intermediate_input_stream);
                System.out.println(" - Map items: " + cbor_decoder.readMapLength());
                System.out.println(" - Command item: (" + cbor_decoder.readTextString() + ", " + cbor_decoder.readTextString() + ")");
                System.out.println(" - ACK item: (" + cbor_decoder.readTextString() + ", " + cbor_decoder.readTextString() + ")");
                
            } else if(s.equals("BYE")) {
                /* Stops this simulator */
                System.out.println("Bye bye!");
                exit = true;
            } else {
                /* Wrong command */
                System.out.println("Sending WRONG command");
                cbor_encoder.writeInt8(5);
                System.out.println("Command Sent at " + System.currentTimeMillis());
                System.out.println("Reading reply");
                reply = cbor_decoder.readInt8();
                System.out.println("Reply received " + reply);
                break;
            }
            System.out.println("");
        }
        
        
        if(socket != null) {
            socket.close();
        }
            
    }
    
}
