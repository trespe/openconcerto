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
 
 package org.openconcerto.erp.core.sales.pos.model;

import org.openconcerto.erp.preferences.TemplateNXProps;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.FileUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class ReceiptCode {

    // year/month/day
    static private final int DIR_DEPTH = 3;
    static private final int FILE_DEPTH = DIR_DEPTH + 1;
    static private final String EXT = ".xml";
    static private final String DELETED_SUFFIX = "_deleted";
    static private final String DELETED_EXT = EXT + DELETED_SUFFIX;
    static private final String IMPORTED_SUFFIX = "_imported";
    static private final String IMPORTED_EXT = EXT + IMPORTED_SUFFIX;

    static public final File getRootDir() {
        return getRootDir(false);
    }

    static public final File getRootDir(final boolean archived) {
        final TemplateNXProps nxprops = (TemplateNXProps) TemplateNXProps.getInstance();
        return new File(nxprops.getDefaultStringValue(), archived ? "Tickets archivés" : "Tickets");
    }

    static public final File getDayDir(final Calendar cal, final boolean create) {
        final int j = cal.get(Calendar.DAY_OF_MONTH);
        final int m = cal.get(Calendar.MONTH) + 1;
        final int a = cal.get(Calendar.YEAR);
        final List<String> dirs = Arrays.asList(DIGIT4_FORMAT.format(a), DIGIT2_FORMAT.format(m), DIGIT2_FORMAT.format(j));
        assert dirs.size() == DIR_DEPTH;
        File res = getRootDir();
        for (final String dir : dirs) {
            res = new File(res, dir);
        }
        if (create) {
            try {
                FileUtils.mkdir_p(res);
            } catch (IOException e) {
                ExceptionHandler.handle("Impossible de créer le dossier des tickets.\n\n" + res.getAbsolutePath());
            }
        }
        return res;
    }

    static public final List<File> getReceiptsToImport(final int caisseNb) {
        return FileUtils.list(getRootDir(), FILE_DEPTH, createFF(DIGIT2_FORMAT.format(caisseNb), false, false));
    }

    protected static FileFilter createFF(final String prefix, final boolean includeDeleted, final boolean includeImported) {
        return new FileFilter() {
            @Override
            public boolean accept(File f) {
                if (!f.isFile() || !f.getName().startsWith(prefix))
                    return false;
                return f.getName().endsWith(EXT) || (includeDeleted && f.getName().endsWith(DELETED_EXT)) || (includeImported && f.getName().endsWith(IMPORTED_EXT));
            }
        };
    }

    static private final String getEnd(final String s, final List<String> candidates) {
        for (final String candidate : candidates)
            if (s.endsWith(candidate))
                return candidate;
        return null;
    }

    static public final ReceiptCode fromFile(final File f) {
        final String name = f.getName();
        final String toRm = getEnd(name, Arrays.asList(EXT, DELETED_EXT, IMPORTED_EXT));
        if (toRm == null)
            return null;
        try {
            return new ReceiptCode(name.substring(0, name.length() - toRm.length()));
        } catch (ParseException e) {
            return null;
        }
    }

    static public void archiveCompletelyImported() throws IOException {
        final File archiveDir = getRootDir(true);
        FileUtils.mkdir_p(archiveDir);
        final File rootDir = getRootDir(false);
        final List<File> dirs = FileUtils.list(rootDir, DIR_DEPTH, FileUtils.DIR_FILTER);
        // don't archive today otherwise number will be wrong (see Ticket.initNumber())
        final File todayDir = getDayDir(Ticket.getCalendar(), false);
        for (final File dir : dirs) {
            // if all receipts are deleted or imported : archive
            if (!todayDir.equals(dir) && dir.listFiles(createFF("", false, false)).length == 0) {
                final File destDir = new File(archiveDir, FileUtils.relative(rootDir, dir));
                FileUtils.mkParentDirs(destDir);
                if (!destDir.exists()) {
                    // move
                    final String err = FileUtils.mv(dir, destDir);
                    if (err != null)
                        throw new IOException(err);
                } else {
                    // merge
                    for (final File f : dir.listFiles()) {
                        FileUtils.mv(f, destDir);
                    }
                    FileUtils.rm(dir);
                }
                assert !dir.exists();
            }
        }
    }

    static private final DecimalFormat DIGIT2_FORMAT = new DecimalFormat("00");
    static private final DecimalFormat DIGIT4_FORMAT = new DecimalFormat("0000");
    static private final DecimalFormat INDEX_FORMAT = new DecimalFormat("00000");

    static {
        DIGIT2_FORMAT.setMaximumIntegerDigits(DIGIT2_FORMAT.getMinimumIntegerDigits());
        DIGIT4_FORMAT.setMaximumIntegerDigits(DIGIT4_FORMAT.getMinimumIntegerDigits());
        INDEX_FORMAT.setMaximumIntegerDigits(INDEX_FORMAT.getMinimumIntegerDigits());
    }

    static private Number parse(final DecimalFormat f, final String s, final ParsePosition pos) throws ParseException {
        return (Number) parse(f, s, pos, f.getMaximumIntegerDigits());
    }

    static private Date parse(final SimpleDateFormat f, final String s, final ParsePosition pos) throws ParseException {
        // only works for fixed width pattern
        return (Date) parse(f, s, pos, f.toPattern().length());
    }

    // formats almost always try to parse to the end of the string, this method prevents that
    static private Object parse(final Format f, final String s, final ParsePosition pos, final int maxChar) throws ParseException {
        return f.parseObject(s.substring(0, pos.getIndex() + maxChar), pos);
    }

    private final int caisseNb;
    private final Calendar day;
    private final int dayIndex;
    private final String code;

    private SimpleDateFormat dateFormat;

    public ReceiptCode(String code) throws ParseException {
        super();
        // Code: 01_05042011_00002
        // filtre les chiffres
        final StringBuilder b = new StringBuilder();
        for (int i = 0; i < code.length(); i++) {
            final char c = code.charAt(i);
            if (Character.isDigit(c)) {
                b.append(c);
            }
        }
        code = b.toString();
        // Code: 010504201100002
        this.code = code;
        this.setCalendar(Ticket.getCalendar());

        // Code: 0105041300002
        // n°caisse sur 2 caracteres
        // date jour mois année JJMMAA
        // numero de ticket formaté sur 5 caractères
        final ParsePosition pos = new ParsePosition(0);
        this.caisseNb = parse(DIGIT2_FORMAT, code, pos).intValue();
        this.day = getCalendar();
        this.day.setTime(parse(this.dateFormat, code, pos));
        this.dayIndex = parse(INDEX_FORMAT, code, pos).intValue();
    }

    public ReceiptCode(final int caisseNb, final Calendar cal, final int dayIndex) {
        super();
        this.setCalendar(cal);
        this.caisseNb = caisseNb;
        this.day = (Calendar) cal.clone();
        this.dayIndex = dayIndex;
        this.code = DIGIT2_FORMAT.format(this.caisseNb) + this.dateFormat.format(this.day.getTime()) + INDEX_FORMAT.format(this.dayIndex);
    }

    public final void setCalendar(final Calendar newCal) {
        final Calendar cal = (Calendar) newCal.clone();
        this.dateFormat = new SimpleDateFormat("ddMMyy");
        this.dateFormat.setCalendar(cal);
        cal.clear();
        cal.set(2000, 0, 1);
        this.dateFormat.set2DigitYearStart(cal.getTime());
    }

    public final Calendar getCalendar() {
        return (Calendar) this.dateFormat.getCalendar().clone();
    }

    public final int getCaisseNb() {
        return this.caisseNb;
    }

    public final Calendar getDay() {
        return this.day;
    }

    public final int getDayIndex() {
        return this.dayIndex;
    }

    public final String getCodePrefix() {
        return this.code.substring(0, this.code.length() - INDEX_FORMAT.getMinimumIntegerDigits());
    }

    public final String getCode() {
        return this.code;
    }

    public final File getDir(final boolean create) {
        return getDayDir(getDay(), create);
    }

    public final File getFile() {
        return new File(getDir(true), getFileName());
    }

    public final String getFileName() {
        return getCode().replace(' ', '_') + EXT;
    }

    public void markDeleted() throws IOException {
        mark(DELETED_SUFFIX);
    }

    public void markImported() throws IOException {
        mark(IMPORTED_SUFFIX);
    }

    private final void mark(final String suffix) throws IOException {
        final File f = getFile();
        if (!f.renameTo(new File(f.getParentFile(), f.getName() + suffix)))
            throw new IOException("Couldn't rename " + f);
    }

    public final List<ReceiptCode> getSameDayCodes(final boolean includeAll) {
        final File dir = getDir(false);
        final File[] listFiles = dir.listFiles(createFF(getCodePrefix(), includeAll, includeAll));
        if (listFiles == null)
            return Collections.emptyList();

        final List<ReceiptCode> res = new ArrayList<ReceiptCode>(listFiles.length);
        for (final File f : listFiles) {
            res.add(fromFile(f));
        }
        return res;
    }
}
