package org.jopenchart;

import java.awt.Color;

public class Label {
    private String label;

    private Color color = Color.GRAY;

    public Label(String label) {
        this.label = label;
    }

    public final String getLabel() {
        return label;
    }

    public final void setLabel(String label) {
        this.label = label;
    }

    @Override
    public String toString() {
        return this.label;
    }

    public Color getColor() {
        return this.color;
    }

    public void setColor(Color color) {
        this.color = color;
    }
}
