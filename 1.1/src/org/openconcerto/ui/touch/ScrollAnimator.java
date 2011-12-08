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
 
 package org.openconcerto.ui.touch;

public class ScrollAnimator extends Animator {

	private ScrollableList scroll;
	private long startOffset;
	private long stopOfset;

	public ScrollAnimator(ScrollableList scrollableList) {
		this.scroll = scrollableList;
		this.duration = 200;
	}

	@Override
	public void process(float fraction, long totalElapsed) {
		double offset = startOffset + Math.sqrt(fraction)
				* (stopOfset - startOffset);
		scroll.setOffsetY((int) Math.round(offset));

	}

	public void setStart(int offsetY) {
		this.startOffset = offsetY;
	}

	public void setStop(long targetOffset) {
		this.stopOfset = targetOffset;
	}

}
