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
 
 package org.openconcerto.sql;

import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.SQLElementDirectory;
import org.openconcerto.sql.element.SQLElementDirectory.DirectoryListener;
import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.DBStructureItem;
import org.openconcerto.sql.model.DBSystemRoot;
import org.openconcerto.sql.model.HierarchyLevel;
import org.openconcerto.sql.model.SQLBase;
import org.openconcerto.sql.model.SQLDataSource;
import org.openconcerto.sql.model.SQLFilter;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLServer;
import org.openconcerto.sql.model.SQLSystem;
import org.openconcerto.sql.request.SQLFieldTranslator;
import org.openconcerto.utils.CollectionMap;
import org.openconcerto.utils.ExceptionHandler;
import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.LogUtils;
import org.openconcerto.utils.MultipleOutputStream;
import org.openconcerto.utils.ProductInfo;
import org.openconcerto.utils.StreamUtils;
import org.openconcerto.utils.cc.IClosure;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.apache.commons.collections.Predicate;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

/**
 * A configuration which takes its values primarily from Properties. You should also subclass its
 * different protected get*() methods. Used properties :
 * <dl>
 * <dt>server.ip</dt>
 * <dd>ip address of the SQL server</dd>
 * <dt>server.driver</dt>
 * <dd>the RDBMS, see {@link org.openconcerto.sql.model.SQLDataSource#DRIVERS}</dd>
 * <dt>server.login</dt>
 * <dd>the login</dd>
 * <dt>server.password</dt>
 * <dd>the password</dd>
 * <dt>server.base</dt>
 * <dd>the database (only used for systems where the root level is not SQLBase)</dd>
 * <dt>base.root</dt>
 * <dd>the name of the DBRoot</dd>
 * <dt>customer</dt>
 * <dd>used to find the default base and the mapping</dd>
 * <dt>JDBC_CONNECTION*</dt>
 * <dd>see {@link #JDBC_CONNECTION}</dd>
 * </dl>
 * 
 * @author Sylvain CUAZ
 * @see #getShowAs()
 */
public class PropsConfiguration extends Configuration {

    /**
     * Properties prefixed with this string will be passed to the datasource as connection
     * properties.
     */
    public static final String JDBC_CONNECTION = "jdbc.connection.";
    public static final String LOG = "log.level.";
    /**
     * If this system property is set to <code>true</code> then {@link #setupLogging(String)} will
     * redirect {@link System#err} and {@link System#out}.
     */
    public static final String REDIRECT_TO_FILE = "redirectToFile";

    protected static enum FileMode {
        IN_JAR, NORMAL_FILE
    };

    // eg 2009-03/26_thursday : ordered and grouped by month
    private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM/dd_EEEE");

    public static String getHostname() {
        final InetAddress addr;
        try {
            addr = InetAddress.getLocalHost();
        } catch (final UnknownHostException e) {
            e.printStackTrace();
            return "local";
        }
        return addr.getHostName();
    }

    protected static Properties create(final InputStream f, final Properties defaults) throws IOException {
        final Properties props = new Properties(defaults);
        if (f != null) {
            props.load(f);
            f.close();
        }
        return props;
    }

    public static final Properties DEFAULTS;
    static {
        DEFAULTS = new Properties();
        final File wd = new File(System.getProperty("user.dir"));
        DEFAULTS.setProperty("wd", wd.getPath());
        DEFAULTS.setProperty("customer", "test");
        DEFAULTS.setProperty("server.ip", "127.0.0.1");
        DEFAULTS.setProperty("server.login", "root");
    }

    private final Properties props;

    // sql tree
    private SQLServer server;
    private DBSystemRoot sysRoot;
    private DBRoot root;
    // rest
    private ProductInfo productInfo;
    private ShowAs showAs;
    private SQLFilter filter;
    private SQLFieldTranslator translator;
    private SQLElementDirectory directory;
    private DirectoryListener directoryListener;
    private File wd;
    private File logDir;
    // split sql tree and the rest since creating the tree is costly
    // and nodes are inter-dependant, while the rest is mostly fast
    // different instances, otherwise lock every Conf instances
    private final Object treeLock = new String("treeLock");
    private final Object restLock = new String("everythingElseLock");

