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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Try to fix the encoding of non-utf8 file names. Currently this class can guess if a name is in
 * cp850 or cp1252.
 * 
 * @author Sylvain CUAZ
 */
public class CorrectPathEncoding {

    static public final String VERSION = "0.5.1";
    /** Some files couldn't be corrected. */
    static public final int EXIT_INCONCLUSIVE = 2;
    /** Usage was displayed. */
    static public final int EXIT_USAGE = 3;

    static public final String FIND_BIN_PROP = "find.path";
    static private final Charset cp850 = Charset.forName("cp850");
    static private final Charset cp1252 = Charset.forName("cp1252");
    static private final Charset[] nonUTF8 = { cp850, cp1252 };
    static private final Charset utf8 = Charset.forName("utf8");
    // all non valid files
    private static final String LIST_FILE = "list.print0";
    // each non valid file followed by the corrected name
    private static final String DATA_FILE = "renameData";
    // all inconclusive files
    private static final String ERROR_FILE = "renameError";
    // the script using DATA_FILE
    private static final String RENAME_SCRIPT = "rename.sh";
    private static final String[] GENERATED_FILES = { LIST_FILE, DATA_FILE, ERROR_FILE, RENAME_SCRIPT };

    static private int lastIndexOf(final byte[] array, byte b) {
        for (int i = array.length - 1; i >= 0; i--) {
            if (array[i] == b)
                return i;
        }
        return -1;
    }

    public static void main(String[] args) throws Exception {
        final int res;
        if (args.length == 0 || args.length > 2)
            res = usage();
        else {
            final String correctOption = "--correct";
            final String option = args.length == 1 ? correctOption : args[0];
            final String value = args.length == 1 ? args[0] : args[1];

            final boolean find = option.equals("--find");
            if (option.equals("--list")) {
                final File list = new File(value);
                res = generateScript(new CorrectPathEncoding(list.getParentFile()), list, find);
            } else {
                final CorrectPathEncoding renamer = new CorrectPathEncoding(new File(value));
                if (find) {
                    res = generateScript(renamer, renamer.find(), find);
                } else if (option.equals("--resolve")) {
                    renamer.resolve();
                    res = 0;
                } else if (option.equals("--exec")) {
                    res = renamer.exec();
                } else if (option.equals(correctOption)) {
                    res = renamer.correct();
                } else if (option.equals("--clean")) {
                    renamer.clean(true);
                    res = 0;
                } else {
                    res = usage();
                }
            }
            if (res != 0)
                System.err.println(option + " returned code " + res);
        }
        System.exit(res);
    }

    static private int generateScript(CorrectPathEncoding renamer, final File list, final boolean find) throws IOException {
        if (!list.isFile())
            throw new IllegalStateException("not a file " + list);

        if (list.length() == 0) {
            if (find) {
                System.out.println("No files to correct found.");
                list.delete();
            } else {
                System.out.println("List empty : " + list.getAbsolutePath());
            }
            return 0;
        } else {
            System.out.println("Generated " + renamer.generateScript(new BufferedInputStream(new FileInputStream(list))) + " renames in " + renamer.dirMap.size() + " directories.");
            return renamer.hasErrors() ? EXIT_INCONCLUSIVE : 0;
        }
    }

    private static int usage() {
        System.out.println(CorrectPathEncoding.class.getSimpleName() + " version " + VERSION);
        System.out.println(CorrectPathEncoding.class.getName() + " [--correct] dir | --find dir | --resolve dir | --clean dir | --list fileList");
        System.out.println("--find will search for files to correct in dir and will output rename.sh in it");
        System.out.println("--resolve will ask how to rename files in " + ERROR_FILE);
        System.out.println("--exec will execute a previously generated script");
        System.out.println("--clean will remove any generated files");
        System.out.println("--correct will call successively --find, --resolve, --exec, --clean");
        System.out.println("--list allow to take an already generated list and output rename.sh in the same directory");
        System.out.println("\tfileList must be a list of filenames separated by NUL, depth-first");
        System.out.println("\n" + FIND_BIN_PROP + " : system property to point to the 'find' program");
        return EXIT_USAGE;
    }

    private final File outDir;
    private final Map<String, Charset> dirMap;

    private boolean verbose = false;

    protected CorrectPathEncoding() {
        this(new File("."));
    }

    /**
     * Create a new instance.
     * 
     * @param outDir where to put the bash script and the data file.
     */
    protected CorrectPathEncoding(File outDir) {
        super();
        if (!outDir.isDirectory())
            throw new IllegalArgumentException(outDir + " is not a directory");
        this.outDir = outDir;
        this.dirMap = new HashMap<String, Charset>(4096);
    }

