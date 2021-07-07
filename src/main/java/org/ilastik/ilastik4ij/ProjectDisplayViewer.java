package org.ilastik.ilastik4ij;

import org.scijava.display.Display;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UserInterface;
import org.scijava.ui.swing.SwingUI;
import org.scijava.ui.viewer.AbstractDisplayViewer;
import org.scijava.ui.viewer.DisplayViewer;
import org.scijava.ui.viewer.DisplayWindow;

@Plugin(type = DisplayViewer.class)
public class ProjectDisplayViewer extends AbstractDisplayViewer<Project> {
    @Override
    public boolean isCompatible(UserInterface ui) {
        return ui instanceof SwingUI;
    }

    @Override
    public boolean canView(Display<?> d) {
        return d instanceof ProjectDisplay;
    }

    @Override
    public void view(DisplayWindow w, Display<?> d) {
        super.view(w, d);
        setPanel(new ProjectDisplayPanel(getDisplay(), w));
    }

    @Override
    public ProjectDisplay getDisplay() {
        return (ProjectDisplay) super.getDisplay();
    }
}
