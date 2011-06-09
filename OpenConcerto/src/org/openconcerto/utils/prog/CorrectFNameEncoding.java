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

import static org.openconcerto.utils.StringCodec.encodedToBash;
import static org.openconcerto.utils.StringCodec.quote;
import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.RecursionType;
import org.openconcerto.utils.StringCodec;
import org.openconcerto.utils.cc.IClosure;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CharacterCodingException;

/**
 * Correct filenames under the current directory. For each filename it looks for non-graphic chars.
 * If found tries an heuristic to determine the encoding, and tries to rename it to its correct
 * form. If the correct form already exists, if its a directory it uses rsync otherwise it simply
 * remove the file.
 * 
 * @author Sylvain
 */
public class CorrectFNameEncoding {

    public static void main(String[] args) throws CharacterCodingException {
        final File root = new File(".");

        // System.err.println(FilenameDecoding.decode("acce\\351ntISO8859_1__\\351"));
        // System.err.println(FilenameDecoding.decode("Mod\\350le"));
        // System.err.println(FilenameDecoding.decode("Affaires\\ CCI/Sainte\\
        // G\\202n\\202vi\\212ve\\ des\\ Bois"));

        mergeTree(root);
    }

    private static void mergeTree(final File root) {
        FileUtils.walk(root, new IClosure<File>() {

            private void exec(File wd, String cmd) throws IOException, InterruptedException {
                final Process mv = Runtime.getRuntime().exec(new String[] { "bash", "-c", cmd }, null, wd);
                System.out.println("[exec] in " + wd + " : " + cmd);
                BufferedReader err = new BufferedReader(new InputStreamReader(mv.getErrorStream()));
                if (mv.waitFor() != 0) {
                    String errL;
                    String errors = "";
                    while ((errL = err.readLine()) != null) {
                        errors += errL + "\n";
                    }
                    System.out.println(errors);
                    throw new IOException(errors);
                }
            }

            @Override
            public void executeChecked(File input) {
                if (input.isDirectory()) {
                    try {
                        final Process p = Runtime.getRuntime().exec(new String[] { "ls", "-b1" }, null, input);
                        final BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                        String encodedName;
                        while ((encodedName = r.readLine()) != null) {
                            if (StringCodec.isEncoded(encodedName)) {
                                final String validName = StringCodec.decode(encodedName);
                                final File validFile = new File(input, validName);
                                final String invalidBashName = encodedToBash(encodedName);
                                // System.out.println(invalidBashName + " -> " + validName);
                                if (!validFile.exists()) {
                                    exec(input, "mv -- " + invalidBashName + " " + quote(validName));
                                } else if (validFile.isDirectory()) {
                                    exec(input, "rsync -a -- " + invalidBashName + "/ " + quote(validName) + " && rm -rf -- " + invalidBashName);
                                } else {
                                    exec(input, "rm -f -- " + invalidBashName);
                                }
                            }
                        }
                    } catch (Exception e) {
                        throw ExceptionUtils.createExn(IllegalStateException.class, "", e);
                    }
                }
            }

        }, RecursionType.BREADTH_FIRST);
    }

}