    public final void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    private final File getErrorFile() {
        return new File(this.outDir, ERROR_FILE);
    }

    private boolean hasErrors() {
        return this.getErrorFile().exists() && this.getErrorFile().length() > 0;
    }

    public final File find() throws Exception {
        final String findBin = System.getProperty(FIND_BIN_PROP, "find");
        final ProcessBuilder procBuilder = new ProcessBuilder(findBin, ".", "-depth", "-not", "-name", "*", "-fprint0", LIST_FILE).directory(this.outDir);
        final Process proc = procBuilder.start();
        final int exitCode = proc.waitFor();
        if (exitCode != 0)
            throw new IllegalStateException("find returned " + exitCode);
        return new File(this.outDir, LIST_FILE);
    }

    /**
     * Generate the script to correct names in <code>ins</code>.
     * 
     * @param ins the list of filenames separated by NUL, depth-first to rename files before their
     *        directory (i.e. find . -depth -not -name '*' -print0).
     * @return the number of files (or directories) renamed.
     * @throws IOException if an error occurs.
     */
    public final Long generateScript(final InputStream ins) throws IOException {
        final OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(this.outDir, DATA_FILE)));
        OutputStream err = null;

        long count = 0;
        boolean containsNonASCII = false;
        // must hold a complete path
        final ByteBuffer buffer = ByteBuffer.allocate(4096);
        int i;
        while ((i = ins.read()) != -1) {
            if (i == 0) {
                // end of path
                if (containsNonASCII) {
                    final int bufferCount = buffer.position();
                    buffer.limit(bufferCount);
                    // reset position before reading
                    buffer.position(0);

                    boolean validUTF8;
                    try {
                        utf8.newDecoder().decode(buffer);
                        validUTF8 = true;
                    } catch (Exception e) {
                        validUTF8 = false;
                    }
                    if (!validUTF8) {
                        final byte[] dst = new byte[bufferCount];
                        // reset position before reading
                        buffer.position(0);
                        buffer.get(dst);
                        try {
                            final byte[] fixedName = this.guessName(dst);
                            out.write(dst);
                            out.write(0);
                            out.write(fixedName);
                            out.write(0);
                            count++;
                        } catch (IOException e) {
                            throw e;
                        } catch (Exception e) {
                            // keep going since this process usually takes a long time
                            e.printStackTrace();
                            if (err == null) {
                                err = new BufferedOutputStream(new FileOutputStream(getErrorFile()));
                            }
                            err.write(dst);
                            err.write(0);
                        }
                    }
                }
                // reset before using it again
                buffer.clear();
                containsNonASCII = false;
            } else {
                buffer.put((byte) i);
                if (i >= 128)
                    containsNonASCII = true;
            }
        }

        out.close();
        if (err != null)
            err.close();

        final BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(this.outDir, RENAME_SCRIPT)), utf8));
        writer.write("#!/bin/bash");
        writer.newLine();
        writer.newLine();
        writer.write("xargs -0 -a " + DATA_FILE + " -L2 mv");
        writer.newLine();
        writer.close();

        return count;
    }

    public final long resolve() throws IOException {
        long res = 0;
        final File errFile = getErrorFile();
        if (errFile.isFile() && errFile.length() > 0) {
            System.out.println("Please provide the correct name (just the filename, without the directory) :");
            final BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            // must hold a complete path
            final ByteBuffer buffer = ByteBuffer.allocate(4096);
            int i;
            final InputStream errStream = new BufferedInputStream(new FileInputStream(errFile));
            final OutputStream out = new BufferedOutputStream(new FileOutputStream(new File(errFile.getParentFile(), DATA_FILE), true));
            while ((i = errStream.read()) != -1) {
                if (i != 0) {
                    buffer.put((byte) i);
                } else {
                    buffer.limit(buffer.position());
                    // reset position before reading
                    buffer.position(0);

                    final Path p = new Path(buffer);

                    System.out.println(new String(p.getBytes(), utf8));
                    // handle case when the terminal sends an extra newline
                    final Pattern patrn = Pattern.compile("[\\p{Space}&&[^ ]]");
                    String answer = patrn.matcher(br.readLine()).replaceAll("");
                    while (answer.indexOf('/') >= 0) {
                        System.out.println("Do not include the directory");
                        answer = patrn.matcher(br.readLine()).replaceAll("");
                    }
                    out.write(p.getBytes());
                    out.write(0);
                    out.write(p.getFixedPath(answer));
                    out.write(0);
                    res++;

                    // reset before using it again
                    buffer.clear();
                }
            }
            out.close();
            errStream.close();
            errFile.delete();
        } else {
            System.out.println("Nothing to resolve");
        }
        return res;
    }

    public final int exec() throws Exception {
        if (new File(this.outDir, RENAME_SCRIPT).exists()) {
            System.out.print("Beginning renames...");
            final ProcessBuilder procBuilder = new ProcessBuilder("bash", RENAME_SCRIPT).directory(this.outDir);
            final Process proc = procBuilder.start();
            final int res = proc.waitFor();
            if (res == 0)
                System.out.println(" done");
            return res;
        } else {
            System.out.println("Nothing to execute.");
            return 0;
        }
    }

    public final void clean(boolean ask) throws IOException {
        final List<File> files = new ArrayList<File>();
        for (final String name : GENERATED_FILES) {
            final File f = new File(this.outDir, name);
            if (f.exists())
                files.add(f);
        }
        if (files.size() == 0) {
            System.out.println("No files to remove.");
        } else {
            final boolean proceed;
            if (ask) {
                System.out.println("Remove " + files + " ?");
                final int read = System.in.read();
                proceed = read == 'y';
            } else {
                proceed = true;
            }
            if (proceed) {
                for (final File f : files)
                    if (!f.delete())
                        throw new IOException("Couldn't remove " + f);
                if (ask)
                    System.out.println("Done.");
                else
                    System.out.println("Removed " + files + ".");
            } else {
                System.out.println("No files removed.");
            }
        }
    }

    public final int correct() throws Exception {
        generateScript(this, this.find(), true);

        if (this.hasErrors())
            this.resolve();

        final int execCode = this.exec();
        if (execCode != 0) {
            return execCode;
        } else {
            // exec() went well so no need to keep files
            this.clean(false);
            return 0;
        }
    }

    private byte[] guessName(final byte[] array) {
        final Path p = new Path(array);

        int cp850Count = 0, cp1252Count = 0;
        for (int i = p.getStart(); i < array.length; i++) {
            final byte b = array[i];
            if (b >= 0) {
                // ASCII
            } else {
                // remove sign
                final int positive = b + 256;
                final Charset cp;
                try {
                    cp = guessCP(positive);
                } catch (RuntimeException e) {
                    throw new IllegalStateException("Couldn't guess at " + i + " : " + new String(array), e);
                }
                if (cp == cp850)
                    cp850Count++;
                else if (cp == cp1252)
                    cp1252Count++;
                // else inconclusive
            }
        }
        final Charset cp;
        if (cp1252Count > 0 && cp850Count == 0) {
            cp = cp1252;
        } else if (cp850Count > 0 && cp1252Count == 0) {
            cp = cp850;
        } else {
            // not sure with any single byte
            cp = guessWithAllBytes(array);
        }
        if (cp == null) {
            throw new IllegalStateException("couldn't guess the encoding ; cp850Count: " + cp850Count + " : " + new String(array, cp850) + ", cp1252Count: " + cp1252Count + " : "
                    + new String(array, cp1252));
        } else {
            final String fixedName = p.getFileName(cp);

            final String dir = p.getDir();
            // files should have the same encoding inside a directory
            if (!this.dirMap.containsKey(dir)) {
                this.dirMap.put(dir, cp);
            } else if (this.dirMap.get(dir) != cp) {
                throw new IllegalStateException("current encoding was " + this.dirMap.get(dir) + " for " + dir + "\nbut " + cp + " for " + fixedName);
            }

            if (this.verbose)
                System.out.println("found charset: " + cp + " : " + fixedName);
            return p.getFixedPath(fixedName);
        }
    }

    private Charset guessWithAllBytes(byte[] array) {
        for (final Charset cs : nonUTF8) {
            final String s = new String(array, cs).toLowerCase();
            // alternatives would be repÁ.doc or repæ.doc
            if (s.endsWith(".repµ.doc") || s.endsWith(".rlpµ.doc"))
                return cs;
        }
        return null;
    }

    private Charset guessCP(final int positive) {
        // see cp.ods
        final Charset cp;
        switch (positive) {
        // case 0x80:
        case 0x81:
        case 0x82:
        case 0x83:
        case 0x84:
        case 0x85:
        case 0x86:
        case 0x87:
        case 0x88:
        case 0x89:
        case 0x8A:
        case 0x8B:
        case 0x8C:
        case 0x8D:
        case 0x8E:
        case 0x8F:

        case 0x90:
        case 0x91:
        case 0x92:
        case 0x93:
        case 0x94:
        case 0x95:
        case 0x96:
        case 0x97:
        case 0x98:
        case 0x99:
        case 0x9A:
        case 0x9B:
            cp = cp850;
            break;

        case 0x9C:
            cp = cp1252;
            break;

        case 0x9D:
        case 0x9E:
        case 0x9F:

            // case 0xA0:
        case 0xA1:
        case 0xA2:
            // case 0xA3:
            // case 0xA4:
            // case 0xA5:
            // case 0xA6:
            // case 0xA7:
            // case 0xA8:
            // case 0xA9:
            // case 0xAA:
            // case 0xAB:
        case 0xAC:
        case 0xAD:
            // case 0xAE:
            // case 0xAF:
            cp = cp850;
            break;

        case 0xB0:
        case 0xB1:
        case 0xB2:
        case 0xB3:
        case 0xB4:
            cp = cp1252;
            break;

        // case 0xB5:
        // case 0xB6:
        case 0xB7:
        case 0xB8:
            cp = cp850;
            break;

        case 0xB9:
        case 0xBA:
        case 0xBB:
        case 0xBC:
            cp = cp1252;
            break;

        // case 0xBD:
        // case 0xBE:
        // case 0xBF:

        case 0xC0:
        case 0xC1:
        case 0xC2:
        case 0xC3:
        case 0xC4:
        case 0xC5:
            // case 0xC6:
            // case 0xC7:
        case 0xC8:
        case 0xC9:
        case 0xCA:
        case 0xCB:
        case 0xCC:
            // case 0xCD:
        case 0xCE:
            // case 0xCF:
            cp = cp1252;
            break;

        case 0xD0:
        case 0xD1:
            throw new IllegalStateException("shouldn't happen " + Integer.toHexString(positive));

            // case 0xD2:
            // case 0xD3:
            // case 0xD4:
            // case 0xD5:
            // case 0xD6:
            // case 0xD7:
            // case 0xD8:
        case 0xD9:
        case 0xDA:
        case 0xDB:
        case 0xDC:
        case 0xDD:
        case 0xDE:
        case 0xDF:
            cp = cp1252;
            break;

        case 0xE0:
            // case 0xE1:
        case 0xE2:
        case 0xE3:
        case 0xE4:
        case 0xE5:
            // case 0xE6:
        case 0xE7:
        case 0xE8:
        case 0xE9:
            // case 0xEA:
            // case 0xEB:
        case 0xEC:
        case 0xED:
        case 0xEE:
            cp = cp1252;
            break;
        // case 0xEF:

        case 0xF0:
            throw new IllegalStateException("shouldn't happen " + Integer.toHexString(positive));

            // case 0xF1:
            // case 0xF2:
            // case 0xF3:
        case 0xF4:
            // case 0xF5:
            // case 0xF6:
        case 0xF7:
            cp = cp1252;
            break;
        case 0xF8:
            cp = cp850;
            break;
        // case 0xF9:
        case 0xFA:
        case 0xFB:
            cp = cp1252;
            break;
        // case 0xFC:

        case 0xFD:
            cp = cp850;
            break;

        // case 0xFE:
        case 0xFF:
            cp = cp850;
            break;

        default:
            // Inconclusive
            System.err.println("Inconclusive : " + Integer.toHexString(positive));
            cp = null;
        }
        return cp;
    }

    private static byte[] bufferToArray(ByteBuffer buffer) {
        final byte[] res = new byte[buffer.limit() - buffer.position()];
        buffer.get(res);
        return res;
    }

    static private final class Path {
        private final byte[] bytes;
        // start of filename (after last /)
        private final int start;

        public Path(ByteBuffer buffer) {
            this(bufferToArray(buffer));
        }

        public Path(final byte[] bytes) {
            super();
            this.bytes = bytes;
            // slash is in the ASCII set
            // also works if not found
            this.start = lastIndexOf(this.bytes, (byte) '/') + 1;
        }

        public byte[] getBytes() {
            return this.bytes;
        }

        public final int getStart() {
            return this.start;
        }

        public final String getDir() {
            return new String(this.getBytes(), 0, this.getStart() - 1, utf8);
        }

        public final String getFileName(final Charset cp) {
            return new String(this.getBytes(), this.getStart(), this.getBytes().length - this.getStart(), cp);
        }

        public final byte[] getFixedPath(final String fixedName) {
            final byte[] fixedNameB = fixedName.getBytes(utf8);
            final byte[] res = new byte[this.getStart() + fixedNameB.length];
            System.arraycopy(this.getBytes(), 0, res, 0, this.getStart());
            System.arraycopy(fixedNameB, 0, res, this.getStart(), fixedNameB.length);
            return res;
        }
    }
}
