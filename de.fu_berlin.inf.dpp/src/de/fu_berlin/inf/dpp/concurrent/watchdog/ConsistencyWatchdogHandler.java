package de.fu_berlin.inf.dpp.concurrent.watchdog;

import java.util.List;
import java.util.concurrent.CancellationException;

import org.apache.log4j.Logger;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.picocontainer.Startable;

import de.fu_berlin.inf.dpp.activities.ChecksumActivity;
import de.fu_berlin.inf.dpp.activities.ChecksumErrorActivity;
import de.fu_berlin.inf.dpp.activities.RecoveryFileActivity;
import de.fu_berlin.inf.dpp.activities.SPath;
import de.fu_berlin.inf.dpp.annotations.Component;
import de.fu_berlin.inf.dpp.editor.EditorManager;
import de.fu_berlin.inf.dpp.filesystem.EclipseFileImpl;
import de.fu_berlin.inf.dpp.session.AbstractActivityConsumer;
import de.fu_berlin.inf.dpp.session.AbstractActivityProducer;
import de.fu_berlin.inf.dpp.session.IActivityConsumer;
import de.fu_berlin.inf.dpp.session.ISarosSession;
import de.fu_berlin.inf.dpp.session.User;
import de.fu_berlin.inf.dpp.synchronize.StartHandle;
import de.fu_berlin.inf.dpp.synchronize.UISynchronizer;
import de.fu_berlin.inf.dpp.util.FileUtils;
import de.fu_berlin.inf.dpp.util.ThreadUtils;

/**
 * This component is responsible for handling Consistency Errors on the host. It
 * both produces and consumes activities.
 */
@Component(module = "consistency")
public final class ConsistencyWatchdogHandler extends AbstractActivityProducer
    implements Startable {

    private static Logger LOG = Logger
        .getLogger(ConsistencyWatchdogHandler.class);

    private final EditorManager editorManager;

    private final ISarosSession session;

    private final UISynchronizer synchronizer;

    private final IActivityConsumer consumer = new AbstractActivityConsumer() {
        @Override
        public void receive(ChecksumErrorActivity checksumError) {
            if (session.isHost())
                triggerRecovery(checksumError);
        }
    };

    @Override
    public void start() {
        session.addActivityConsumer(consumer);
        session.addActivityProducer(this);
    }

    @Override
    public void stop() {
        session.removeActivityConsumer(consumer);
        session.removeActivityProducer(this);
    }

    public ConsistencyWatchdogHandler(final ISarosSession session,
        final EditorManager editorManager, final UISynchronizer synchronizer) {
        this.session = session;
        this.editorManager = editorManager;
        this.synchronizer = synchronizer;
    }

    private void triggerRecovery(final ChecksumErrorActivity checksumError) {

        LOG.debug("received Checksum Error: " + checksumError);

        /*
         * fork a thread as this is normally called from the UI thread and so we
         * would not be able to receive lock confirmations and so the blocking
         * the session would cause a timeout in the StopManager
         */

        /*
         * TODO ensure that only one recovery is run at the same time ? i.e use
         * a single ThreadWorker ?
         */
        ThreadUtils.runSafeAsync(LOG, new Runnable() {
            @Override
            public void run() {
                runRecovery(checksumError);
            }
        });
    }

    private void runRecovery(final ChecksumErrorActivity checksumError)
        throws CancellationException {

        List<StartHandle> startHandles = null;

        try {

            startHandles = session.getStopManager().stop(session.getUsers(),
                "Consistency recovery");

            recoverFiles(checksumError);

            /*
             * We have to start the StartHandle of the inconsistent user first
             * (blocking!) because otherwise the other participants can be
             * started before the inconsistent user completely processed the
             * consistency recovery.
             */

            // find the StartHandle of the inconsistent user
            StartHandle inconsistentStartHandle = null;
            for (StartHandle startHandle : startHandles) {
                if (checksumError.getSource().equals(startHandle.getUser())) {
                    inconsistentStartHandle = startHandle;
                    break;
                }
            }
            if (inconsistentStartHandle == null) {
                LOG.error("could not find start handle"
                    + " of the inconsistent user");
            } else {
                // FIXME evaluate the return value
                inconsistentStartHandle.startAndAwait();
                startHandles.remove(inconsistentStartHandle);
            }
        } finally {
            if (startHandles != null)
                for (StartHandle startHandle : startHandles)
                    startHandle.start();
        }
    }

    private void recoverFiles(final ChecksumErrorActivity checksumError) {

        synchronizer.syncExec(new Runnable() {
            @Override
            public void run() {

                for (final SPath path : checksumError.getPaths()) {

                    recoverFile(checksumError.getSource(), path);

                    // Tell the user that we sent all files
                    fireActivity(new ChecksumErrorActivity(session
                        .getLocalUser(), checksumError.getSource(), null,
                        checksumError.getRecoveryID()));

                }
            }
        });
    }

    /**
     * Recover a single file for the given user (that is either send the file or
     * tell the user to remove it).
     */
    private void recoverFile(final User from, final SPath path) {

        final IFile file = ((EclipseFileImpl) path.getFile()).getDelegate();

        // Reset jupiter
        session.getConcurrentDocumentServer().reset(from, path);

        final User user = session.getLocalUser();

        if (!file.exists()) {
            // Tell the client to delete the file
            fireActivity(RecoveryFileActivity.removed(user, path, from, null));
            fireActivity(ChecksumActivity.missing(user, path));
            return;
        }

        /*
         * save the editor the dirty contents are flushed to the underlying
         * storage
         */
        editorManager.saveLazy(path);

        String charset = null;

        try {
            charset = file.getCharset();
        } catch (CoreException e) {
            LOG.warn("could not determine encoding for file: " + file, e);
        }

        byte[] content = FileUtils.getLocalFileContent(file);

        if (content == null) {
            LOG.error("could not read file: " + file);
            return;
        }

        fireActivity(RecoveryFileActivity.created(user, path, content, from,
            charset));

        /*
         * Immediately follow up with a new checksum activity so that the remote
         * side can verify the recovered file.
         */

        DocumentChecksum checksum = new DocumentChecksum(path);
        checksum.update(editorManager.getContent(path));

        fireActivity(new ChecksumActivity(user, path, checksum.getHash(),
            checksum.getLength(), null));
    }
}
