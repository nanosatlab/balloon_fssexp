package Tools;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ParsePersistentFile {

    public static void main(String args[]) throws IOException {
        
        System.out.println("Welcome to the Parsing Tool for the Persistent File of FSS Experiment");
        String result_file = "../UPC/persistent/manager.prst";
        File persistent_file = new File(result_file);
        FileInputStream file_stream = new FileInputStream(persistent_file);
        int exp_number = file_stream.read();
        file_stream.close();
        
        System.out.println("Persistent File:");
        System.out.println("    - Experiment number: " + exp_number);
    }
    
}
