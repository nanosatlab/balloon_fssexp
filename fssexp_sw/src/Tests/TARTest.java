package Tests;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import Common.Constants;
import TAR.TarEntry;
import TAR.TarOutputStream;
import jZLib.GZIPOutputStream;


public class TARTest {

    public static void main(String args[]) throws IOException {
        
        int experiment_id = 1;
        
        System.out.println(System.getProperty("user.dir"));
        String download_path = "./UPC/to_download/";
        String destination = download_path + String.format("%03d", experiment_id) + "_" + System.currentTimeMillis() + "_" + Constants.dwn_file_pattern;
        
        File file = new File(destination);
        
        if(file.exists() == true) {
            file.delete();
        }
        //file.createNewFile();
        
        FileOutputStream dest = new FileOutputStream( destination );
        
        // Create a TarOutputStream
        TarOutputStream out = new TarOutputStream( new BufferedOutputStream( dest ) );
        
        // Files to tar
        File[] filesToTar=new File[3];
        filesToTar[0]=new File(download_path + "fss_packets.data");
        filesToTar[1]=new File(download_path + "housekeeping.data");
        filesToTar[2]=new File(download_path + "log.data");
        
        /*for(File f:filesToTar){
           out.putNextEntry(new TarEntry(f, f.getName()));
           BufferedInputStream origin = new BufferedInputStream(new FileInputStream( f ));
           int count;
           byte data[] = new byte[2048];
        
           while((count = origin.read(data)) != -1) {
              out.write(data, 0, count);
           }
        
           out.flush();
           origin.close();
        }
        
        out.close();
        
        
        /* Compress to GZIP */
        String gzip = download_path + String.format("%03d", experiment_id) + "_" + System.currentTimeMillis() + "_" + Constants.dwn_file_pattern + ".gz";
        
        GZIPOutputStream out_file = new GZIPOutputStream(new FileOutputStream(gzip));
        FileInputStream in = new FileInputStream(destination);
        byte[] buffer = new byte[1024];
        int len;
        int offset = 0;
        while((len = in.read(buffer)) != -1){
            System.out.println("Writing: offset " + offset + " length " + len);
            out_file.write(buffer, 0, len);
        }
        
        out_file.close();
        
        /*GZIPOutputStream out_file = new GZIPOutputStream(new FileOutputStream(gzip));
        FileInputStream in = new FileInputStream(destination);
        byte[] buffer = new byte[1024];
        int len;
        while((len=in.read(buffer)) != -1){
            out_file.write(buffer, 0, len);
        }
        
        out_file.close();*/
        
    }
    
}
