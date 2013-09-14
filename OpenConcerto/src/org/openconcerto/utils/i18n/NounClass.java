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
 
 package org.openconcerto.utils.i18n;

import net.jcip.annotations.Immutable;

/**
 * The class of a noun. Each locale might create and use its own.
 * 
 * @author Sylvain
 * @see <a href="Wikipedia">http://en.wikipedia.org/wiki/Noun_class</a>
 */
@Immutable
public class NounClass extends GrammaticalBase {

    static public final NounClass MASCULINE = new NounClass("masculine");
    static public final NounClass FEMININE = new NounClass("feminine");
    static public final NounClass NEUTER = new NounClass("neuter");

    public NounClass(String name) {
        super(name);
    }
}
