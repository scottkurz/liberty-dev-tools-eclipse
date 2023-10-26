/******************************************************************************* 
 * Copyright (c) 2019 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the 
 * Eclipse Public License v1.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v10.html 
 * 
 * Contributors: 
 * Red Hat, Inc. - initial API and implementation 
 ******************************************************************************/
/**
 * This class is a copy/paste of jbosstools-quarkus language server plugin
 * https://github.com/jbosstools/jbosstools-quarkus/blob/main/plugins/org.jboss.tools.quarkus.lsp4e/src/org/jboss/tools/quarkus/lsp4e/QuarkusLanguageServer.java
 * with modifications made for the Liberty Tools Microprofile LS plugin
 *
 */

package io.openliberty.tools.eclipse.mpls;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.lsp4e.server.ProcessStreamConnectionProvider;

import io.openliberty.tools.eclipse.ls.plugin.LibertyToolsLSPlugin;
import io.openliberty.tools.eclipse.lsclient.DebugUtil;

public class LibertyMPLSConnection extends ProcessStreamConnectionProvider {

    public LibertyMPLSConnection() {
        List<String> commands = new ArrayList<>();
        commands.add(computeJavaPath());
        String debugArg = DebugUtil.getDebugJVMArg(getClass().getName());
        if (debugArg.length() > 0) {
            commands.add(debugArg);
        }
        commands.add("-classpath");
        try {
            commands.add(computeClasspath());
            commands.add("org.eclipse.lsp4mp.ls.MicroProfileServerLauncher");
            setCommands(commands);
            setWorkingDirectory(System.getProperty("user.dir"));
        } catch (IOException e) {
            LibertyToolsLSPlugin.getDefault().getLog()
                    .log(new Status(IStatus.ERROR, LibertyToolsLSPlugin.getDefault().getBundle().getSymbolicName(), e.getMessage(), e));
        }
    }

    private String computeClasspath() throws IOException {
        StringBuilder builder = new StringBuilder();
        URL url = FileLocator.toFileURL(getClass().getResource("/server/mp-langserver/org.eclipse.lsp4mp.ls.jar"));
        builder.append(new java.io.File(url.getPath()).getAbsolutePath());
        return builder.toString();
    }

    private String computeJavaPath() {
        File f = new File(System.getProperty("java.home"), "bin/java" + (Platform.getOS().equals(Platform.OS_WIN32) ? ".exe" : ""));
        return f.getAbsolutePath();
    }

    @Override
    public Object getInitializationOptions(URI rootUri) {

        Map<String, Object> root = new HashMap<>();
        Map<String, Object> settings = new HashMap<>();
        Map<String, Object> microprofile = new HashMap<>();
        Map<String, Object> tools = new HashMap<>();
        Map<String, Object> trace = new HashMap<>();
        trace.put("server", "verbose");
        tools.put("trace", trace);
        Map<String, Object> codeLens = new HashMap<>();
        codeLens.put("urlCodeLensEnabled", "true");
        tools.put("codeLens", codeLens);
        microprofile.put("tools", tools);
        settings.put("microprofile", microprofile);
        root.put("settings", settings);
        Map<String, Object> extendedClientCapabilities = new HashMap<>();
        Map<String, Object> commands = new HashMap<>();
        Map<String, Object> commandsKind = new HashMap<>();
        commandsKind.put("valueSet", Arrays.asList("microprofile.command.configuration.update", "microprofile.command.open.uri"));
        commands.put("commandsKind", commandsKind);
        extendedClientCapabilities.put("commands", commands);
        extendedClientCapabilities.put("completion", new HashMap<>());
        extendedClientCapabilities.put("shouldLanguageServerExitOnShutdown", Boolean.TRUE);
        root.put("extendedClientCapabilities", extendedClientCapabilities);
        return root;
    }

    @Override
    public String toString() {
        return "Liberty MP Language Server: " + super.toString();
    }

}
