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
 
 package org.openconcerto.utils.prog;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class SVNUtils {

    public static void main(String[] args) throws IOException {
        if (args[0].equals("lastChangedRev")) {
            System.out.println(lastChangedRev(args[1]));
        } else {
            System.err.println("must choose a method");
        }
    }

    public static Process svn(String[] args) throws IOException {
        final String[] cmdNArgs = new String[args.length + 1];
        cmdNArgs[0] = "svn";
        System.arraycopy(args, 0, cmdNArgs, 1, args.length);
        return Runtime.getRuntime().exec(cmdNArgs);
    }

    public static String lastChangedRev(String path) throws IOException {
        final Process p = svn(new String[] { "info", path + "@HEAD" });

        final BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        String res = null;
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("Last Changed Rev"))
                res = line.substring(line.indexOf(':') + 1).trim();
        }
        return res;
    }

}
