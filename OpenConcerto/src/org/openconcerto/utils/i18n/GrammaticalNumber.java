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
 * A grammatical number. Each locale might create and use its own.
 * 
 * @author Sylvain
 * @see <a href="Wikipedia">http://en.wikipedia.org/wiki/Grammatical_number</a>
 */
@Immutable
public class GrammaticalNumber extends GrammaticalBase {

    public static final GrammaticalNumber SINGULAR = new GrammaticalNumber("singular");
    public static final GrammaticalNumber PLURAL = new GrammaticalNumber("plural");

    public static final GrammaticalNumber SINGULATIVE = new GrammaticalNumber("singulative");
    public static final GrammaticalNumber COLLECTIVE = new GrammaticalNumber("collective");

    public static final GrammaticalNumber DUAL = new GrammaticalNumber("dual");
    public static final GrammaticalNumber TRIAL = new GrammaticalNumber("trial");
    public static final GrammaticalNumber QUADRAL = new GrammaticalNumber("quadral");

    public static final GrammaticalNumber PAUCAL = new GrammaticalNumber("paucal");
    public static final GrammaticalNumber PARTITIVE = new GrammaticalNumber("partitive");

    public GrammaticalNumber(String name) {
        super(name);
    }
}
