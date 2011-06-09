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
 
 package org.openconcerto.utils.html;


public abstract class Page {
	private static Site site;

	public Page() {
		site.addPage(this);
	}

	static void setSite(Site s) {
		site = s;
	}

	public HTMLContent getContent() {
		return new HTMLParagraph("Contenu vide");
	}

	// ex: index.html ou dir/test.html
	public abstract String getPath();

	public abstract String getMenuTitle();

	public abstract String getTitle();

	public abstract String getDescription();

	// 
	public abstract String getBodyId();

	public abstract void init();

	public abstract void create();

	@Override
	public boolean equals(Object obj) {
		Page p = (Page) obj;
		return p.getPath().equals(this.getPath());
	}
}
