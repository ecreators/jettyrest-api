package de.mydata.restserver.ssl;

import de.mydata.restserver.utils.ProcessUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public class SSLCert {
	
	public static String makeSSL(File javaKeyTool, String storePassword, File certOutputFile) {
		String output;
		
		int    vadility         = 360;
		int    keysizeBytes     = 2048;
		String destincationFile = certOutputFile.getPath();
		String command = String.format("%s -genkey -keyalg RSA -alias selfsigned -keystore %s -storepass %s -validity %s -keysize %s",
		                               javaKeyTool.getPath(),
		                               destincationFile,
		                               storePassword,
		                               vadility,
		                               keysizeBytes);
		try {
			output = ProcessUtils.awaitProcessForOutput(command);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}
		return output;
	}
	
}
