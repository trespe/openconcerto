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

import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.CollectionUtils;
import org.openconcerto.utils.MessageDigestUtils;
import org.openconcerto.utils.RecursionType;
import org.openconcerto.utils.StringInputStream;
import org.openconcerto.utils.cc.ExnClosure;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPCommand;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.io.CopyStreamAdapter;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.CopyStreamListener;
import org.apache.commons.net.io.Util;

public class IFtp extends FTPClient {

    // *** Static

    private static final String MD5_SUFFIX = ".md5";
    private static final int MD5_LENGTH = 32;

    private static final String md5ToName(String fname, String md5) {
        if (md5.length() != MD5_LENGTH)
            throw new IllegalArgumentException("md5 size must be " + MD5_LENGTH + " : " + md5);
        return fname + "_" + md5 + MD5_SUFFIX;
    }

    // *** Instance

    private final ThreadLocal<CopyStreamListener> streamListeners = new ThreadLocal<CopyStreamListener>();

    @Override
    public boolean login(String username, String password) throws IOException {
        final boolean res = super.login(username, password);
        this.setFileType(FTP.BINARY_FILE_TYPE);
        return res;
    }

    public List<File> sync(File lDir) throws IOException, NoSuchAlgorithmException {
        return this.sync(lDir, null);
    }

    public List<File> sync(File lDir, CopyFileListener l) throws IOException, NoSuchAlgorithmException {
        return this.sync(lDir, l, false);
    }

    // path separator (/) is not standard, so cd first where you want to upload
    public List<File> sync(File ldir, final CopyFileListener l, boolean forceUpload) throws IOException, NoSuchAlgorithmException {
        // there might be more than one md5 per file, if an error occured during the last sync()
        final CollectionMap<String, String> nameToMD5s = new CollectionMap<String, String>();
        final Map<String, FTPFile> ftpFiles = new HashMap<String, FTPFile>();
        final FTPFile[] listFiles = this.listFiles();
        if (listFiles == null) {
            System.out.println("IFtp.sync(): listFiles null :" + ldir.getName());
            return new ArrayList<File>();
        }

        for (int i = 0; i < listFiles.length; i++) {
            FTPFile rFile = listFiles[i];
            // Oui ca arrive!
            if (rFile != null) {
                // some FTP servers returns 450 when NLST an empty directory, so use LIST
                final String name = rFile.getName();
                if (name != null) {
                    if (name.endsWith(MD5_SUFFIX)) {
                        // originalName_a045d5e6.md5
                        final int underscore = name.length() - MD5_SUFFIX.length() - MD5_LENGTH - 1;
                        if (underscore >= 0) {
                            final String fname = name.substring(0, underscore);
                            final String md5 = name.substring(underscore + 1, underscore + 1 + MD5_LENGTH);
                            nameToMD5s.put(fname, md5);
                        }
                    } else {
                        ftpFiles.put(name, rFile);
                    }
                } else {
                    System.out.println("IFtp.sync(): rFile.getName() null : [" + i + "]" + ldir.getName());
                }
            } else {
                System.out.println("IFtp.sync(): rFile null : [" + i + "]" + ldir.getName());
            }

        }

        final List<File> uploaded = new ArrayList<File>();
        final List<File> dirs = new ArrayList<File>();
        for (final File lFile : ldir.listFiles()) {
            if (lFile.isFile() && lFile.canRead()) {
                final String lName = lFile.getName();
                final List<String> md5List = (List<String>) nameToMD5s.getNonNull(lName);
                final String lMD5 = MessageDigestUtils.getMD5(lFile);
                boolean shouldUpload = true;
                if (!forceUpload && ftpFiles.containsKey(lFile.getName())) {
                    final long lSize = lFile.length();
                    final long rSize = ftpFiles.get(lFile.getName()).getSize();
                    shouldUpload = lSize != rSize || !lMD5.equalsIgnoreCase(CollectionUtils.getSole(md5List));
                }
                if (shouldUpload) {
                    // delete all previous
                    for (final String md5 : md5List) {
                        this.deleteFile(md5ToName(lName, md5));
                    }
                    if (l != null) {
                        final long fileSize = lFile.length();
                        this.streamListeners.set(new CopyStreamAdapter() {
                            @Override
                            public void bytesTransferred(long totalBytesTransferred, int bytesTransferred, long streamSize) {
                                l.bytesTransferred(lFile, totalBytesTransferred, bytesTransferred, fileSize);
                            }
                        });
                    }
                    this.storeFile(lName, new FileInputStream(lFile));
                    // don't signal the MD5 file to the listener
                    this.streamListeners.set(null);
                    this.storeFile(md5ToName(lName, lMD5), new StringInputStream(lMD5 + "\t" + lName));
                    uploaded.add(lFile);
                }
            } else if (lFile.isDirectory())
                dirs.add(lFile);
            // ignore other types of file
        }
        for (final File dir : dirs) {
            this.makeDirectory(dir.getName());
            if (!this.changeWorkingDirectory(dir.getName()))
                throw new IllegalStateException("could not cd to " + dir.getName());
            uploaded.addAll(this.sync(dir, l, forceUpload));
            this.changeToParentDirectory();
        }
        return uploaded;
    }

    public final void saveR(File local) throws IOException {
        FTPUtils.saveR(this, local);
    }

    public void rmR(String toRm) throws IOException {
        FTPUtils.rmR(this, toRm);
    }

    public final void recurse(ExnClosure<FTPFile, ?> c) throws IOException {
        recurse(c, RecursionType.DEPTH_FIRST);
    }

    public final void recurse(ExnClosure<FTPFile, ?> c, RecursionType type) throws IOException {
        FTPUtils.recurse(this, c, type);
    }

    // our __storeFile use CopyStreamListener
    private boolean __storeFile(int command, String remote, InputStream local) throws IOException {
        OutputStream output;
        Socket socket;

        if ((socket = _openDataConnection_(command, remote)) == null)
            return false;

        output = new BufferedOutputStream(socket.getOutputStream(), getBufferSize());

        // __fileType is private, if we really want we could subclass setFileType() to have access
        // if (__fileType == ASCII_FILE_TYPE)
        // output = new ToNetASCIIOutputStream(output);

        // Treat everything else as binary for now
        try {
            final CopyStreamListener l = this.streamListeners.get();
            final long size = CopyStreamEvent.UNKNOWN_STREAM_SIZE;
            if (l != null) {
                // copyStream() doesn't pass 0
                l.bytesTransferred(0, 0, size);
            }
            Util.copyStream(local, output, getBufferSize(), size, l, false);
        } catch (IOException e) {
            try {
                socket.close();
            } catch (IOException f) {
            }
            throw e;
        }
        output.close();
        socket.close();
        return completePendingCommand();
    }

    public boolean storeFile(String remote, InputStream local) throws IOException {
        return __storeFile(FTPCommand.STOR, remote, local);
    }

    public boolean appendFile(String remote, InputStream local) throws IOException {
        return __storeFile(FTPCommand.APPE, remote, local);
    }

    public boolean storeUniqueFile(String remote, InputStream local) throws IOException {
        return __storeFile(FTPCommand.STOU, remote, local);
    }

    public boolean storeUniqueFile(InputStream local) throws IOException {
        return __storeFile(FTPCommand.STOU, null, local);
    }

}
