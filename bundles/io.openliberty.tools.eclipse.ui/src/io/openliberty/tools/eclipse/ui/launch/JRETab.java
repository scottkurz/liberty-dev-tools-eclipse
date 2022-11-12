package io.openliberty.tools.eclipse.ui.launch;

import java.io.File;
import java.nio.file.Paths;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.debug.ui.launchConfigurations.JavaJRETab;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;

import io.openliberty.tools.eclipse.logging.Trace;
import io.openliberty.tools.eclipse.utils.ErrorHandler;
import io.openliberty.tools.eclipse.utils.Utils;

public class JRETab extends JavaJRETab {

    /** JRE container key. */
    public static final String JRE_CONTAINER_KEY = "org.eclipse.jdt.launching.JRE_CONTAINER";

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isValid(ILaunchConfiguration config) {
        if (!super.isValid(config)) {
            return false;
        }

        setErrorMessage(null);

        // Issue a warning if we detect that the java installation is not a JDK.
        String javaHome = resolveJavaHome(config);
        java.nio.file.Path javacPath = Paths.get(javaHome, "bin", (Utils.isWindows() ? "javac.exe" : "javac"));
        File javacFile = javacPath.toFile();
        if (!javacFile.exists()) {
            super.setErrorMessage("A Java Development Kit (JDK) is required to use Liberty dev mode.");
            return false;
        }
        return true;
    }

    /**
     * Resolves the java installation to use based on the configuration.
     */
    public static String resolveJavaHome(ILaunchConfiguration configuration) {
        IVMInstall install;
        String keyValue = null;

        // The JRE_CONTAINER_KEY is set when using the configuration's execution environment
        // or an alternate JRE. If this is not set, the workspace default JRE is used.
        try {
            ILaunchConfigurationWorkingCopy configWorkingCopy = configuration.getWorkingCopy();
            keyValue = configWorkingCopy.getAttribute(JRE_CONTAINER_KEY, (String) null);
        } catch (Exception e) {
            String msg = "Unable to resolve the Java installation path using configuration." + configuration.getName()
                    + ". Using the workspace Java installation";
            if (Trace.isEnabled()) {
                Trace.getTracer().trace(Trace.TRACE_UI, msg, e);
            }
            ErrorHandler.processWarningMessage(msg, e);
        }

        if (keyValue != null) {
            IPath javaPath = org.eclipse.core.runtime.Path.fromOSString(keyValue);
            install = JavaRuntime.getVMInstall(javaPath);
        } else {
            install = JavaRuntime.getDefaultVMInstall();
        }

        return install.getInstallLocation().getAbsolutePath();
    }
}
