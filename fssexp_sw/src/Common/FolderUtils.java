package Common;

import java.lang.System;


public class FolderUtils {

	public String home_folder;
	
	public FolderUtils() {
		home_folder = System.getProperty("user.dir");
	}
}