    // * toAdd
    private final List<Configuration> showAsToAdd;
    private final List<Configuration> directoryToAdd;
    private final List<Configuration> translationsToAdd;

    // SSL
    private Session conn;
    private boolean isUsingSSH;

    public PropsConfiguration() throws IOException {
        this(new File("fwk_SQL.properties"), DEFAULTS);
    }

    /**
     * Creates a new setup.
     * 
     * @param f the file from which to load.
     * @param defaults the defaults, can be <code>null</code>.
     * @throws IOException if an error occurs while reading f.
     */
    public PropsConfiguration(final File f, final Properties defaults) throws IOException {
        this(new FileInputStream(f), defaults);
    }

    public PropsConfiguration(final InputStream f, final Properties defaults) throws IOException {
        this(create(f, defaults));
    }

    public PropsConfiguration(final Properties props) {
        this.props = props;
        this.showAsToAdd = new ArrayList<Configuration>();
        this.directoryToAdd = new ArrayList<Configuration>();
        this.translationsToAdd = new ArrayList<Configuration>();
        this.setUp();
    }

    @Override
    public void destroy() {
        closeSSLConnection();
        if (this.server != null) {
            this.server.destroy();
        }
        if (this.directoryListener != null)
            this.directory.removeListener(this.directoryListener);
    }

    public final String getProperty(final String name) {
        return this.props.getProperty(name);
    }

    public final String getProperty(final String name, final String def) {
        return this.props.getProperty(name, def);
    }

    // since null aren't allowed, null means remove
    protected final void setProperty(final String name, final String val) {
        if (val == null)
            this.props.remove(name);
        else
            this.props.setProperty(name, val);
    }

    protected final void setProductInfo(final ProductInfo productInfo) {
        this.productInfo = productInfo;
    }

    private void setUp() {
        this.setProductInfo(ProductInfo.getInstance());
        this.sysRoot = null;
        this.setTranslator(null);
        this.setDirectory(null);
        this.setShowAs(null);
        this.setFilter(null);
    }

    public final SQLSystem getSystem() {
        return SQLSystem.get(this.getProperty("server.driver"));
    }

    protected String getLogin() {
        return this.getProperty("server.login");
    }

    protected String getPassword() {
        return this.getProperty("server.password");
    }

    public String getDefaultBase() {
        final boolean rootIsBase = this.getSystem().getDBRootLevel().equals(HierarchyLevel.SQLBASE);
        return rootIsBase ? this.getRootName() : this.getSystemRootName();
    }

    /**
     * Return the correct stream depending on file mode. If file mode is
     * {@link FileMode#NORMAL_FILE} it will first check if a file named <code>name</code> exists,
     * otherwise it will look in the jar.
     * 
     * @param name name of the stream, eg /ilm/f.xml.
     * @return the corresponding stream, or <code>null</code> if not found.
     */
    public final InputStream getStream(final String name) {
        final File f = getFile(name);
        if (mustUseClassloader(f)) {
            return this.getClass().getResourceAsStream(name);
        } else
            try {
                return new FileInputStream(f);
            } catch (final FileNotFoundException e) {
                return null;
            }
    }

    private File getFile(final String name) {
        return new File(name.startsWith("/") ? name.substring(1) : name);
    }

    private boolean mustUseClassloader(final File f) {
        return this.getFileMode() == FileMode.IN_JAR || !f.exists();
    }

    public final String getResource(final String name) {
        final File f = getFile(name);
        if (mustUseClassloader(f)) {
            return this.getClass().getResource(name).toExternalForm();
        } else {
            return f.getAbsolutePath();
        }
    }

