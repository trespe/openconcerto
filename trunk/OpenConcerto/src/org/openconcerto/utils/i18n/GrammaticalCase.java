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
 * A grammatical case. Each locale might create and use its own.
 * 
 * @author Sylvain
 * @see <a href="Wikipedia">http://en.wikipedia.org/wiki/Grammatical_case</a>
 */
@Immutable
public class GrammaticalCase extends GrammaticalBase {

    static public final GrammaticalCase NOMINATIVE = new GrammaticalCase("nominative");
    static public final GrammaticalCase ACCUSATIVE = new GrammaticalCase("accusative");
    static public final GrammaticalCase GENITIVE = new GrammaticalCase("genitive");
    static public final GrammaticalCase DATIVE = new GrammaticalCase("dative");
    static public final GrammaticalCase LOCATIVE = new GrammaticalCase("locative");
    static public final GrammaticalCase ABLATIVE = new GrammaticalCase("ablative");
    static public final GrammaticalCase INSTRUMENTAL = new GrammaticalCase("instrumental");
    static public final GrammaticalCase PREPOSITIONAL = new GrammaticalCase("prepositional");

    public GrammaticalCase(String name) {
        super(name);
    }
}
