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
 
 package org.openconcerto.erp.config;

import org.openconcerto.sql.Configuration;
import org.openconcerto.utils.ExceptionUtils;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.StreamUtils;
import org.openconcerto.utils.Unzip;

import java.io.File;
import java.io.IOException;

// import javax.jnlp.ExtensionInstallerService;
// import javax.jnlp.ServiceManager;
// import javax.jnlp.UnavailableServiceException;
import javax.swing.JOptionPane;

public class GestionJNLPInstaller {

    public static final String DB_CACHE_PATH = "webstart/DBCache.zip";

    public static void main(String[] args) {
//        if (args.length == 0)
//            return;
//
//        final String arg0 = args[0].trim().toLowerCase();
//        try {
//            final ExtensionInstallerService installer = (ExtensionInstallerService) ServiceManager.lookup(ExtensionInstallerService.class.getName());
//            try {
//                final ComptaPropsConfiguration conf = ComptaPropsConfiguration.create();
//                Configuration.setInstance(conf);
//                if (arg0.equals("install")) {
//                    if (conf.isServerless()) {
//                        // force creation of DB in the install phase.
//                        conf.getSystemRoot();
//                    } else {
//                        // don't bother for embedded DB
//                        try {
//                            final File dir = getDBCacheDir(conf);
//                            // don't overwrite
//                            if (!dir.exists() || dir.listFiles().length == 0) {
//                                FileUtils.mkdir_p(dir);
//                                final File tmp = File.createTempFile(GestionJNLPInstaller.class.getName(), "DBCache.zip");
//                                tmp.deleteOnExit();
//                                StreamUtils.copy(GestionJNLPInstaller.class.getResourceAsStream("/" + DB_CACHE_PATH), tmp);
//                                Unzip.toDir(tmp, dir);
//                                tmp.delete();
//                            }
//                        } catch (Exception e) {
//                            // not required, so don't abort installation if it fails
//                            e.printStackTrace();
//                        }
//                    }
//                    conf.destroy();
//                } else if (arg0.equals("uninstall")) {
//                    // dataDir is below confDir
//                    final File toRm = conf.getConfDir();
//                    // close all resources so we can delete
//                    conf.destroy();
//                    if (!FileUtils.rmR(toRm))
//                        throw new IOException("Couldn't delete " + toRm);
//                    assert !toRm.exists() && !conf.getDataDir().exists();
//                } else {
//                    throw new IllegalArgumentException("Unknown argument " + arg0);
//                }
//                installer.setStatus("OK");
//                installer.installSucceeded(false);
//            } catch (Throwable e) {
//                // haven't found another way to report the error
//                e.printStackTrace();
//                JOptionPane.showMessageDialog(null, "Install failed : " + ExceptionUtils.getStackTrace(e));
//                installer.setStatus("Install failed : " + e);
//                installer.installFailed();
//            }
//        } catch (UnavailableServiceException e) {
//            throw new IllegalStateException(e);
//        }
    }

    private static File getDBCacheDir(final ComptaPropsConfiguration conf) {
        // getFileCache() needs it
        assert Configuration.getInstance() == conf;
        return conf.getServer().getFileCache().getServerCache().getDir();
    }

    // used by ant
    public static String getDBCacheDir(final String confFile) {
        final ComptaPropsConfiguration conf = ComptaPropsConfiguration.create(false, new File(confFile));
        Configuration.setInstance(conf);
        final String res = getDBCacheDir(conf).getAbsolutePath();
        conf.destroy();
        return res;
    }
}