    protected FileMode getFileMode() {
        return FileMode.IN_JAR;
    }

    protected final DBRoot createRoot() {
        if (getRootName() != null)
            return this.getSystemRoot().getRoot(getRootName());
        else
            throw new NullPointerException("no rootname");
    }

    public String getRootName() {
        return this.getProperty("base.root");
    }

    protected SQLFilter createFilter() {
        return SQLFilter.create(this.getSystemRoot(), getDirectory());
    }

    public String getWanHostAndPort() {
        final String wanAddr = getProperty("server.wan.addr");
        final String wanPort = getProperty("server.wan.port", "22");
        return wanAddr + ":" + wanPort;
    }

    public boolean isUsingSSH() {
        return this.isUsingSSH;
    }

    protected SQLServer createServer() {
        final String wanAddr = getProperty("server.wan.addr");
        final String wanPort = getProperty("server.wan.port");
        if (wanAddr == null || wanPort == null)
            return doCreateServer();

        final Logger log = Log.get();
        Exception origExn = null;
        final SQLServer defaultServer;
        if (!"true".equals(getProperty("server.wan.only"))) {
            try {
                defaultServer = doCreateServer();
                // works since all ds params are provided by doCreateServer()
                defaultServer.getSystemRoot(getSystemRootName());
                // ok
                log.config("using " + defaultServer);

                return defaultServer;
            } catch (final RuntimeException e) {
                origExn = e;
                // on essaye par SSL
                log.config(e.getLocalizedMessage());
            }
            assert origExn != null;
        }
        this.openSSLConnection(wanAddr, Integer.valueOf(wanPort));
        this.isUsingSSH = true;
        log.info("ssl connection to " + this.conn.getHost() + ":" + this.conn.getPort());
        final int localPort = 5436;
        try {
            // TODO add and use server.port
            final String[] serverAndPort = getProperty("server.ip").split(":");
            log.info("ssl tunnel from local port " + localPort + " to remote " + serverAndPort[0] + ":" + serverAndPort[1]);
            this.conn.setPortForwardingL(localPort, serverAndPort[0], Integer.valueOf(serverAndPort[1]));
        } catch (final Exception e1) {
            throw new IllegalStateException("Impossible de créer la liaison sécurisée. Vérifier que le logiciel n'est pas déjà lancé.", e1);
        }
        setProperty("server.ip", "localhost:" + localPort);
        final SQLServer serverThruSSL = doCreateServer();
        try {
            serverThruSSL.getSystemRoot(getSystemRootName());
        } catch (final Exception e) {
            this.closeSSLConnection();
            throw new IllegalStateException("Couldn't connect through SSL : " + e.getLocalizedMessage(), origExn);
        }
        return serverThruSSL;

    }

    private SQLServer doCreateServer() {
        // give login/password as its often the case that they are the same for all the bases of a
        // server (mandated for MySQL : when the graph is built, it needs access to all the bases)
        return new SQLServer(getSystem(), this.getProperty("server.ip"), null, getLogin(), getPassword(), new IClosure<DBSystemRoot>() {
            @Override
            public void executeChecked(final DBSystemRoot input) {
                input.setRootsToMap(getRootsToMap());
                initSystemRoot(input);
            }
        }, new IClosure<SQLDataSource>() {
            @Override
            public void executeChecked(final SQLDataSource input) {
                initDS(input);
            }
        });
    }

