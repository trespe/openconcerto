package org.openconcerto.modules.storage.docs.ovh;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Properties;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.openconcerto.erp.storage.StorageEngine;

import com.googlecode.sardine.DavResource;
import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;

public class CloudNASStorageEngine implements StorageEngine {

    private String baseUrl, userUrl;
    private Sardine sardine;

    public CloudNASStorageEngine() {
    }

    public CloudNASStorageEngine(String account) {
        this.baseUrl = "https://cloud.ovh.fr/" + account;
    }

    @Override
    public boolean isConfigured() {
        try {
            Properties props = CloudNASPreferencePanel.getProperties();
            return !props.getProperty(CloudNASPreferencePanel.ACCOUNT, "").isEmpty() && !props.getProperty(CloudNASPreferencePanel.ACCOUNT_LOGIN, "").isEmpty()
                    && !props.getProperty(CloudNASPreferencePanel.ACCOUNT_PASSWORD, "").isEmpty();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

    }

    @Override
    public void connect() throws IOException {
        final Properties props = CloudNASPreferencePanel.getProperties();
        this.baseUrl = "https://cloud.ovh.fr/" + props.getProperty(CloudNASPreferencePanel.ACCOUNT, "");
        login(props.getProperty(CloudNASPreferencePanel.ACCOUNT_LOGIN, ""), props.getProperty(CloudNASPreferencePanel.ACCOUNT_PASSWORD, ""));
    }

    @Override
    public void disconnect() {
        // Nothing to do
    }

    public void store(final File file, String remotePath, final String title, boolean synchronous) throws IOException {
        final FileInputStream fIn = new FileInputStream(file);
        store(fIn, remotePath, title, synchronous);
        fIn.close();
    }

    public void store(final InputStream inStream, String remotePath, final String title, boolean synchronous) throws IOException {
        if (sardine == null) {
            throw new IllegalStateException("Cannot upload before login is done");
        }
        remotePath = remotePath.replace('\\', '/');
        if (!remotePath.startsWith("/")) {
            remotePath = '/' + remotePath;
        }

        final Properties props = CloudNASPreferencePanel.getProperties();
        final String[] paths = remotePath.split("/");

        final String rPath = "/" + props.getProperty(CloudNASPreferencePanel.ACCOUNT, "") + "/" + props.getProperty(CloudNASPreferencePanel.ACCOUNT_LOGIN, "") + "/OpenConcerto" + remotePath + "/"
                + title;
        final Runnable r = new Runnable() {

            @Override
            public void run() {
                String request = "";
                try {
                    request = upload(inStream, paths, rPath);

                } catch (Exception e) {
                    JOptionPane.showMessageDialog(new JFrame(), "Impossible d'envoyer le fichier " + title + " sur le Cloud NAS OVH.\n" + request);
                    e.printStackTrace();
                }
            }

        };
        if (synchronous) {
            try {
                upload(inStream, paths, rPath);
            } catch (URISyntaxException e) {
                throw new IOException(e);
            }
        } else {
            Thread t = new Thread(r);
            t.setName("Upload to OVH Cloud NAS" + remotePath + "/" + title);
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            t.start();
        }

    }

    private String upload(final InputStream inStream, final String[] paths, final String rPath) throws IOException, URISyntaxException {
        String request;
        paths[0] = "/OpenConcerto";
        String url = CloudNASStorageEngine.this.userUrl;
        for (int i = 0; i < paths.length; i++) {
            url = url + paths[i] + "/";
            if (!sardine.exists(url)) {
                System.out.println("Create dir:" + url);
                sardine.createDirectory(url);
            }

        }
        final URI uri = new URI("https", null, "cloud.ovh.fr", 443, rPath, null, null);
        // An error occur if the request contains :443
        request = uri.toASCIIString().replace("https://cloud.ovh.fr:443", "https://cloud.ovh.fr");
        sardine.put(request, inStream);
        return request;
    }

    public void login(String login, String password) throws IOException {
        if (baseUrl == null) {
            throw new IllegalStateException("Account not defined");
        }
        this.userUrl = baseUrl + "/" + login;
        sardine = SardineFactory.begin(login, password);

        final List<DavResource> resources = sardine.list(userUrl);
        if (!sardine.exists(userUrl + "/OpenConcerto")) {
            sardine.createDirectory(userUrl + "/OpenConcerto");
        }
        boolean dirFound = true;
        for (DavResource res : resources) {
            if (res.getName().endsWith("/OpenConcerto/") && res.isDirectory()) {
                dirFound = true;
            }
        }
        if (!dirFound) {
            throw new IllegalArgumentException("Unable to get directory: OpenConcerto");
        }

    }

    @Override
    public boolean allowAutoStorage() {
        try {
            final Properties props = CloudNASPreferencePanel.getProperties();
            return props.getProperty(CloudNASPreferencePanel.AUTO, "true").equals("true");
        } catch (IOException e) {
            return false;
        }

    }

}
