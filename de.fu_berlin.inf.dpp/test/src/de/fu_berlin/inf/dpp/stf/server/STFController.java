package de.fu_berlin.inf.dpp.stf.server;

import java.rmi.AccessException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

import org.apache.log4j.Logger;

import de.fu_berlin.inf.dpp.Saros;
import de.fu_berlin.inf.dpp.accountManagement.XMPPAccountStore;
import de.fu_berlin.inf.dpp.editor.EditorManager;
import de.fu_berlin.inf.dpp.feedback.FeedbackManager;
import de.fu_berlin.inf.dpp.net.internal.DataTransferManager;
import de.fu_berlin.inf.dpp.project.SarosSessionManager;
import de.fu_berlin.inf.dpp.stf.client.Tester;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.finder.STFBotImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.finder.STFWorkbenchBotImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.finder.remoteWidgets.EditorImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.finder.remoteWidgets.STFButtonImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.finder.remoteWidgets.STFLabelImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.finder.remoteWidgets.STFListImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.finder.remoteWidgets.STFMenuImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.finder.remoteWidgets.STFShellImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.finder.remoteWidgets.STFTableImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.finder.remoteWidgets.STFTextImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.finder.remoteWidgets.STFToolbarButtonImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.finder.remoteWidgets.STFTreeImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.finder.remoteWidgets.STFTreeItemImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.finder.remoteWidgets.STFViewImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.sarosFinder.remoteComponents.EclipseComponentImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.sarosFinder.remoteComponents.WorkbenchImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.sarosFinder.remoteComponents.contextMenu.OpenCImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.sarosFinder.remoteComponents.contextMenu.SarosCImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.sarosFinder.remoteComponents.contextMenu.TeamCImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.sarosFinder.remoteComponents.menuBar.EditMImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.sarosFinder.remoteComponents.menuBar.FileMImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.sarosFinder.remoteComponents.menuBar.RefactorMImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.sarosFinder.remoteComponents.menuBar.SarosMImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.sarosFinder.remoteComponents.menuBar.WindowMImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.sarosFinder.remoteComponents.views.ConsoleViewImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.sarosFinder.remoteComponents.views.PEViewImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.sarosFinder.remoteComponents.views.ProgressViewImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.sarosFinder.remoteComponents.views.sarosViews.ChatViewImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.sarosFinder.remoteComponents.views.sarosViews.RSViewImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.sarosFinder.remoteComponents.views.sarosViews.RosterViewImp;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.sarosFinder.remoteComponents.views.sarosViews.SessionViewImp;

/**
 * SarosRmiSWTWorkbenchBot controls Eclipse Saros from the GUI perspective. It
 * exports {@link SarosState} via RMI. You should not use this within tests.
 * Have a look at {@link Tester} if you want to write tests.
 * 
 */
public class STFController {

    private static final transient Logger log = Logger
        .getLogger(STFController.class);

    public static int sleepTime = 750;

    /** The RMI registry used, is not exported */
    private static transient Registry registry;

    /*
     * sometimes when connecting to a server i'm getting error:
     * java.rmi.NoSuchObjectException:no Such object in table. This happens when
     * the remote object the stub refers to has been DGC'd and GC's locally. My
     * solution is keeping a static references "classVariable" to the object in
     * the object in the server JVM.
     */
    public static void exportedObjects(int port, Saros saros,
        SarosSessionManager sessionManager,
        DataTransferManager dataTransferManager, EditorManager editorManager,
        XMPPAccountStore xmppAccountStore, FeedbackManager feedbackManager)
        throws RemoteException {

        EclipseComponentImp.saros = saros;
        EclipseComponentImp.sessionManager = sessionManager;
        EclipseComponentImp.dataTransferManager = dataTransferManager;
        EclipseComponentImp.editorManager = editorManager;
        EclipseComponentImp.xmppAccountStore = xmppAccountStore;
        EclipseComponentImp.feedbackManager = feedbackManager;
        try {
            registry = LocateRegistry.createRegistry(port);
        } catch (RemoteException e) {
            registry = LocateRegistry.getRegistry(port);
        }

        exportObject(STFBotImp.getInstance(), "stfBot");
        exportObject(STFWorkbenchBotImp.getInstance(), "bot");
        exportObject(STFShellImp.getInstance(), "shell");
        exportObject(STFTableImp.getInstance(), "table");
        exportObject(STFTreeImp.getInstance(), "tree");
        exportObject(STFTreeItemImp.getInstance(), "treeItem");
        exportObject(STFButtonImp.getInstance(), "button");
        exportObject(STFToolbarButtonImp.getInstance(), "toolbarButton");
        exportObject(STFViewImp.getInstance(), "view");
        exportObject(STFMenuImp.getInstance(), "menu");
        exportObject(STFLabelImp.getInstance(), "label");
        exportObject(STFTextImp.getInstance(), "text");
        exportObject(STFListImp.getInstance(), "list");
        exportObject(PEViewImp.getInstance(), "packageExplorerView");
        exportObject(ProgressViewImp.getInstance(), "progressView");
        exportObject(EditorImp.getInstance(), "eclipseEditor");
        exportObject(RosterViewImp.getInstance(), "rosterView");
        exportObject(SessionViewImp.getInstance(), "sessionView");
        exportObject(RSViewImp.getInstance(), "remoteScreenView");
        exportObject(ChatViewImp.getInstance(), "chatView");
        exportObject(WorkbenchImp.getInstance(), "workbench");
        exportObject(ConsoleViewImp.getInstance(), "consoleView");
        exportObject(FileMImp.getInstance(), "fileM");
        exportObject(EditMImp.getInstance(), "editM");
        exportObject(RefactorMImp.getInstance(), "refactorM");
        exportObject(WindowMImp.getInstance(), "windowM");
        exportObject(SarosMImp.getInstance(), "sarosM");
        exportObject(TeamCImp.getInstance(), "team");
        exportObject(SarosCImp.getInstance(), "saros");
        exportObject(OpenCImp.getInstance(), "open");
    }

    /**
     * Add a shutdown hook to unbind exported Object from registry.
     */
    private static void addShutdownHook(final String name) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    if (registry != null && name != null)
                        registry.unbind(name);
                } catch (RemoteException e) {
                    log.warn("Failed to unbind: " + name, e);
                } catch (NotBoundException e) {
                    log.warn("Failed to unbind: " + name, e);
                }
            }
        });
    }

    /**
     * Export object by given name on our local RMI Registry.
     */
    private static Remote exportObject(Remote exportedObject, String exportName) {
        try {
            Remote remoteObject = UnicastRemoteObject.exportObject(
                exportedObject, 0);
            addShutdownHook(exportName);
            registry.bind(exportName, remoteObject);
            return remoteObject;
        } catch (RemoteException e) {
            log.error("Could not export the object " + exportName, e);
        } catch (AlreadyBoundException e) {
            log.error("Could not bind the object " + exportName
                + ", because it is bound already.", e);
        }
        return null;
    }

    public static void listRmiObjects() {
        try {
            for (String s : registry.list())
                log.debug("registered Object: " + s);
        } catch (AccessException e) {
            log.error("Failed on access", e);
        } catch (RemoteException e) {
            log.error("Failed", e);
        }
    }
}