    private void openSSLConnection(final String addr, final int port) {
        final String username = getSSLUserName();
        final String pass = getSSLPassword();
        boolean isAuthenticated = false;

        final JSch jsch = new JSch();
        try {
            if (pass == null) {
                final ByteArrayOutputStream out = new ByteArrayOutputStream(700);
                final String name = username + "_dsa";
                final InputStream in = getClass().getResourceAsStream(name);
                if (in == null)
                    throw new IllegalStateException("Missing private key " + getClass().getCanonicalName() + "/" + name);
                StreamUtils.copy(in, out);
                in.close();
                jsch.addIdentity(username, out.toByteArray(), null, null);
            }

            this.conn = jsch.getSession(username, addr, port);
            if (pass != null)
                this.conn.setPassword(pass);
            final Properties config = new Properties();
            // Set StrictHostKeyChecking property to no to avoid UnknownHostKey issue
            config.put("StrictHostKeyChecking", "no");
            // *2 gain
            config.put("compression.s2c", "zlib@openssh.com,zlib,none");
            config.put("compression.c2s", "zlib@openssh.com,zlib,none");
            this.conn.setConfig(config);
            // wait no more than 6 seconds for TCP connection
            this.conn.connect(6000);

            isAuthenticated = true;
        } catch (final Exception e) {
            throw new IllegalStateException("Connection failed", e);
        }
        if (!isAuthenticated)
            throw new IllegalStateException("Authentication failed.");
    }

    public String getSSLUserName() {
        return this.getProperty("server.wan.user");
    }

    protected String getSSLPassword() {
        return this.getProperty("server.wan.password");
    }

    private void closeSSLConnection() {
        if (this.conn != null) {
            this.conn.disconnect();
            this.conn = null;
        }
    }

    protected Collection<String> getRootsToMap() {
        final Set<String> res = new HashSet<String>();

        if (this.getRootName() != null)
            res.add(this.getRootName());
        final String rootsToMap = getProperty("systemRoot.rootsToMap");
        if (rootsToMap != null)
            res.addAll(SQLRow.toList(rootsToMap));

        return res;
    }

    public String getSystemRootName() {
        return this.getProperty("systemRoot");
    }

    protected DBSystemRoot createSystemRoot() {
        // all ds params specified by createServer()
        final DBSystemRoot res = this.getServer(false).getSystemRoot(this.getSystemRootName());
        // handle case when the root is not yet created
        if (res.getChildrenNames().contains(this.getRootName()))
            res.setDefaultRoot(this.getRootName());
        final String rootsPath = getProperty("systemRoot.rootPath");
        if (rootsPath != null) {
            for (final String root : SQLRow.toList(rootsPath))
                // not all the items of the path may exist in every databases (eg Controle.Common)
                if (res.getChildrenNames().contains(root))
                    res.appendToRootPath(root);
        }
        return res;
    }

    // called at the end of the DBSystemRoot constructor (before having a data source)
    protected void initSystemRoot(DBSystemRoot input) {
    }

    protected void initDS(final SQLDataSource ds) {
        ds.setCacheEnabled(true);
        propIterate(new IClosure<String>() {
            @Override
            public void executeChecked(final String propName) {
                final String jdbcName = propName.substring(JDBC_CONNECTION.length());
                ds.addConnectionProperty(jdbcName, PropsConfiguration.this.getProperty(propName));
            }
        }, JDBC_CONNECTION);
    }

    public final void propIterate(final IClosure<String> cl, final String startsWith) {
        this.propIterate(cl, new Predicate() {
            @Override
            public boolean evaluate(final Object propName) {
                return ((String) propName).startsWith(startsWith);
            }
        });
    }

    /**
     * Apply <code>cl</code> for each property that matches <code>filter</code>.
     * 
     * @param cl what to do for each found property.
     * @param filter which property to use.
     */
    public final void propIterate(final IClosure<String> cl, final Predicate filter) {
        for (final String propName : this.props.stringPropertyNames()) {
            if (filter.evaluate(propName)) {
                cl.executeChecked(propName);
            }
        }
    }

    /**
     * For each property starting with {@link #LOG}, set the level of the specified logger to the
     * property's value. Eg if there's "log.level.=FINE", the root logger will be set to log FINE
     * messages.
     */
    public final void setLoggersLevel() {
        this.propIterate(new IClosure<String>() {
            @Override
            public void executeChecked(final String propName) {
                final String logName = propName.substring(LOG.length());
                LogUtils.getLogger(logName).setLevel(Level.parse(getProperty(propName)));
            }
        }, LOG);
    }

