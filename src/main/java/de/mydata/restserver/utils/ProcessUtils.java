package de.mydata.restserver.utils;

import de.mydata.model.Predicate;
import org.eclipse.jetty.util.ReadLineInputStream;

import java.io.IOException;

/**
 * @author Bjoern Frohberg, mydata GmbH
 */
public class ProcessUtils {
	
	public static String awaitProcessForOutput(String command) throws IOException, InterruptedException {
		return awaitProcessForOutput(command, false);
	}
	
	public static String awaitProcessForOutput(String command, boolean embeddedProcess) throws IOException, InterruptedException {
		return awaitProcessForOutput(command, null, embeddedProcess);
	}
	
	public static String awaitProcessForOutput(String command, Predicate<String> continueCondition) throws IOException, InterruptedException {
		return awaitProcessForOutput(command, continueCondition, false);
	}
	
	public static String awaitProcessForOutput(String command, Predicate<String> continueCondition, boolean embeddedProcess) throws IOException, InterruptedException {
		String  output;
		Process process;
		if(embeddedProcess) {
			process = new ProcessBuilder(command.split("\\s")).start();
		} else {
			process = Runtime.getRuntime().exec(command);
		}
		ReadLineInputStream reader = new ReadLineInputStream(process.getInputStream());
		StringBuilder       sb     = new StringBuilder();
		for (String line = reader.readLine(); line != null; line = reader.readLine()) {
			if(sb.length() > 0) {
				sb.append(System.lineSeparator());
			}
			sb.append(line);
			if(continueCondition != null && continueCondition.test(sb.toString())) {
				break;
			}
		}
		output = sb.toString();
		return output;
	}
}
