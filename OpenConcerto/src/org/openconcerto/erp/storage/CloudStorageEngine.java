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
 
 package org.openconcerto.erp.storage;

import org.openconcerto.erp.config.ComptaPropsConfiguration;
import org.openconcerto.utils.StreamUtils;
import org.openconcerto.utils.sync.SyncClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class CloudStorageEngine implements StorageEngine {

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public boolean allowAutoStorage() {
        return true;
    }

    @Override
    public void connect() throws IOException {
    }

    @Override
    public void disconnect() throws IOException {
    }

    @Override
    public void store(InputStream inStream, String remotePath, String title, boolean synchronous) throws IOException {
        final File localFile = File.createTempFile("OpenConcerto", title);
        remotePath = remotePath.replace('\\', '/');
        try {
            final ComptaPropsConfiguration config = ComptaPropsConfiguration.getInstanceCompta();
            remotePath = config.getSocieteID() + "/" + remotePath;
            final SyncClient client = new SyncClient("https://" + config.getStorageServer());
            client.setVerifyHost(false);
            StreamUtils.copy(inStream, localFile);
            System.out.println("CloudStorageEngine: send file:" + localFile.getCanonicalPath() + " to " + remotePath + " " + title);
            client.sendFile(localFile, remotePath, title, config.getToken());
        } catch (Exception e) {
            throw new IOException(e);
        } finally {
            if (localFile.exists()) {
                localFile.delete();
            }
        }

    }
}
