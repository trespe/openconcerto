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
 
 /*
 * Created on 31 oct. 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.openconcerto.ui;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;

/**
 * VFlowLayout is similair to FlowLayout except it lays out components
 * vertically. Extends FlowLayout because it mimics much of the
 * behavior of the FlowLayout class, except vertically. An additional
 * feature is that you can specify a fill to edge flag, which causes
 * the VFlowLayout manager to resize all components to expand to the
 * column width Warning: This causes problems when the main panel
 * has less space that it needs and it seems to prohibit multi-column
 * output
 * @author Larry Schuler
 * @version 1.0, 1/1/96
 * @author based on FLowLayout by AVH.
 */
public class VFlowLayout extends FlowLayout {

    public static final int TOP 	= 0;
    public static final int MIDDLE 	= 1;
    public static final int BOTTOM 	= 2;

    int align;
    int hgap;
    int vgap;
	boolean fill;

    /**
     * Construct a new VFlowLayout with a middle alignemnt, and 
	 * the fill to edge flag set.
     */

    public VFlowLayout() {
		this(MIDDLE, 0, 0, true);
    }

	/** 
	 * Construct a new VFlowLayout with a middle alignemnt.
	 * @param fill the fill to edge flag
	 */
	public VFlowLayout(boolean fill){
		this(MIDDLE, 5, 5, fill);
	}

    /**
	 * Construct a new VFlowLayout with a middle alignemnt.
     * @param align the alignment value
     */
    public VFlowLayout(int align) {
		this(align, 5, 5, true);
    }

	/**
	 * Construct a new VFlowLayout.
	 * @param align the alignment value
	 * @param fill the fill to edge flag
	 */
    public VFlowLayout(int align, boolean fill) {
		this(align, 5, 5, fill);
    }

    /**
	 * Construct a new VFlowLayout.
	 * @param align the alignment value
	 * @param hgap the horizontal gap variable
	 * @param vgap the vertical gap variable
	 * @param fill the fill to edge flag
     */
    public VFlowLayout(int align, int hgap, int vgap, boolean fill) {
		this.align = align;
		this.hgap = hgap;
		this.vgap = vgap;
		this.fill = fill;
    }

    /**
     * Returns the preferred dimensions given the components
     * in the target container.
     * @param target the component to lay out
     */
    public Dimension preferredLayoutSize(Container target) {
		Dimension tarsiz = new Dimension(0, 0);

		for (int i = 0 ; i < target.getComponentCount(); i++) {
	    	Component m = target.getComponent(i);
	    	if (m.isVisible()) {
				Dimension d = m.getPreferredSize();
				tarsiz.width = Math.max(tarsiz.width, d.width);
				if (i > 0) {
		    		tarsiz.height += hgap;
				}
				tarsiz.height += d.height;
	    	}
		}
		Insets insets = target.getInsets();
		tarsiz.width += insets.left + insets.right + hgap*2;
		tarsiz.height += insets.top + insets.bottom + vgap*2;
		return tarsiz;
    }

    /**
     * Returns the minimum size needed to layout the target container
     * @param target the component to lay out 
     */
    public Dimension minimumLayoutSize(Container target) {
		Dimension tarsiz = new Dimension(0, 0);

		for (int i = 0 ; i < target.getComponentCount() ; i++) {
	    	Component m = target.getComponent(i);
	    	if (m.isVisible()) {
				Dimension d = m.getMinimumSize();
				tarsiz.width = Math.max(tarsiz.width, d.width);
				if (i > 0) {
		    		tarsiz.height += vgap;
				}
				tarsiz.height += d.height;
	    	}
		}
		Insets insets = target.getInsets();
		tarsiz.width += insets.left + insets.right + hgap*2;
		tarsiz.height += insets.top + insets.bottom + vgap*2;
		return tarsiz;
    }

    /** 
	 * places the components defined by first to last within the target 
	 * container using the bounds box defined
     * @param target the container
     * @param x the x coordinate of the area
     * @param y the y coordinate of the area
     * @param width the width of the area
     * @param height the height of the area
     * @param first the first component of the container to place
     * @param last the last component of the container to place
     */
    private void placethem(Container target, int x, int y, 
	                            int width, int height, 
								int first, int last) {

		if ( align == VFlowLayout.MIDDLE )
			y += height / 2;
		if ( align == VFlowLayout.BOTTOM )
			y += height;

		for (int i = first ; i < last ; i++) {
	    	Component m = target.getComponent(i);
			Dimension md = m.getSize();
	    	if (m.isVisible()) {
				int px = x + (width-md.width)/2;
				m.setLocation(x + (width-md.width)/2, y );
				y += vgap + md.height;
	    	}
		}
    }

    /**
     * Lays out the container. 
     * @param target the container to lay out.
     */
    public void layoutContainer(Container target) {
		Insets insets = target.getInsets();
		int maxheight = target.getSize().height - (insets.top + insets.bottom + vgap*2);
		int maxwidth = target.getSize().width - (insets.left + insets.right + hgap*2);
		int numcomp = target.getComponentCount();
		int x = insets.left + hgap, y = 0;
		int colw = 0, start = 0;

		for (int i = 0 ; i < numcomp ; i++) {
	    	Component m = target.getComponent(i);
	    	if (m.isVisible()) {
				Dimension d = m.getPreferredSize();
				if ( this.fill ){
					m.setSize(maxwidth, d.height);
					d.width=maxwidth;
				} else{
					m.setSize(d.width, d.height);
				}
			
				if ( y > maxheight ){
					placethem(target, x, insets.top + vgap,
								   colw, maxheight- y, start, i);
					y = d.height;
					x += hgap + colw;
					colw = d.width;
					start = i;
				}else{	
					if ( y > 0 ) y+= vgap;
					y += d.height;
					colw = Math.max(colw, d.width);
				}
			}
		}
		placethem(target, x, insets.top + vgap, colw, maxheight - y, start, numcomp);
    }
}