    public void setupLogging() {
        this.setupLogging("logs");
    }

    public void setupLogging(final String dirName) {
        this.setupLogging(dirName, Boolean.getBoolean(REDIRECT_TO_FILE));
    }

    protected boolean keepStandardStreamsWhenRedirectingToFile() {
        return true;
    }

    protected DateFormat getLogDateFormat() {
        return DATE_FORMAT;
    }

    public void setupLogging(final String dirName, final boolean redirectToFile) {
        final File logDir;
        try {
            final File softLogDir = new File(this.getWD() + "/" + dirName + "/" + getHostname() + "-" + System.getProperty("user.name"));
            // don't throw an exception if this fails, we'll fall back to homeLogDir
            softLogDir.mkdirs();
            if (softLogDir.canWrite()) {
                logDir = softLogDir;
            } else {
                final File homeLogDir = new File(System.getProperty("user.home") + "/." + this.getAppName() + "/" + dirName);
                FileUtils.mkdir_p(homeLogDir);
                if (homeLogDir.canWrite())
                    logDir = homeLogDir;
                else
                    throw new IOException("Home log directory not writeable : " + homeLogDir);
            }
            assert logDir.exists() && logDir.canWrite();
            System.out.println("Log directory: " + logDir.getAbsolutePath());
        } catch (final IOException e) {
            throw new IllegalStateException("unable to create log dir", e);
        }
        this.logDir = logDir;
        final String logNameBase = this.getAppName() + "_" + getLogDateFormat().format(new Date());

        // must be done before setUpConsoleHandler(), otherwise log output not redirected
        if (redirectToFile) {
            final File logFile = new File(logDir, (logNameBase + ".txt"));
            try {
                FileUtils.mkdir_p(logFile.getParentFile());
                System.out.println("Log file: " + logFile.getAbsolutePath());
                final OutputStream fileOut = new FileOutputStream(logFile, true);
                final OutputStream out, err;
                System.out.println("Java System console:" + System.console());
                boolean launchedFromEclipse = new File(".classpath").exists();
                if (launchedFromEclipse) {
                    System.out.println("Launched from eclipse");
                }
                if ((System.console() != null || launchedFromEclipse) && this.keepStandardStreamsWhenRedirectingToFile()) {
                    System.out.println("Redirecting standard output to file and console");
                    out = new MultipleOutputStream(fileOut, new FileOutputStream(FileDescriptor.out));
                    System.out.println("Redirecting error output to file and console");
                    err = new MultipleOutputStream(fileOut, new FileOutputStream(FileDescriptor.err));

                } else {
                    out = fileOut;
                    err = fileOut;
                }
                System.setErr(new PrintStream(new BufferedOutputStream(err, 128), true));
                System.setOut(new PrintStream(new BufferedOutputStream(out, 128), true));
            } catch (final IOException e) {
                throw new IllegalStateException("unable to write to log file", e);
            }
            // Takes about 350ms so run it async
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        FileUtils.ln(logFile, new File(logDir, "last.log"));
                    } catch (final IOException e) {
                        // the link is not important
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            System.out.println("Standard streams not redirected to file");
        }

        // removes default
        LogUtils.rmRootHandlers();
        // add console handler
        LogUtils.setUpConsoleHandler();
        // add file handler (supports concurrent launches, doesn't depend on date)
        try {
            final File logFile = new File(logDir, this.getAppName() + "-%u-age%g.log");
            FileUtils.mkdir_p(logFile.getParentFile());
            System.out.println("Logger logs: " + logFile.getAbsolutePath());
            // 2 files of at most 5M, each new launch append
            // if multiple concurrent launches %u is used
            final FileHandler fh = new FileHandler(logFile.getPath(), 5 * 1024 * 1024, 2, true);
            fh.setFormatter(new SimpleFormatter());
            Logger.getLogger("").addHandler(fh);
        } catch (final IOException e) {
            ExceptionHandler.handle("Enregistrement du Logger désactivé", e);
        }

        this.setLoggersLevel();
    }

