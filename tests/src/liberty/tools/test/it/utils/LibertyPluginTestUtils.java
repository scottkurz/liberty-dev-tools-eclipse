/*******************************************************************************
* Copyright (c) 2022 IBM Corporation and others.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v. 2.0 which is available at
* http://www.eclipse.org/legal/epl-2.0.
*
* SPDX-License-Identifier: EPL-2.0
*
* Contributors:
*     IBM Corporation - initial implementation
*******************************************************************************/
package liberty.tools.test.it.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWorkbenchBrowserSupport;
import org.junit.jupiter.api.Assertions;

/**
 * Tests Open Liberty Eclipse plugin functions.
 */
public class LibertyPluginTestUtils {

    /**
     * Validates that the deployed application is active.
     *
     * @param expectSuccess True if the validation is expected to be successful. False, otherwise.
     */
    public static void validateApplicationOutcome(String appName, boolean expectSuccess, String testAppPath) {
        String expectedMvnAppResp = "Hello! How are you today?";
        String appUrl = "http://localhost:9080/" + appName + "/servlet";
        int retryCountLimit = 40;
        int reryIntervalSecs = 3;
        int retryCount = 0;

        while (retryCount < retryCountLimit) {
            retryCount++;
            int status = 0;
            try {
                URL url = new URL(appUrl);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("GET");

                // Possible error: java.net.ConnectException: Connection refused
                con.connect();
                status = con.getResponseCode();

                if (expectSuccess) {
                    if (status != HttpURLConnection.HTTP_OK) {
                        Thread.sleep(reryIntervalSecs * 1000);
                        con.disconnect();
                        continue;
                    }

                    BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    String responseLine = "";
                    StringBuffer content = new StringBuffer();
                    while ((responseLine = br.readLine()) != null) {
                        content.append(responseLine).append(System.lineSeparator());
                    }

                    if (!(content.toString().contains(expectedMvnAppResp))) {
                        Thread.sleep(reryIntervalSecs * 1000);
                        con.disconnect();
                        continue;
                    }

                    return;
                } else {
                    if (status == HttpURLConnection.HTTP_OK) {
                        Thread.sleep(reryIntervalSecs * 1000);
                        con.disconnect();
                        continue;
                    }

                    return;
                }
            } catch (Exception e) {
                if (expectSuccess) {
                    System.out.println(
                            "INFO: Retrying application connection: Response code: " + status + ". Error message: " + e.getMessage());
                    try {
                        Thread.sleep(reryIntervalSecs * 1000);
                    } catch (Exception ee) {
                        ee.printStackTrace(System.out);
                    }
                    continue;
                }

                return;
            }
        }

        // If we are here, the expected outcome was not found.
        System.out.println("--------------------------- messages.log ----------------------------");
        try (BufferedReader br = new BufferedReader(new FileReader(testAppPath + "/wlp/usr/servers/defaultServer/logs/messages.log"))) {
    	   String line;
    	   while ((line = br.readLine()) != null) {
    	       System.out.println(line);
    	   }
    	} catch (Exception e) {
			e.printStackTrace();
		}
        System.out.println("---------------------------------------------------------------------");
        
        Assertions.fail("Timed out while waiting for application under URL: " + appUrl + " to become available.");
    }

    /**
     * Validates that the test report represented by the input path exists.
     *
     * @param pathToTestReport The path to the report.
     */
    public static void validateTestReportExists(Path pathToTestReport) {
        int retryCountLimit = 50;
        int reryIntervalSecs = 1;
        int retryCount = 0;

        while (retryCount < retryCountLimit) {
            retryCount++;

            boolean fileExists = fileExists(pathToTestReport.toAbsolutePath());
            if (!fileExists) {
                try {
                    Thread.sleep(reryIntervalSecs * 1000);
                } catch (Exception e) {
                    e.printStackTrace(System.out);
                    continue;
                }
                continue;
            }

            return;
        }

        // If we are here, the expected outcome was not found.
        Assertions.fail("Timed out while waiting for test report: " + pathToTestReport + " to become available.");
    }

    /**
     * Returns true or false depending on if the input text is found in the target file
     * @throws IOException 
     */
    public static boolean isTextInFile(String filePath, String text) throws IOException {
    	
    	List<String> lines = Files.readAllLines(Paths.get(filePath));
        for(String line : lines) {
        	if (line.contains(text)) {
        		return true;
        	}
        }
    	return false;
    }

    /**
     * Returns true if the Eclipse instance supports internal browsers. False, otherwise.
     *
     * @return True if the Eclipse instance supports internal browsers. False, otherwise.
     */
    public static boolean isInternalBrowserSupportAvailable() {
        final String availableKey = "available";
        final Map<String, Boolean> results = new HashMap<String, Boolean>();

        Display.getDefault().syncExec(new Runnable() {
            @Override
            public void run() {
                IWorkbenchBrowserSupport bSupport = PlatformUI.getWorkbench().getBrowserSupport();
                if (bSupport.isInternalWebBrowserAvailable()) {
                    results.put(availableKey, Boolean.TRUE);
                } else {
                    results.put(availableKey, Boolean.FALSE);
                }
            }
        });

        return results.get(availableKey);
    }
    
    /**
     * Adds or updates the JVM copy of the environment variables using the key and value inputs.
     *
     * @param key The environment variable to add or update.
     * @param value The value associated with the input key.
     *
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public static void updateJVMEnvVariableCache(String key, String value) throws Exception {
        Map<String, String> jvmEnvVars = null;
        if (onWindows()) {
            Class<?> pec = Class.forName("java.lang.ProcessEnvironment");
            Field field = pec.getDeclaredField("theCaseInsensitiveEnvironment");
            field.setAccessible(true);
            jvmEnvVars = (Map<String, String>) field.get(null);
        } else {
            Map<String, String> envVariables = System.getenv();
            Field field = envVariables.getClass().getDeclaredField("m");
            field.setAccessible(true);
            jvmEnvVars = ((Map<String, String>) field.get(envVariables));
        }

        jvmEnvVars.put(key, value);
    }
    
    /**
     * Returns true if the current process is running on a windows environment. False, otherwise.
     *
     * @return True if the current process is running on a windows environment. False, otherwise.
     */
    public static boolean onWindows() {
        return System.getProperty("os.name").contains("Windows");
    }

    /**
     * Returns true if the file identified by the input path exists. False, otherwise.
     *
     * @param path The file's path.
     *
     * @return True if the file identified by the input path exists. False, otherwise.
     */
    private static boolean fileExists(Path filePath) {
        File f = new File(filePath.toString());
        boolean exists = f.exists();

        return exists;
    }

    /**
     * Deletes file identified by the input path. If the file is a directory, it must be empty.
     *
     * @param path The file's path.
     *
     * @return Returns true if the file identified by the input path was deleted. False, otherwise.
     */
    public static boolean deleteFile(File file) {
        boolean deleted = true;

        if (file.exists()) {
            if (!file.isDirectory()) {
                deleted = file.delete();
            } else {
                deleted = deleteDirectory(file);
            }
        }

        return deleted;
    }

    /**
     * Recursively deletes the input file directory.
     *
     * @param filePath The directory path.
     *
     * @return
     */
    private static boolean deleteDirectory(File file) {
        File[] files = file.listFiles();
        if (files != null) {
            for (int i = 0; i < files.length; i++) {
                deleteDirectory(files[i]);
            }
        }
        return file.delete();
    }
}
