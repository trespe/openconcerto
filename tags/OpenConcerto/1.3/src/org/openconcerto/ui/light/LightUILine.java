/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2011 OpenConcerto, by ILM Informatique. All rights reserved.
 * 
 * The contents of this file are subject to the terms of the GNU General Public License Version 3
 * only ("GPL"). You may not use this file except in compliance with the License. You can obtain a
 * copy of the License at http://www.gnu.org/licenses/gpl-3.0.html See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each file.
 */
 
 package org.openconcerto.ui.light;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class LightUILine implements Serializable {

    public static final int ALIGN_GRID = 0;
    public static final int ALIGN_LEFT = 1;
    public static final int ALIGN_RIGHT = 2;

    private static final long serialVersionUID = 4132718509484530435L;
    private int weightY;
    private boolean fillHeight;
    private int gridAlignment = ALIGN_GRID;

    private List<LightUIElement> elements = new ArrayList<LightUIElement>();

    public int getSize() {
        return elements.size();
    }

    public void add(LightUIElement element) {
        elements.add(element);
    }

    public void dump(PrintStream out) {
        int size = elements.size();
        out.println("LightUILine " + size + " elements, weightY: " + weightY + " fillHeight: " + fillHeight);
        for (int i = 0; i < size; i++) {
            LightUIElement element = elements.get(i);
            out.print("Element " + i + " : ");
            element.dump(out);
        }
    }

    public int getWidth() {
        int w = 0;
        final int size = elements.size();
        for (int i = 0; i < size; i++) {
            w += elements.get(i).getGridWidth();
        }
        return w;
    }

    public int getWeightY() {
        return weightY;
    }

    public void setWeightY(int weightY) {
        this.weightY = weightY;
    }

    public boolean isFillHeight() {
        return fillHeight;
    }

    public void setFillHeight(boolean fillHeight) {
        this.fillHeight = fillHeight;
    }

    public LightUIElement getElement(int i) {
        return elements.get(i);
    }

    public int getGridAlignment() {
        return gridAlignment;
    }

    public void setGridAlignment(int gridAlignment) {
        this.gridAlignment = gridAlignment;
    }
}
