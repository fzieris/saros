package de.fu_berlin.inf.dpp.intellij.ui.actions;

import de.fu_berlin.inf.dpp.intellij.ui.util.SafeDialogUtils;

/**
 * Class used to report that an action is not implemented yet.
 */
public class NotImplementedAction extends AbstractSarosAction {

    private String name;

    public NotImplementedAction(String name) {
        this.name = name;
    }

    @Override
    public String getActionName() {
        return name;
    }

    @Override
    public void execute() {
        LOG.info("Not implemented action [" + name + "]");

        SafeDialogUtils.showError(
            "We are sorry, but action [" + name + "] not implemented yet!",
            "Not Implemented");

        actionPerformed();
    }
}
