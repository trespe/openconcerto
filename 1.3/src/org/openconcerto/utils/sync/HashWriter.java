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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;

public class HashWriter {
    public static int blockSize = 1024;
    private File in;

    public HashWriter(File inputFile) {
        this.in = inputFile;
    }

    public void saveHash(File outputFile) {
        try {
            if (!outputFile.exists()) {
                new File(outputFile.getParent()).mkdirs();
            }
            DataOutputStream bOut = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(outputFile)));
            bOut.writeInt((int) this.in.length());
            System.out.println("FileSize:" + this.in.length());
            MessageDigest hashSum = MessageDigest.getInstance("SHA-256");
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            BufferedInputStream fb = new BufferedInputStream(new FileInputStream(in));
            RollingChecksum32 r32 = new RollingChecksum32();
            byte[] buffer = new byte[blockSize];

            int readSize = fb.read(buffer);
            while (readSize > 0) {
                // Update
                r32.check(buffer, 0, readSize);
                md5.reset();
                md5.update(buffer, 0, readSize);
                hashSum.update(buffer, 0, readSize);
                // read
                readSize = fb.read(buffer);
                // System.out.print(r32.getValue() + " : ");
                final byte[] engineDigest = md5.digest();

                // System.out.println(Base64.encodeBytes(engineDigest));
                bOut.writeInt(r32.getValue());
                bOut.write(engineDigest);

            }

            byte[] fileHash = new byte[hashSum.getDigestLength()];
            fileHash = hashSum.digest();
            bOut.write(fileHash);
            bOut.close();

        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public static byte[] getHash(File f) throws Exception {
        MessageDigest hashSum = MessageDigest.getInstance("SHA-256");

        final BufferedInputStream fb = new BufferedInputStream(new FileInputStream(f));
        byte[] buffer = new byte[blockSize];
        int readSize = fb.read(buffer);
        while (readSize > 0) {
            // Update
            hashSum.update(buffer, 0, readSize);
            // read
            readSize = fb.read(buffer);
        }
        fb.close();
        byte[] fileHash = new byte[hashSum.getDigestLength()];
        fileHash = hashSum.digest();
        return fileHash;
    }

    public static boolean compareHash(byte[] h1, byte[] h2) {
        final int length = h1.length;
        if (length != h2.length) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            if (h1[i] != h2[i]) {
                return false;
            }
        }
        return true;
    }

}
