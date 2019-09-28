package Tests;

import java.nio.ByteBuffer;

import Common.Constants;
import FSS_experiment.DataGenerator;
import Storage.FSSDataBuffer;
import Storage.Log;

public class FSSDataBufferTest {

    public static void main(String args[]) {

        Log logger = new Log();
        int max = 10;
        FSSDataBuffer buffer = new FSSDataBuffer(logger, max);
        
        System.out.println("Buffer size: " + buffer.getSize());
        byte[] data_2;
        byte[] data = new byte[Constants.data_size];
        data = "hello world!".getBytes();
        
        
        /* No content, null pointer */
        data_2 = buffer.extractData();
        if(data_2 == null) {
            System.out.println("Nothing to extract");
        }
        
        /* Add data */
        if(buffer.insertData(data)) {
            System.out.println("Inserted data correctly");
        } else {
            System.out.println("Error during insert data");
        }
        
        System.out.println("Buffer size: " + buffer.getSize());
        
        data = "paco peco ch".getBytes();
        buffer.insertData(data);
        
        System.out.println("Buffer size: " + buffer.getSize());
    
        /* Extract data */
        data_2 = buffer.extractData();
        if(data_2 != null) {
            //System.out.println("Extracted correctly: " + new String(data_2, StandardCharsets.UTF_8) + " with size " + data_2.length);
        } else {
            System.out.println("Error during extraction");
        }
        
        System.out.println("Buffer size: " + buffer.getSize());
        
        data_2 = buffer.extractData();
        if(data_2 != null) {
            //System.out.println("Extracted correctly: " + new String(data_2, StandardCharsets.UTF_8));
        } else {
            System.out.println("Error during extraction");
        }
    
        System.out.println("Buffer size: " + buffer.getSize());
        
        /* Until the limit */
        for(int i = 0; i < max + 5; i ++) {
            data = String.format("--0x%08X", i).getBytes();
            buffer.insertData(data);
        }
        
        System.out.println("Buffer size: " + buffer.getSize());
        System.out.println("Dropped: " + buffer.getDrops());
        
    }
    
}
