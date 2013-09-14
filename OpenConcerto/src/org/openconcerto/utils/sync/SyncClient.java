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
 
 package org.openconcerto.utils.sync;

import org.openconcerto.utils.Base64;
import org.openconcerto.utils.FileUtils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

public class SyncClient {
    private long byteSent;
    private long byteReceived;
    private long byteSyncDownload;
    private long byteSyncUpload;
    private long filesSyncDownload;
    private long filesSyncUpload;
    private String baseUrl = "http://127.0.0.1:80";
    private boolean verifyHost = true;

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            printUsage();
            return;
        }
        String token = "";
        SyncClient c = new SyncClient();
        final String command = args[0];
        if (command.equalsIgnoreCase("put")) {
            String from = args[1];
            String to = args[2];
            String remotePath = c.setServerUrlFrom(to);
            if (remotePath != null) {
                final File localFile = new File(from);
                if (!localFile.exists()) {
                    System.out.println(localFile.getAbsolutePath() + " does not exist");
                    return;
                }
                if (!localFile.isDirectory()) {
                    String remoteName;
                    if (to.endsWith("/")) {
                        remoteName = localFile.getName();
                    } else {
                        remoteName = to.substring(to.lastIndexOf("/") + 1);
                    }
                    c.sendFile(localFile, remotePath, remoteName);
                } else {
                    c.sendDirectory(localFile, remotePath);
                }
                c.dumpStat();
            }
            return;
        } else if (command.equalsIgnoreCase("get")) {
            String rPath = c.setServerUrlFrom(args[1]);
            String to = args[2];

            final File dir = new File(to);
            if (dir.isFile()) {
                System.out.println(dir.getAbsolutePath() + " is not a directory");
                return;
            }
            dir.mkdirs();
            if (rPath.endsWith("/")) {

                c.retrieveDirectory(dir, rPath, token);
                c.dumpStat();
            } else {
                String remoteName = rPath;
                String remotePath = "/";
                if (rPath.contains("/")) {
                    int i = rPath.lastIndexOf('/');
                    remotePath = rPath.substring(0, i);
                    remoteName = rPath.substring(i + 1);
                }
                try {
                    c.retrieveFile(dir, remotePath, remoteName, token);
                    c.dumpStat();
                } catch (FileNotFoundException e) {
                    System.out.println("The file you are trying to download does not exists. If you are trying to download a directory, just add a / ");

                }
            }
            return;
        } else if (command.equalsIgnoreCase("list")) {
            String url = args[1];
            if (!url.startsWith("http://")) {
                System.out.println(url + " is not an http url, exiting.");
                return;
            }
            int n = url.substring(7).indexOf('/');
            if (n <= 2) {
                System.out.println("invalid url " + url);
                return;
            }
            c.baseUrl = url.substring(0, 7 + n);
            final String remothPath = url.substring(7 + n);
            try {
                c.listDirectory(remothPath, token);
            } catch (Exception e) {
                System.out.println(remothPath + " does not exist on the server");
            }
            return;
        } else if (command.equalsIgnoreCase("del") || command.equalsIgnoreCase("rm") || command.equalsIgnoreCase("delete") || command.equalsIgnoreCase("remove")) {
            System.out.println("My coder said I'm not allowed to delete files today. Sorry.");
            return;
        } else {
            System.out.println("Only commands put, get and list are allowed.");
            printUsage();
            return;
        }

    }

    private void sendDirectory(File localDir, String remotePath) throws Exception {
        if (!remotePath.endsWith("/")) {
            remotePath += "/";
        }
        File[] fl = localDir.listFiles();
        for (int i = 0; i < fl.length; i++) {
            File file = fl[i];
            if (file.isDirectory()) {
                sendDirectory(file, remotePath + file.getName());
            } else {
                sendFile(file, remotePath, file.getName());
            }
        }

    }

    public SyncClient() {
    }

    public SyncClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    private String setServerUrlFrom(String url) {
        if (!url.startsWith("http://") || !url.startsWith("https://")) {
            System.out.println(url + " is not an http url, exiting.");
            return null;
        }
        int n = url.substring(7).indexOf('/');
        if (n <= 2) {
            System.out.println("invalid url " + url);
            return null;
        }
        baseUrl = url.substring(0, 7 + n);
        final String remothPath = url.substring(7 + n);
        return remothPath;
    }

    private static void printUsage() {
        System.out.println("SyncClient usage examples:");
        System.out.println("Upload");
        System.out.println("To upload a file:           java -jar sync.jar put  /etc/fstab http://16.105.9.190:80/backup/fstabOk");
        System.out.println("To upload a directory:      java -jar sync.jar put  /etc/      http://16.105.9.190:80/backup/etc/");
        System.out.println("Download");
        System.out.println("To download a file:         java -jar sync.jar get  http://16.105.9.190:80/backup/fstabOk /tmp/");
        System.out.println("To download a directory:    java -jar sync.jar get  http://16.105.9.190:80/backup/        /tmp/");
        System.out.println("To list a remote directory: java -jar sync.jar list http://16.105.9.190:80/backup/");
        System.out.println("Note: directory synchronization is recursive");
    }

    public void dumpStat() {
        System.out.println("SyncClient statistics");
        System.out.println("Download:");
        System.out.println("- files received: " + this.filesSyncDownload);
        System.out.println("- bytes sync'ed :" + this.byteSyncDownload);
        System.out.print("- bytes received: " + this.byteReceived);
        if (this.byteSyncDownload > 0) {
            System.out.println(" (" + ((100f * this.byteReceived) / this.byteSyncDownload) + "%)");
        } else {
            System.out.println();
        }

        System.out.println("Upload:");
        System.out.println("- files sent: " + this.filesSyncUpload);
        System.out.println("- bytes sync'ed :" + this.byteSyncUpload);
        System.out.print("- bytes sent: " + this.byteSent);
        if (this.byteSyncUpload > 0) {
            System.out.println(" (" + ((100f * this.byteSent) / this.byteSyncUpload) + "%)");
        } else {
            System.out.println();
        }
    }

    private File localHashDirectory;

    private void setLocalHashDirectory(File file) {
        localHashDirectory = file;
    }

    public void sendFile(File localFile, String remotePath, String remoteName) throws Exception {
        sendFile(localFile, remotePath, remoteName, null);
    }

    public void sendFile(File localFile, String remotePath, String remoteName, String token) throws Exception {
        if (localFile == null) {
            throw new IllegalArgumentException("null file");
        }
        if (!localFile.exists()) {
            throw new IllegalArgumentException(localFile.getAbsolutePath() + " does not exist");
        }
        this.filesSyncUpload++;

        // Construct data
        String data = URLEncoder.encode("rp", "UTF-8") + "=" + URLEncoder.encode(remotePath, "UTF-8");
        data += "&" + URLEncoder.encode("rn", "UTF-8") + "=" + URLEncoder.encode(remoteName, "UTF-8");
        if (token != null) {
            data += "&" + URLEncoder.encode("tk", "UTF-8") + "=" + URLEncoder.encode(token, "UTF-8");
        }
        this.byteSent += data.getBytes().length;
        // Send data
        URLConnection conn = getConnection(this.baseUrl + "/getHash");
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();
        wr.close();
        byte[] localFileHash = null;
        int localFileSize = (int) localFile.length();
        RangeList rangesOk = new RangeList(localFileSize);
        MoveOperationList moves = new MoveOperationList();
        this.byteSyncUpload = localFileSize;
        try {
            // Get the response
            DataInputStream in = new DataInputStream(new BufferedInputStream(conn.getInputStream()));
            int remoteFileSize = in.readInt();
            this.byteReceived += 4;

            final float a = (float) remoteFileSize / HashWriter.blockSize;

            int nb = (int) Math.ceil(a);

            HashMap<Integer, byte[]> map = new HashMap<Integer, byte[]>(nb * 2 + 1);
            HashMap<Integer, Integer> mapBlock = new HashMap<Integer, Integer>(nb * 2 + 1);
            for (int i = 0; i < nb; i++) {
                byte[] b = new byte[16];
                final int r32 = in.readInt();
                // System.out.print("Known hash " + r32 + " : ");
                in.read(b);
                this.byteReceived += 4 + 16;

                map.put(r32, b);
                mapBlock.put(r32, i);
            }
            byte[] remoteFileHash = new byte[32];
            in.read(remoteFileHash);
            this.byteReceived += 32;
            in.close();

            if (localFileSize == remoteFileSize) {
                localFileHash = HashWriter.getHash(localFile);
                if (HashWriter.compareHash(localFileHash, remoteFileHash)) {

                    // Already in sync
                    return;
                }
            }

            // compare delta
            RollingChecksum32 checksum = new RollingChecksum32();
            byte[] buffer = new byte[HashWriter.blockSize];

            BufferedInputStream fb = new BufferedInputStream(new FileInputStream(localFile));
            final int read = fb.read(buffer);
            this.byteReceived += read;
            checksum.check(buffer, 0, read);
            int v = 0;
            int start = 0;

            MessageDigest md5Digest = MessageDigest.getInstance("MD5");

            do {

                int r32 = checksum.getValue();

                byte[] md5 = map.get(r32);
                if (md5 != null) {
                    // local block maybe exists in the remote file
                    // let's check if true with md5
                    md5Digest.reset();
                    md5Digest.update(buffer);
                    byte[] localMd5 = md5Digest.digest();
                    if (HashWriter.compareHash(md5, localMd5)) {

                        // Block found!!!
                        // Copy block to: mapBlock.get(r32)*blockSize;
                        int offset = mapBlock.get(r32) * HashWriter.blockSize;
                        //
                        MoveOperation m = new MoveOperation(offset, start, HashWriter.blockSize);
                        moves.add(m);
                        //
                        rangesOk.add(new Range(start, start + HashWriter.blockSize));
                    }

                }

                // read
                v = fb.read();

                start++;
                // Update
                System.arraycopy(buffer, 1, buffer, 0, buffer.length - 1);
                buffer[buffer.length - 1] = (byte) v;
                checksum.roll((byte) v);

            } while (v >= 0);
            fb.close();

        } catch (FileNotFoundException e) {
            // System.out.println("Sending the complete file");
        }
        if (localFileHash == null) {
            localFileHash = HashWriter.getHash(localFile);
        }
        try {
            sendDelta(localFile, remotePath, remoteName, moves, rangesOk.getUnusedRanges(), localFileHash, token);
        } catch (Exception e) {
            System.err.println("Unable to send delta: " + localFile.getAbsolutePath() + " to " + remoteName + " " + moves);
            rangesOk.dump();
            throw e;
        }
    }

    private void sendDelta(File localFile, String remotePath, String remoteName, MoveOperationList moves, List<Range> rangesToSend, byte[] localFileHash, String token) throws IOException {
        if (token == null) {
            token = "";
        }

        URLConnection conn = getConnection(this.baseUrl + "/putFile");
        conn.setDoOutput(true);

        final DataOutputStream wr = new DataOutputStream(new GZIPOutputStream(new BufferedOutputStream(conn.getOutputStream())) {
            @Override
            public synchronized void write(byte[] b, int off, int len) throws IOException {
                SyncClient.this.byteSent += len;
                super.write(b, off, len);
            }

            @Override
            public synchronized void write(int b) throws IOException {
                SyncClient.this.byteSent++;
                super.write(b);
            }
        });

        wr.writeUTF(remotePath);
        wr.writeUTF(remoteName);
        wr.writeUTF(token);
        wr.write(localFileHash);
        wr.writeInt((int) localFile.length());
        // Moves
        moves.write(wr);

        // Delta
        wr.writeInt(rangesToSend.size());
        RandomAccessFile rIn = new RandomAccessFile(localFile, "r");

        for (Range r : rangesToSend) {
            rIn.seek(r.getStart());
            byte[] buffer = new byte[r.getStop() - r.getStart()];
            rIn.readFully(buffer);

            wr.writeInt(r.getStart());
            wr.writeInt(r.getStop());
            wr.write(buffer);
            // System.out.println("SyncClient.sendDelta() : " + r.getStart() + "-" + r.getStop() +
            // " " + buffer.length);

        }
        rIn.close();
        wr.flush();
        wr.close();

        // Reading data is mandatory
        DataInputStream in = new DataInputStream(new BufferedInputStream(conn.getInputStream()));
        this.byteReceived += 32;
        byte[] rHash = new byte[32];
        in.read(rHash);
        in.close();
        if (!HashWriter.compareHash(localFileHash, rHash)) {
            throw new IllegalStateException("Hash error");
        }

    }

    public void retrieveDirectory(File dir, String remotePath, String token) throws Exception {
        ArrayList<FileProperty> list = getList(remotePath, token);
        // Check locally
        for (int i = 0; i < list.size(); i++) {
            final FileProperty fp = list.get(i);
            if (!fp.isDirectory()) {
                retrieveFile(dir, remotePath, fp.getName(), fp.getSize(), fp.getSha256(), token);
            } else {
                final File dir2 = new File(dir, fp.getName());
                dir2.mkdirs();
                retrieveDirectory(dir2, remotePath + "/" + fp.getName(), token);
            }
        }

    }

    public void listDirectory(String remotePath, String token) throws Exception {
        ArrayList<FileProperty> list = getList(remotePath, token);
        Collections.sort(list);
        final int size = list.size();
        int maxSize = -1;
        for (int i = 0; i < size; i++) {
            int s = list.get(i).getSize();
            if (s > maxSize) {
                maxSize = s;
            }
        }
        int lSize = (" " + maxSize).length();
        if (maxSize < 0) {
            lSize = 0;
        }
        SimpleDateFormat spd = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (int i = 0; i < size; i++) {
            FileProperty fp = list.get(i);
            if (lSize > 0) {
                if (fp.getSize() >= 0) {
                    System.out.print(rightAlign(String.valueOf(fp.getSize()), lSize));
                } else {
                    System.out.print(rightAlign("", lSize));
                }
                System.out.print(" " + spd.format(fp.getDate()));
                System.out.println(" " + fp.getName());
            } else {
                System.out.println(fp.getName());
            }

        }
    }

    public static String rightAlign(String s, int width) {
        String r = s;
        int n = width - s.length();
        for (int i = 0; i < n; i++) {
            r = ' ' + r;
        }
        return r;
    }

    public ArrayList<FileProperty> getList(String remotePath, String token) throws UnsupportedEncodingException, MalformedURLException, IOException {
        // If the file exists or not, get the hashValues to compare the hash

        // Construct data
        String data = "rp=" + URLEncoder.encode(remotePath, "UTF-8");
        if (token != null) {
            data += "&tk=" + URLEncoder.encode(token, "UTF-8");
        }
        this.byteSent += data.getBytes().length;
        // Send data
        URLConnection conn = getConnection(baseUrl + "/getDir");
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();
        wr.close();

        // Get the response ASAP in order to not block the server while computing locally hash256
        DataInputStream in = new DataInputStream(new BufferedInputStream(conn.getInputStream()));
        int fileCount = in.readInt();
        this.byteReceived += 4;
        ArrayList<FileProperty> list = new ArrayList<FileProperty>();
        for (int i = 0; i < fileCount; i++) {
            final String fileName = in.readUTF();
            this.byteReceived += fileName.getBytes().length;
            final int fileSize = in.readInt();
            this.byteReceived += 4;
            final long fileDate = in.readLong();
            this.byteReceived += 8;
            final byte[] sha256 = new byte[32];
            if (fileSize >= 0) {
                in.read(sha256);
                this.byteReceived += 32;
            }
            FileProperty fp = new FileProperty(fileName, fileSize, fileDate, sha256);
            list.add(fp);
        }
        in.close();
        return list;
    }

    /**
     * @return true if the file exists and is retrieved :)
     * */
    public void retrieveFile(File dir, String remotePath, String remoteName, String token) throws Exception {

        // If the file exists or not, get the hashValues to compare the hash

        // Construct data
        String data = "rp=" + URLEncoder.encode(remotePath, "UTF-8");
        data += "&rn=" + URLEncoder.encode(remoteName, "UTF-8");
        data += "&shaOnly=" + URLEncoder.encode("true", "UTF-8");
        if (token != null) {
            data += "&tk=" + URLEncoder.encode(token, "UTF-8");
        }
        this.byteSent += data.getBytes().length;
        // Send data
        URLConnection conn = getConnection(this.baseUrl + "/getHash");
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();
        wr.close();

        // Get the response

        DataInputStream in = new DataInputStream(new BufferedInputStream(conn.getInputStream()));
        int fileSize = in.readInt();
        this.byteReceived += 4;

        byte[] fileHash = new byte[32];
        in.read(fileHash);
        this.byteReceived += 32;
        in.close();

        retrieveFile(dir, remotePath, remoteName, fileSize, fileHash, token);

    }

    private void retrieveFile(File dir, String remotePath, String remoteName, int fileSize, byte[] fileHash, String token) throws IOException, Exception {
        this.filesSyncDownload++;
        this.byteSyncDownload += fileSize;
        File localFile = resolveFile(dir, remoteName);
        if (!localFile.exists()) {
            downloadFile(localFile, remotePath, remoteName, fileSize, token);
            byte[] fileLocalHash = HashWriter.getHash(localFile);
            if (!HashWriter.compareHash(fileHash, fileLocalHash)) {
                throw new IllegalStateException("Full download failed. Hash error");
            }
        } else {
            boolean needToResync = false;
            // File exists
            // 1 - check size
            if (localFile.length() != fileSize) {
                needToResync = true;
            } else {
                // 2 - check hash sha256
                byte[] fileLocalHash = HashWriter.getHash(localFile);
                if (!HashWriter.compareHash(fileHash, fileLocalHash)) {
                    needToResync = true;
                }
            }

            if (needToResync) {
                if (localFile.length() > HashWriter.blockSize) {
                    retrieveFileWithDelta(localFile, remotePath, remoteName, token);
                } else {
                    downloadFile(localFile, remotePath, remoteName, fileSize, token);
                    byte[] fileLocalHash = HashWriter.getHash(localFile);
                    if (!HashWriter.compareHash(fileHash, fileLocalHash)) {
                        throw new IllegalStateException("Full download failed. Hash error");
                    }
                }
            }

        }
    }

    /**
     * @param fileHash2
     * @return true if the file exists and is retrieved :)
     * */
    private void retrieveFileWithDelta(File localFile, String remotePath, String remoteName, String token) throws Exception {

        // Construct data
        String data = URLEncoder.encode("rp", "UTF-8") + "=" + URLEncoder.encode(remotePath, "UTF-8");
        data += "&" + URLEncoder.encode("rn", "UTF-8") + "=" + URLEncoder.encode(remoteName, "UTF-8");
        if (token != null) {
            data += "&tk=" + URLEncoder.encode(token, "UTF-8");
        }
        this.byteSent += data.getBytes().length;
        // Send data
        URLConnection conn = getConnection(this.baseUrl + "/getHash");
        conn.setDoOutput(true);
        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
        wr.write(data);
        wr.flush();
        wr.close();

        // Get the response
        DataInputStream in = new DataInputStream(new BufferedInputStream(conn.getInputStream()));
        int fileSize = in.readInt();
        this.byteReceived += 4;
        this.byteSyncDownload = fileSize;

        final float a = (float) fileSize / HashWriter.blockSize;

        int nb = (int) Math.ceil(a);

        HashMap<Integer, byte[]> map = new HashMap<Integer, byte[]>(nb * 2 + 1);
        HashMap<Integer, Integer> mapBlock = new HashMap<Integer, Integer>(nb * 2 + 1);
        for (int i = 0; i < nb; i++) {
            byte[] b = new byte[16];
            final int r32 = in.readInt();
            // System.out.print("Known hash " + r32 + " : ");
            in.read(b);
            this.byteReceived += 4 + 16;

            map.put(r32, b);
            mapBlock.put(r32, i);
        }
        byte[] fileHash = new byte[32];
        in.read(fileHash);
        this.byteReceived += 32;
        in.close();

        // create the new file
        File newFile = createEmptyFile(localFile.getParentFile(), fileSize);
        RandomAccessFile rNewFile = new RandomAccessFile(newFile, "rw");
        // compare delta
        RollingChecksum32 checksum = new RollingChecksum32();
        byte[] buffer = new byte[HashWriter.blockSize];

        BufferedInputStream fb = new BufferedInputStream(new FileInputStream(localFile));
        final int read = fb.read(buffer);
        checksum.check(buffer, 0, read);
        int v = 0;
        int start = 0;
        int end = read;
        MessageDigest md5Digest = MessageDigest.getInstance("MD5");
        RangeList rangesOk = new RangeList(fileSize);
        do {

            int r32 = checksum.getValue();

            byte[] md5 = map.get(r32);
            if (md5 != null) {
                // local block maybe exists in the remote file
                // let's check if true with md5
                md5Digest.reset();
                md5Digest.update(buffer);
                byte[] localMd5 = md5Digest.digest();
                if (HashWriter.compareHash(md5, localMd5)) {

                    // Block found!!!
                    // Copy block to: mapBlock.get(r32)*blockSize;
                    int offset = mapBlock.get(r32) * HashWriter.blockSize;
                    //
                    rNewFile.seek(offset);
                    rNewFile.write(buffer);
                    //
                    rangesOk.add(new Range(offset, offset + HashWriter.blockSize));
                }

            }

            // read
            v = fb.read();
            start++;
            // Update
            System.arraycopy(buffer, 1, buffer, 0, buffer.length - 1);
            buffer[buffer.length - 1] = (byte) v;
            checksum.roll((byte) v);
            end++;
        } while (v >= 0);
        fb.close();
        rangesOk.dump();

        // DOwnload missing parts
        final List<Range> unusedRanges = rangesOk.getUnusedRanges();
        DataInputStream zIn = getContent(remotePath, remoteName, unusedRanges, token);

        BufferedOutputStream fOut = new BufferedOutputStream(new FileOutputStream(localFile));

        final int size = unusedRanges.size();
        for (int i = 0; i < size; i++) {
            Range range = unusedRanges.get(i);
            rNewFile.seek(range.getStart());
            byte[] b = new byte[(int) range.size()];
            zIn.readFully(b);

            rNewFile.write(b);

        }

        fOut.close();
        zIn.close();

        rNewFile.close();

        // Check hash sha256
        byte[] fileLocalHash = HashWriter.getHash(newFile);
        if (!HashWriter.compareHash(fileHash, fileLocalHash)) {
            throw new IllegalStateException("Partial download failed. Hash error");
        }
        FileUtils.rm(localFile);
        FileUtils.mv(newFile, localFile);

    }

    private File createEmptyFile(File dir, int fileSize) throws FileNotFoundException, IOException {
        Random r = new Random();
        File newFile = new File(dir, "sync." + r.nextInt());
        BufferedOutputStream bOut = new BufferedOutputStream(new FileOutputStream(newFile));
        int l = fileSize;
        byte[] emptyBuffer = new byte[4096];
        while (l > 0) {
            bOut.write(emptyBuffer, 0, Math.min(4096, (int) l));
            l -= 4096;
        }
        bOut.close();
        return newFile;
    }

    private void downloadFile(File localFile, String remotePath, String remoteName, int fileSize, String token) throws IOException {
        List<Range> list = new ArrayList<Range>(1);
        list.add(new Range(0, fileSize));

        DataInputStream zIn = getContent(remotePath, remoteName, list, token);

        BufferedOutputStream fOut = new BufferedOutputStream(new FileOutputStream(localFile));
        StreamUtils.copy(zIn, fOut);
        fOut.close();
        zIn.close();

    }

    private DataInputStream getContent(String remotePath, String remoteName, List<Range> list, String token) throws MalformedURLException, IOException, UnsupportedEncodingException {
        URLConnection conn = getConnection(this.baseUrl + "/getFile");
        conn.setDoOutput(true);

        final ByteArrayOutputStream bOutputStream = new ByteArrayOutputStream();
        final DataOutputStream wr = new DataOutputStream(bOutputStream);
        wr.writeInt(list.size());
        for (Range range : list) {
            // System.out.println("SyncClient.getContent() from server: " + range);
            wr.writeInt(range.getStart());
            wr.writeInt(range.getStop());
        }
        wr.close();

        String data = "rp=" + URLEncoder.encode(remotePath, "UTF-8");
        data += "&rn=" + URLEncoder.encode(remoteName, "UTF-8");
        data += "&ra=" + URLEncoder.encode(Base64.encodeBytes(bOutputStream.toByteArray()), "UTF-8");
        if (token != null) {
            data += "&tk=" + URLEncoder.encode(token, "UTF-8");
        }
        this.byteSent += data.getBytes().length;
        OutputStreamWriter osw = new OutputStreamWriter(conn.getOutputStream());
        osw.write(data);
        osw.flush();
        osw.close();

        final InputStream inputStream = new BufferedInputStream(conn.getInputStream()) {
            @Override
            public synchronized int read(byte[] b, int off, int len) throws IOException {
                byteReceived += len;
                return super.read(b, off, len);
            }
        };
        // System.out.println("SyncClient.downloadFile() " + inputStream.available());
        GZIPInputStream zIn = new GZIPInputStream(inputStream);
        return new DataInputStream(zIn);
    }

    List<Date> getVersions(String remotePath, String remoteName) {
        List<Date> l = new ArrayList<Date>();
        return l;
    }

    public File resolveFile(File dir, String remoteName) {

        if (remoteName.contains("..")) {
            return null;
        }
        if (remoteName.contains("/") || remoteName.contains("\\")) {
            return null;
        }

        return new File(dir, remoteName);
    }

    public void clearStat() {
        byteSent = 0;
        byteReceived = 0;
        byteSyncDownload = 0;
        byteSyncUpload = 0;
        filesSyncDownload = 0;
        filesSyncUpload = 0;
    }

    public static final HostnameVerifier HostnameNonVerifier = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };

    URLConnection getConnection(String strUrl) throws IOException {
        URL url = new URL(strUrl);
        URLConnection conn = url.openConnection();
        if (!verifyHost && strUrl.startsWith("https")) {
            HttpsURLConnection httpsCon = (HttpsURLConnection) conn;
            httpsCon.setHostnameVerifier(HostnameNonVerifier);
        }

        return conn;
    }

    public void setVerifyHost(boolean verify) {
        this.verifyHost = verify;
    }
}
