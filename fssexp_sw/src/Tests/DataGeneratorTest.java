package Tests;

import FSS_experiment.DataGenerator;
import IPCStack.SimpleLinkProtocol;
import Storage.FSSDataBuffer;
import Storage.Log;

public class DataGeneratorTest {

    public static void main(String args[]) {

        Log logger = new Log();
        FSSDataBuffer buffer = new FSSDataBuffer(logger, 10);
        SimpleLinkProtocol ipc_stack = new SimpleLinkProtocol(logger, 0);
        DataGenerator generator = new DataGenerator(logger, 1, -1, 0, buffer, ipc_stack);
        
        generator.start();
        
        try {
            Thread.sleep(15000);
        } catch (InterruptedException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        
        generator.controlledStop();
        
        try {
            generator.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    
    }
    
}
