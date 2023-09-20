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
 * https://github.com/jbosstools/jbosstools-quarkus/blob/main/plugins/org.jboss.tools.quarkus.lsp4e/src/org/jboss/tools/quarkus/lsp4e/QuarkusLanguageClient.java
 * with modifications made for the Liberty Tools Microprofile LS plugin
 *
 */
package io.openliberty.tools.eclipse.mpls;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.lsp4e.LanguageClientImpl;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;
import org.eclipse.lsp4mp.commons.codeaction.CodeActionResolveData;
import org.eclipse.lsp4mp.commons.JavaCursorContextResult;
import org.eclipse.lsp4mp.commons.JavaFileInfo;
import org.eclipse.lsp4mp.commons.MicroProfileDefinition;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeActionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCodeLensParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCompletionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaCompletionResult;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDefinitionParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaDiagnosticsParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaFileInfoParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaHoverParams;
import org.eclipse.lsp4mp.commons.MicroProfileJavaProjectLabelsParams;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfo;
import org.eclipse.lsp4mp.commons.MicroProfileProjectInfoParams;
import org.eclipse.lsp4mp.commons.MicroProfilePropertyDefinitionParams;
import org.eclipse.lsp4mp.commons.ProjectLabelInfoEntry;
import org.eclipse.lsp4mp.commons.utils.JSONUtility;
import org.eclipse.lsp4mp.jdt.core.IMicroProfilePropertiesChangedListener;
import org.eclipse.lsp4mp.jdt.core.MicroProfileCorePlugin;
import org.eclipse.lsp4mp.jdt.core.ProjectLabelManager;
import org.eclipse.lsp4mp.jdt.core.PropertiesManager;
import org.eclipse.lsp4mp.jdt.core.PropertiesManagerForJava;
import org.eclipse.lsp4mp.ls.api.MicroProfileLanguageClientAPI;
import org.eclipse.lsp4mp.ls.api.MicroProfileLanguageServerAPI;

import io.openliberty.tools.eclipse.ls.plugin.LibertyToolsLSPlugin;

/**
 * Liberty Devex MicroProfile language client.
 * 
 * @author
 */
public class LibertyMPLSClientImpl extends LanguageClientImpl implements MicroProfileLanguageClientAPI {

    private static IMicroProfilePropertiesChangedListener SINGLETON_LISTENER;

    private IMicroProfilePropertiesChangedListener listener = event -> {
        ((MicroProfileLanguageServerAPI) getLanguageServer()).propertiesChanged(event);
    };

    public LibertyMPLSClientImpl() {
        if (SINGLETON_LISTENER != null) {
            MicroProfileCorePlugin.getDefault().removeMicroProfilePropertiesChangedListener(SINGLETON_LISTENER);
        }
        SINGLETON_LISTENER = listener;
        MicroProfileCorePlugin.getDefault().addMicroProfilePropertiesChangedListener(listener);
    }

    @Override
    public CompletableFuture<MicroProfileProjectInfo> getProjectInfo(MicroProfileProjectInfoParams params) {
        return CompletableFutures.computeAsync((cancelChecker) -> {
            final MicroProfileProjectInfo[] projectInfo = new MicroProfileProjectInfo[1];
            Job job = Job.create("MicroProfile properties collector", (ICoreRunnable) monitor -> {
                projectInfo[0] = PropertiesManager.getInstance().getMicroProfileProjectInfo(params, JDTUtilsLTEImpl.getInstance(), monitor);
            });
            job.schedule();
            try {
                job.join();
            } catch (InterruptedException e) {
                LibertyToolsLSPlugin.logException(e.getLocalizedMessage(), e);
            }
            return projectInfo[0];
        });

    }

    private IProgressMonitor getProgressMonitor(CancelChecker cancelChecker) {
        IProgressMonitor monitor = new NullProgressMonitor() {
            public boolean isCanceled() {
                cancelChecker.checkCanceled();
                return false;
            };
        };
        return monitor;
    }

