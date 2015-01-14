package org.openconcerto.modules.label;

import java.awt.Graphics;

import org.openconcerto.sql.model.SQLRowAccessor;

public interface LabelRenderer {
    public void paintLabel(Graphics g, SQLRowAccessor row, int x, int y, int gridWith, int gridHeight);
}
