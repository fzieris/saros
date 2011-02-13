package de.fu_berlin.inf.dpp.stf.client.testProject.testsuits.invitation.permutations;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.RemoteException;

import org.eclipse.core.runtime.CoreException;
import org.junit.BeforeClass;
import org.junit.Test;

import de.fu_berlin.inf.dpp.stf.client.testProject.testsuits.STFTest;
import de.fu_berlin.inf.dpp.stf.server.rmiSarosSWTBot.eclipse.workbench.finder.remoteWidgets.Shell;

public class TestParallelInvitationWithTerminationByInvitees extends STFTest {

    /**
     * Preconditions:
     * <ol>
     * <li>Alice (Host, Write Access)</li>
     * <li>Bob (Read-Only Access)</li>
     * <li>Carl (Read-Only Access)</li>
     * <li>Dave (Read-Only Access)</li>
     * <li>Edna (Read-Only Access)</li>
     * </ol>
     * 
     * @throws AccessException
     * @throws RemoteException
     * 
     */

    @BeforeClass
    public static void runBeforeClass() throws RemoteException {
        initTesters(TypeOfTester.ALICE, TypeOfTester.BOB, TypeOfTester.CARL,
            TypeOfTester.DAVE, TypeOfTester.EDNA);
        setUpWorkbench();
        setUpSaros();
    }

    /**
     * Steps:
     * <ol>
     * <li>Alice invites everyone else simultaneously.</li>
     * <li>Bob cancels the invitation at the beginning.</li>
     * <li>Carl cancels the invitation after receiving the file list, but before
     * the synchronisation starts.</li>
     * <li>Dave cancels the invitation during the synchronisation.</li>
     * <li>Edna accepts the invitation (using a new project).</li>
     * <li>Edna leaves the session.</li>
     * </ol>
     * 
     * Result:
     * <ol>
     * <li>Alice is notified of the peer canceling the invitation.</li>
     * <li>Alice is notified of Edna joining the session.</li>
     * <li></li>
     * <li>Alice is notified of Edna leaving the session.</li> *
     * </ol>
     * 
     * @throws CoreException
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void parallelInvitationWihtTerminationByInvitees()
        throws IOException, CoreException, InterruptedException {
        alice.fileM.newJavaProjectWithClasses(PROJECT1, PKG1, CLS1);

        /*
         * build session with bob, carl and dave simultaneously
         */
        alice.sarosC.shareProject(PROJECT1, bob.getBaseJid(),
            dave.getBaseJid(), carl.getBaseJid(), edna.getBaseJid());

        Shell shell_bob = bob.bot().shell(SHELL_SESSION_INVITATION);
        shell_bob.activateAndWait();
        shell_bob.bot_().button(CANCEL).click();

        Shell shell_alice = alice.bot().shell(SHELL_PROBLEM_OCCURRED);
        shell_alice.waitUntilActive();
        assertTrue(alice.sarosC.getSecondLabelOfShellProblemOccurred().matches(
            bob.getName() + ".*"));
        shell_alice.bot_().button(OK).click();

        Shell shell_carl = carl.bot().shell(SHELL_SESSION_INVITATION);
        carl.bot().waitUntilShellOpen(SHELL_SESSION_INVITATION);
        shell_carl.activate();
        carl.sarosC.confirmShellSessionnInvitation();
        shell_carl.bot_().button(CANCEL).click();

        shell_alice.waitUntilActive();
        assertTrue(alice.sarosC.getSecondLabelOfShellProblemOccurred().matches(
            carl.getName() + ".*"));
        shell_alice.bot_().button(OK).click();

        Shell shell_dave = dave.bot().shell(SHELL_SESSION_INVITATION);
        shell_dave.activateAndWait();
        dave.sarosC.confirmShellSessionnInvitation();
        shell_dave.bot_().button(CANCEL).click();

        shell_alice.waitUntilActive();
        assertTrue(alice.sarosC.getSecondLabelOfShellProblemOccurred().matches(
            dave.getName() + ".*"));
        shell_alice.bot_().button(OK).click();

        edna.bot().waitUntilShellOpen(SHELL_SESSION_INVITATION);
        edna.bot().shell(SHELL_SESSION_INVITATION).activate();
        edna.sarosC.confirmShellSessionnInvitation();
        edna.sarosC.confirmShellAddProjectWithNewProject(PROJECT1);
        edna.sarosSessionV.leaveTheSessionByPeer();
        assertFalse(edna.sarosSessionV.isInSession());
        assertFalse(alice.sarosSessionV.hasReadOnlyAccessNoGUI(edna.jid));
    }
}