    @Override
    public CompletableFuture<Location> getPropertyDefinition(MicroProfilePropertyDefinitionParams params) {

        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<List<? extends CodeLens>> getJavaCodelens(MicroProfileJavaCodeLensParams javaParams) {
        return CompletableFutures.computeAsync((cancelChecker) -> {
            IProgressMonitor monitor = getProgressMonitor(cancelChecker);
            try {
                return PropertiesManagerForJava.getInstance().codeLens(javaParams, JDTUtilsLTEImpl.getInstance(), monitor);
            } catch (JavaModelException e) {
                LibertyToolsLSPlugin.logException(e.getLocalizedMessage(), e);
                return Collections.emptyList();
            }
        });
    }

    @Override
    public CompletableFuture<List<PublishDiagnosticsParams>> getJavaDiagnostics(MicroProfileJavaDiagnosticsParams javaParams) {
        return CompletableFutures.computeAsync((cancelChecker) -> {
            IProgressMonitor monitor = getProgressMonitor(cancelChecker);
            try {
                return PropertiesManagerForJava.getInstance().diagnostics(javaParams, JDTUtilsLTEImpl.getInstance(), monitor);
            } catch (JavaModelException e) {
                LibertyToolsLSPlugin.logException(e.getLocalizedMessage(), e);
                return Collections.emptyList();
            }
        });
    }

    @Override
    public CompletableFuture<List<CodeAction>> getJavaCodeAction(MicroProfileJavaCodeActionParams javaParams) {
        return CompletableFutures.computeAsync((cancelChecker) -> {
            IProgressMonitor monitor = getProgressMonitor(cancelChecker);
            try {
                return (List<CodeAction>) PropertiesManagerForJava.getInstance().codeAction(javaParams, JDTUtilsLTEImpl.getInstance(),
                        monitor);
            } catch (JavaModelException e) {
                LibertyToolsLSPlugin.logException(e.getLocalizedMessage(), e);
                return Collections.emptyList();
            }
        });
    }

    @Override
    public CompletableFuture<List<ProjectLabelInfoEntry>> getAllJavaProjectLabels() {
        return CompletableFutures.computeAsync((cancelChecker) -> {
            IProgressMonitor monitor = getProgressMonitor(cancelChecker);
            return ProjectLabelManager.getInstance().getProjectLabelInfo();
        });
    }
    
    @Override
    public CompletableFuture<ProjectLabelInfoEntry> getJavaProjectLabels(MicroProfileJavaProjectLabelsParams javaParams) {
        return CompletableFutures.computeAsync((cancelChecker) -> {
            IProgressMonitor monitor = getProgressMonitor(cancelChecker);
            return ProjectLabelManager.getInstance().getProjectLabelInfo(javaParams, JDTUtilsLTEImpl.getInstance(), monitor);
        });
    }

    @Override
    public CompletableFuture<JavaFileInfo> getJavaFileInfo(MicroProfileJavaFileInfoParams javaParams) {
        return CompletableFutures.computeAsync(cancelChecker -> {
            IProgressMonitor monitor = getProgressMonitor(cancelChecker);
            return PropertiesManagerForJava.getInstance().fileInfo(javaParams, JDTUtilsLTEImpl.getInstance(), monitor);
        });
    }

    @Override
    public CompletableFuture<List<MicroProfileDefinition>> getJavaDefinition(MicroProfileJavaDefinitionParams javaParams) {
        return CompletableFutures.computeAsync(cancelChecker -> {
            IProgressMonitor monitor = getProgressMonitor(cancelChecker);
            try {
                return PropertiesManagerForJava.getInstance().definition(javaParams, JDTUtilsLTEImpl.getInstance(), monitor);
            } catch (JavaModelException e) {
                LibertyToolsLSPlugin.logException(e.getLocalizedMessage(), e);
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<MicroProfileJavaCompletionResult> getJavaCompletion(MicroProfileJavaCompletionParams javaParams) {
        return CompletableFutures.computeAsync(cancelChecker -> {
            IProgressMonitor monitor = getProgressMonitor(cancelChecker);
            try {
                CompletionList completionList = PropertiesManagerForJava.getInstance().completion(javaParams, JDTUtilsLTEImpl.getInstance(), monitor);
                JavaCursorContextResult javaCursorContext = PropertiesManagerForJava.getInstance().javaCursorContext(javaParams, JDTUtilsLTEImpl.getInstance(), monitor);               
                return new MicroProfileJavaCompletionResult(completionList, javaCursorContext); 
            } catch (JavaModelException e) {
                LibertyToolsLSPlugin.logException(e.getLocalizedMessage(), e);
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<Hover> getJavaHover(MicroProfileJavaHoverParams javaParams) {
        return CompletableFutures.computeAsync((cancelChecker) -> {
            IProgressMonitor monitor = getProgressMonitor(cancelChecker);
            try {
                return PropertiesManagerForJava.getInstance().hover(javaParams, JDTUtilsLTEImpl.getInstance(), monitor);
            } catch (JavaModelException e) {
                LibertyToolsLSPlugin.logException(e.getLocalizedMessage(), e);
                return null;
            }
        });
    }

    @Override
    public CompletableFuture<CodeAction> resolveCodeAction(CodeAction unresolved) {
        return CompletableFutures.computeAsync((cancelChecker) -> {
            try {
                IProgressMonitor monitor = getProgressMonitor(cancelChecker);
                // Deserialize CodeAction#data which is a JSonObject to CodeActionResolveData
                CodeActionResolveData resolveData = JSONUtility.toModel(unresolved.getData(), CodeActionResolveData.class);
                unresolved.setData(resolveData);
                return PropertiesManagerForJava.getInstance().resolveCodeAction(unresolved, JDTUtilsLTEImpl.getInstance(), monitor);
            } catch (JavaModelException e) {
                LibertyToolsLSPlugin.logException(e.getLocalizedMessage(), e);
                return null;
            }
        });
    }
}