    public final File getLogDir() {
        return this.logDir;
    }

    public void tearDownLogging() {
        this.tearDownLogging(Boolean.getBoolean(REDIRECT_TO_FILE));
    }

    public void tearDownLogging(final boolean redirectToFile) {
        LogUtils.rmRootHandlers();
        if (redirectToFile) {
            System.out.close();
            System.err.close();
        }
    }

    protected ShowAs createShowAs() {
        final ShowAs res = new ShowAs(this.getRoot());
        assert this.directoryListener == null;
        this.directoryListener = new DirectoryListener() {
            @Override
            public void elementRemoved(final SQLElement elem) {
                res.removeTable(elem.getTable());
            }

            @Override
            public void elementAdded(final SQLElement elem) {
                final CollectionMap<String, String> sa = elem.getShowAs();
                if (sa != null) {
                    for (final Entry<String, Collection<String>> e : sa.entrySet()) {
                        try {
                            if (e.getKey() == null)
                                res.show(elem.getTable(), (List<String>) e.getValue());
                            else
                                res.show(elem.getTable().getField(e.getKey()), (List<String>) e.getValue());
                        } catch (RuntimeException exn) {
                            throw new IllegalStateException("Couldn't add showAs for " + elem + " : " + e, exn);
                        }
                    }
                }
            }
        };
        synchronized (this.getDirectory()) {
            for (final SQLElement elem : this.getDirectory().getElements()) {
                this.directoryListener.elementAdded(elem);
            }
            this.getDirectory().addListener(this.directoryListener);
        }
        return res;
    }

    protected SQLElementDirectory createDirectory() {
        return new SQLElementDirectory();
    }

    // items will be passed to #getStream(String)
    protected List<String> getMappings() {
        return Arrays.asList("mapping.xml", "mapping-" + this.getProperty("customer") + ".xml");
    }

    protected SQLFieldTranslator createTranslator() {
        final List<String> mappings = getMappings();
        if (mappings.size() == 0)
            throw new IllegalStateException("empty mappings");

        final SQLFieldTranslator trns = new SQLFieldTranslator(this.getRoot(), PropsConfiguration.class.getResourceAsStream("mapping.xml"), this.getDirectory());
        for (final String m : mappings) {
            // do not force to have one mapping for each client
            final InputStream in = this.getStream(m);
            if (in != null)
                trns.load(this.getRoot(), in);
        }
        return trns;
    }

    protected File createWD() {
        return new File(this.getProperty("wd"));
    }

    // *** add

    /**
     * Add the passed Configuration to this. If an item is not already created, this method won't,
     * instead the item to add will be stored. Also items of this won't be replaced by those of
     * <code>conf</code>.
     * 
     * @param conf the conf to add.
     */
    @Override
    public final Configuration add(final Configuration conf) {
        if (this.showAs != null)
            this.getShowAs().putAll(conf.getShowAs());
        else
            this.showAsToAdd.add(conf);

        if (this.translator != null)
            this.getTranslator().putAll(conf.getTranslator());
        else
            this.translationsToAdd.add(conf);

        if (this.directory != null)
            this.getDirectory().putAll(conf.getDirectory());
        else
            this.directoryToAdd.add(conf);

        return this;
    }

    // *** getters

    @Override
    public final ShowAs getShowAs() {
        synchronized (this.restLock) {
            if (this.showAs == null) {
                this.setShowAs(this.createShowAs());
                for (final Configuration s : this.showAsToAdd)
                    this.getShowAs().putAll(s.getShowAs());
                this.showAsToAdd.clear();
            }
        }
        return this.showAs;
    }

    @Override
    public final SQLBase getBase() {
        return this.getNode(SQLBase.class);
    }

