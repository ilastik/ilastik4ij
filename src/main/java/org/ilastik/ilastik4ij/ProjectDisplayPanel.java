package org.ilastik.ilastik4ij;

import org.scijava.ui.swing.viewer.SwingDisplayPanel;
import org.scijava.ui.viewer.DisplayWindow;

import javax.swing.*;
import java.awt.*;

public class ProjectDisplayPanel extends SwingDisplayPanel {
    private final ProjectDisplay display;
    private final DisplayWindow window;

    private final JLabel pathLabel;
    private final JLabel typeLabel;

    public ProjectDisplayPanel(ProjectDisplay display, DisplayWindow window) {
        this.display = display;
        this.window = window;

        int fontSize = getFont().getSize();
        setBorder(BorderFactory.createEmptyBorder(fontSize, fontSize, fontSize, fontSize));
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        pathLabel = new JLabel();
        add(pathLabel);
        Font font = pathLabel.getFont();
        pathLabel.setFont(new Font(font.getName(), font.getStyle(), font.getSize() + 4));

        typeLabel = new JLabel();
        add(typeLabel);

        window.setContent(this);
    }

    @Override
    public ProjectDisplay getDisplay() {
        return display;
    }

    @Override
    public DisplayWindow getWindow() {
        return window;
    }

    @Override
    public void redoLayout() {
    }

    @Override
    public void setLabel(String s) {
    }

    @Override
    public void redraw() {
        Project project = getProject();
        if (project == null) {
            return;
        }

        pathLabel.setText(project.path.getFileName().toString());
        pathLabel.setToolTipText(project.path.toString());

        typeLabel.setText(project.type.name);

        window.pack();
    }

    public Project getProject() {
        return display.isEmpty() ? null : display.get(0);
    }
}
