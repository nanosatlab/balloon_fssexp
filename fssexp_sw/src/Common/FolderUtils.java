package Common;

import java.io.File;
import java.io.IOException;
import java.lang.System;


public class FolderUtils {

	private String home_folder;
	private long creation_time;
	public String hk_name;
	public String prst_name;
	public String log_name;
	public String conf_name;
	public String dwn_name;
	public String tx_name;
	public String rx_name;
	public String payload_name;
	
	private static String TAG = "[FolderUtils] ";
	
	public FolderUtils(TimeUtils time) 
	{
		creation_time = time.getTimeMillis();
		home_folder = System.getProperty("user.dir");
		createTreeFolder();
		createFiles();
	}
	
	public void createTreeFolder() 
	{
		/* check main folder */
		File fldr = new File(home_folder + Constants.exp_root);
		if(fldr.exists() == false) {
			fldr.mkdir();
		}
		/* check persistent */
		fldr = new File(home_folder + Constants.persistent_path);
		if(fldr.exists() == false) {
			fldr.mkdir();
		}
		/* check log */
		fldr = new File(home_folder + Constants.log_path);
		if(fldr.exists() == false) {
			fldr.mkdir();
		}
		/* check data */
		fldr = new File(home_folder + Constants.data_path);
		if(fldr.exists() == false) {
			fldr.mkdir();
		}
		/* check fss data */
		fldr = new File(home_folder + Constants.fss_data_path);
		if(fldr.exists() == false) {
			fldr.mkdir();
		}
		/* check configuration */
		fldr = new File(home_folder + Constants.conf_path);
		if(fldr.exists() == false) {
			fldr.mkdir();
		}
	}
	
	public boolean createFiles() 
	{
		boolean correct = false;
		try {
			/* check persistent */
			prst_name = home_folder + Constants.persistent_file + "_" + creation_time;
			File fl = new File(prst_name);
			if(fl.exists() == false) {
				fl.createNewFile();
			}
			System.out.println(TAG + "Created folder " + prst_name);
			/* check log */
			log_name = home_folder + Constants.log_file + "_" + creation_time;
			fl = new File(log_name);
			if(fl.exists() == false) {
				fl.createNewFile();
			}
			System.out.println(TAG + "Created folder " + log_name);
			/* check hk */
			hk_name = home_folder + Constants.hk_file + "_" + creation_time;
			fl = new File(hk_name);
			if(fl.exists() == false) {
				fl.createNewFile();
			}
			System.out.println(TAG + "Created folder " + hk_name);
			/* check downloaded packets */
			dwn_name = home_folder + Constants.dwn_file + "_" + creation_time;
			fl = new File(dwn_name);
			if(fl.exists() == false) {
				fl.createNewFile();
			}
			System.out.println(TAG + "Created folder " + dwn_name);
			/* check isl tx packets */
			tx_name = home_folder + Constants.tx_file + "_" + creation_time;
			fl = new File(tx_name);
			if(fl.exists() == false) {
				fl.createNewFile();
			}
			System.out.println(TAG + "Created folder " + tx_name);
			/* check isl rx packets */
			rx_name = home_folder + Constants.rx_file + "_" + creation_time;
			fl = new File(rx_name);
			if(fl.exists() == false) {
				fl.createNewFile();
			}
			System.out.println(TAG + "Created folder " + rx_name);
			/* check payload packets */
			payload_name = home_folder + Constants.payload_file + "_" + creation_time;
			fl = new File(payload_name);
			if(fl.exists() == false) {
				fl.createNewFile();
			}
			System.out.println(TAG + "Created folder " + payload_name);
			/* check configuration */
			conf_name = home_folder + Constants.conf_file + "_" + creation_time;
			fl = new File(conf_name);
			if(fl.exists() == false) {
				fl.createNewFile();
			}
			System.out.println(TAG + "Created folder " + conf_name);
			correct = true;
		} catch(IOException e) {
			System.out.println(TAG + "[ERROR] Impossible to create the files");
		}
		return correct;
	}
}
