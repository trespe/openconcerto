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

import java.io.Serializable;

public class LightControler implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 5894135825924339012L;
    private final String type, src, dest;
    public static final String TYPE_ACTIVATION_ON_SELECTION = "activationOnSelection";
    public static final String TYPE_ADD_DEFAULT = "addDefault";
    public static final String TYPE_REMOVE = "remove";

    public LightControler(String type, String src, String dest) {
        this.type = type;
        this.src = src;
        this.dest = dest;
    }

    public String getType() {
        return type;
    }

    public String getSrc() {
        return src;
    }

    public String getDest() {
        return dest;
    }

    @Override
    public String toString() {
        return super.getClass().getName() + " : " + type + " :" + src + "," + dest;
    }
}
