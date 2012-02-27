package org.openconcerto.modules.google.docs;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.openconcerto.erp.storage.StorageEngine;

import com.google.gdata.util.AuthenticationException;

public class GoogleDocsStorageEngine implements StorageEngine {
    private GoogleDocsUtils gUtils;

    @Override
    public boolean isConfigured() {
        try {
            final Properties props = GoogleDocsPreferencePanel.getProperties();
            return !props.getProperty(GoogleDocsPreferencePanel.ACCOUNT_LOGIN, "").isEmpty() && !props.getProperty(GoogleDocsPreferencePanel.ACCOUNT_PASSWORD, "").isEmpty();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void connect() throws IOException {
        final Properties props = GoogleDocsPreferencePanel.getProperties();
        gUtils = new GoogleDocsUtils("OpenConcerto");
        try {
            gUtils.login(props.getProperty(GoogleDocsPreferencePanel.ACCOUNT_LOGIN, ""), props.getProperty(GoogleDocsPreferencePanel.ACCOUNT_PASSWORD, ""));
        } catch (AuthenticationException e) {
            throw new IOException("Identifiant ou mot de passe incorrect");
        }

    }

    @Override
    public void disconnect() throws IOException {
        // Nothing to do
    }

    @Override
    public void store(InputStream inStream, String remotePath, String title, boolean synchronous) throws IOException {
        if (gUtils == null) {
            throw new IllegalStateException("Cannot upload before login is done");
        }
        remotePath = remotePath.replace('\\', '/');
        final File f = File.createTempFile("openconcerto", "googledocs_" + title);
        final FileOutputStream fOut = new FileOutputStream(f);
        final BufferedOutputStream bOut = new BufferedOutputStream(fOut);
        final byte[] buffer = new byte[4096 * 8];
        int len = 0;
        do {
            len = inStream.read(buffer);
            if (len > 0) {
                fOut.write(buffer, 0, len);
            }
        } while (len > 0);
        bOut.close();
        fOut.close();
        try {
            gUtils.uploadFile(f, remotePath, title, synchronous);
        } catch (Exception e) {
            throw new IOException(e);
        }
        if (synchronous) {
            f.delete();
        } else {
            f.deleteOnExit();
        }
    }

    public void store(final File file, String remotePath, final String title, boolean synchronous) throws IOException {
        final FileInputStream fIn = new FileInputStream(file);
        store(fIn, remotePath, title, synchronous);
        fIn.close();
    }

    @Override
    public boolean allowAutoStorage() {
        try {
            final Properties props = GoogleDocsPreferencePanel.getProperties();
            return props.getProperty(GoogleDocsPreferencePanel.AUTO, "true").equals("true");
        } catch (IOException e) {
            return false;
        }
    }
}
