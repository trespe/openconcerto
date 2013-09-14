package org.openconcerto.modules.extensionbuilder;

import java.awt.GridLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

public class ExtensionListPanel extends JPanel {
    private JPanel rPanel = new JPanel();
    private JSplitPane split;
    private ExtensionBuilderModule extensionBuilderModule;
    final ExtensionMainListPanel newLeftComponent;

    public ExtensionListPanel(ExtensionBuilderModule extensionBuilderModule) {
        if (extensionBuilderModule == null) {
            throw new IllegalArgumentException("null ExtensionBuilderModule");
        }
        this.extensionBuilderModule = extensionBuilderModule;
        this.setLayout(new GridLayout(1, 1));
        newLeftComponent = new ExtensionMainListPanel(this);
        this.split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, newLeftComponent, rPanel);
        this.add(this.split);
        newLeftComponent.fill();
    }

    public void setRightPanel(JComponent p) {
        this.invalidate();
        this.split.setRightComponent(p);
        this.revalidate();
        this.repaint();
    }

    public ExtensionBuilderModule getExtensionBuilderModule() {
        return extensionBuilderModule;
    }

    public void modelChanged() {
        newLeftComponent.modelChanged();
    }

}