    @Override
    public final DBRoot getRoot() {
        synchronized (this.treeLock) {
            if (this.root == null)
                this.setRoot(this.createRoot());
        }
        return this.root;
    }

    @Override
    public final DBSystemRoot getSystemRoot() {
        synchronized (this.treeLock) {
            if (this.sysRoot == null)
                this.sysRoot = this.createSystemRoot();
        }
        return this.sysRoot;
    }

    /**
     * Get the node of the asked class, creating just the necessary instances (ie getNode(Server)
     * won't do a getBase().getServer()).
     * 
     * @param <T> the type wanted.
     * @param clazz the class wanted, eg SQLBase.class, DBSystemRoot.class.
     * @return the corresponding instance, eg getBase() for SQLBase, getServer() or getBase() for
     *         DBSystemRoot depending on the SQL system.
     */
    public final <T extends DBStructureItem<?>> T getNode(final Class<T> clazz) {
        final SQLSystem sys = this.getServer().getSQLSystem();
        final HierarchyLevel l = sys.getLevel(clazz);
        if (l == HierarchyLevel.SQLSERVER)
            return this.getServer().getAnc(clazz);
        else if (l == sys.getLevel(DBSystemRoot.class))
            return this.getSystemRoot().getAnc(clazz);
        else if (l == sys.getLevel(DBRoot.class))
            return this.getRoot().getAnc(clazz);
        else
            throw new IllegalArgumentException("doesn't know an item of " + clazz);
    }

    public final SQLServer getServer() {
        return this.getServer(true);
    }

    private final SQLServer getServer(final boolean initSysRoot) {
        synchronized (this.treeLock) {
            if (this.server == null) {
                this.setServer(this.createServer());
                // necessary otherwise the returned server has no datasource
                // (eg getChildren() will fail)
                if (initSysRoot && this.server.getSQLSystem().getLevel(DBSystemRoot.class) == HierarchyLevel.SQLSERVER)
                    this.getSystemRoot();
            }
        }
        return this.server;
    }

    @Override
    public final SQLFilter getFilter() {
        synchronized (this.restLock) {
            if (this.filter == null)
                this.setFilter(this.createFilter());
        }
        return this.filter;
    }

    @Override
    public final SQLFieldTranslator getTranslator() {
        synchronized (this.restLock) {
            if (this.translator == null) {
                this.setTranslator(this.createTranslator());
                for (final Configuration s : this.translationsToAdd)
                    this.getTranslator().putAll(s.getTranslator());
                this.translationsToAdd.clear();
            }
        }
        return this.translator;
    }

    @Override
    public final SQLElementDirectory getDirectory() {
        synchronized (this.restLock) {
            if (this.directory == null) {
                this.setDirectory(this.createDirectory());
                for (final Configuration s : this.directoryToAdd)
                    this.getDirectory().putAll(s.getDirectory());
                this.directoryToAdd.clear();
            }
        }
        return this.directory;
    }

    public final ProductInfo getProductInfo() {
        return this.productInfo;
    }

    @Override
    public final String getAppName() {
        return this.getProductInfo().getName();
    }

    @Override
    public final File getWD() {
        synchronized (this.restLock) {
            if (this.wd == null)
                this.setWD(this.createWD());
        }
        return this.wd;
    }

    // *** setters

    // MAYBE add synchronized (not necessary since they're private, and only called with the lock)

    private final void setFilter(final SQLFilter filter) {
        this.filter = filter;
    }

    private void setServer(final SQLServer server) {
        this.server = server;
    }

    private final void setRoot(final DBRoot root) {
        this.root = root;
    }

    private final void setShowAs(final ShowAs showAs) {
        this.showAs = showAs;
    }

    private final void setTranslator(final SQLFieldTranslator translator) {
        this.translator = translator;
    }

    private final void setDirectory(final SQLElementDirectory directory) {
        this.directory = directory;
    }

    private final void setWD(final File dir) {
        this.wd = dir;
    }
}
