package org.openconcerto.modules.extensionbuilder;

import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

public abstract class AbstractSplittedPanel extends JPanel {
    protected JSplitPane split;
    protected JComponent leftComponent;
    protected Extension extension;

    public AbstractSplittedPanel(Extension extension) {
        this.extension = extension;
        this.setLayout(new GridLayout(1, 1));
        this.setOpaque(false);
        leftComponent = createLeftComponent();
        split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftComponent, new JPanel());
        this.add(split);
    }

    public void setRightPanel(JComponent p) {
        this.invalidate();
        split.setRightComponent(p);
        this.revalidate();
        this.repaint();
    }

    public abstract JComponent createLeftComponent();

}
