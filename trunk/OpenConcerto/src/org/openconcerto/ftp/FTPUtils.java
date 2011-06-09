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
 
 package org.openconcerto.ftp;

import org.openconcerto.utils.RecursionType;
import org.openconcerto.utils.cc.ExnClosure;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

public final class FTPUtils {

    private FTPUtils() {
    }

    // MAYBE use recurse, but can't recursively pass local
    static public final void saveR(FTPClient ftp, File local) throws IOException {
        local.mkdirs();
        for (FTPFile child : ftp.listFiles()) {
            final String childName = child.getName();
            if (childName.indexOf('.') != 0) {
                if (child.isDirectory()) {
                    ftp.changeWorkingDirectory(childName);
                    saveR(ftp, new File(local, childName));
                    ftp.changeToParentDirectory();
                } else {
                    final OutputStream outs = new FileOutputStream(new File(local, childName));
                    ftp.retrieveFile(childName, outs);
                    outs.close();
                }
            }
        }
    }

    static public final void rmR(final FTPClient ftp, final String toRm) throws IOException {
        final String cwd = ftp.printWorkingDirectory();
        // si on ne peut cd, le dossier n'existe pas
        if (ftp.changeWorkingDirectory(toRm)) {
            recurse(ftp, new ExnClosure<FTPFile, IOException>() {
                @Override
                public void executeChecked(FTPFile input) throws IOException {
                    final boolean res;
                    if (input.isDirectory())
                        res = ftp.removeDirectory(input.getName());
                    else
                        res = ftp.deleteFile(input.getName());
                    if (!res)
                        throw new IOException("unable to delete " + input);
                }
            }, RecursionType.DEPTH_FIRST);
        }
        ftp.changeWorkingDirectory(cwd);
        ftp.removeDirectory(toRm);
    }

    static public final void recurse(FTPClient ftp, ExnClosure<FTPFile, ?> c) throws IOException {
        recurse(ftp, c, RecursionType.DEPTH_FIRST);
    }

    static public final void recurse(FTPClient ftp, ExnClosure<FTPFile, ?> c, RecursionType type) throws IOException {
        for (FTPFile child : ftp.listFiles()) {
            if (child.getName().indexOf('.') != 0) {
                if (type == RecursionType.BREADTH_FIRST)
                    c.executeCheckedWithExn(child, IOException.class);
                if (child.isDirectory()) {
                    ftp.changeWorkingDirectory(child.getName());
                    recurse(ftp, c, type);
                    ftp.changeToParentDirectory();
                }
                if (type == RecursionType.DEPTH_FIRST)
                    c.executeCheckedWithExn(child, IOException.class);
            }
        }
    }
}
