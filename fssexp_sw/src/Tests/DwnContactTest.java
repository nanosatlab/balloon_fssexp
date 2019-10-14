package Tests;

import java.io.IOException;

import Common.FolderUtils;
import Common.TimeUtils;
import Configuration.ExperimentConf;
import Downlink.TTC;
import Storage.Log;

public class DwnContactTest {

    public static void main(String args[]) throws IOException {

    	FolderUtils folder = new FolderUtils();
        TimeUtils time = new TimeUtils();
        Log logger = new Log(time, folder);
        ExperimentConf conf = new ExperimentConf(logger);
        TTC ttc = new TTC(time, conf, logger);
        
        ttc.start();
    
    }
    
}
