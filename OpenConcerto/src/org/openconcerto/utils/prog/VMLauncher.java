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
 
 package org.openconcerto.utils.prog;

import org.openconcerto.utils.FileUtils;
import org.openconcerto.utils.OSFamily;
import org.openconcerto.utils.ProcessStreams;
import org.openconcerto.utils.ProcessStreams.Action;
import org.openconcerto.utils.PropertiesUtils;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A class meant to be used as the main class of a jar and which launch another instance of the Java
 * VM.
 * 
 * @author Sylvain
 * @see #launch(String, List)
 */
public abstract class VMLauncher {

    /**
     * Boolean system property, if set to <code>true</code> then {@link #restart(Class, List)} will
     * simply return <code>null</code>. Useful e.g. when using IDE launch configuration (to debug).
     */
    static public final String NO_RESTART = "vm.noRestart";

    private static NativeLauncherFinder getNativeAppLauncher() {
        final OSFamily os = OSFamily.getInstance();
        final NativeLauncherFinder l;
        if (os.equals(OSFamily.Windows)) {
            l = new WinLauncherFinder();
        } else if (os.equals(OSFamily.Mac)) {
            l = new MacLauncherFinder();
        } else {
            l = UnknownLauncherFinder;
        }
        return l;
    }

    private static List<String> getNativeCommand(List<String> args) {
        final NativeLauncherFinder l = getNativeAppLauncher();
        return l.getAppPath() == null ? null : l.getCommand(args);
    }

    /**
     * Allow to find out if the running VM was launched using a native application.
     * 
     * @author Sylvain
     */
    private static abstract class NativeLauncherFinder {
        private final String cp, firstItem;

        public NativeLauncherFinder() {
            this.cp = ManagementFactory.getRuntimeMXBean().getClassPath();
            final int sepIndex = this.cp.indexOf(File.pathSeparatorChar);
            this.firstItem = sepIndex < 0 ? this.cp : this.cp.substring(0, sepIndex);
        }

        public final String getClassPath() {
            return this.cp;
        }

        public final String getFirstItem() {
            return this.firstItem;
        }

        /**
         * The path to the native application if any.
         * 
         * @return the path, <code>null</code> if no native application could be found.
         */
        public abstract String getAppPath();

        /**
         * The command to launch this application with the passed arguments.
         * 
         * @param args the program arguments.
         * @return the command.
         */
        public abstract List<String> getCommand(final List<String> args);
    }

    private static class MacLauncherFinder extends NativeLauncherFinder {
        private static final String APP_EXT = ".app";
        private static final Pattern MAC_PATTERN = Pattern.compile(Pattern.quote(APP_EXT) + "/Contents/Resources(/Java)?/[^/]+\\.jar$");

        @Override
        public String getAppPath() {
            final Matcher matcher = MAC_PATTERN.matcher(this.getFirstItem());
            if (matcher.matches()) {
                final String appPath = getFirstItem().substring(0, matcher.start() + APP_EXT.length());
                final File contentsDir = new File(appPath, "Contents");
                final List<String> bundleContent = Arrays.asList(contentsDir.list());
                if (bundleContent.contains("Info.plist") && bundleContent.contains("PkgInfo") && new File(contentsDir, "MacOS").isDirectory())
                    return appPath;
            }
            return null;
        }

        @Override
        public List<String> getCommand(List<String> args) {
            final List<String> command = new ArrayList<String>(4 + args.size());
            command.add("open");
            // since we restarting we need to launch a new instance of us
            command.add("-n");
            command.add(getAppPath());
            command.add("--args");
            command.addAll(args);
            return command;
        }
    }

    private static class WinLauncherFinder extends NativeLauncherFinder {
        @Override
        public String getAppPath() {
            // launch4j
            if (this.getFirstItem().endsWith(".exe"))
                return getFirstItem();
            else
                return null;
        }

        @Override
        public List<String> getCommand(List<String> args) {
            final List<String> command = new ArrayList<String>(4 + args.size());
            command.add(getAppPath());
            command.addAll(args);
            return command;
        }
    }

    private static final NativeLauncherFinder UnknownLauncherFinder = new NativeLauncherFinder() {
        @Override
        public String getAppPath() {
            return null;
        }

        @Override
        public List<String> getCommand(List<String> args) {
            throw new UnsupportedOperationException();
        }
    };

    public static final Process restart(final Class<?> mainClass, final String... args) throws IOException {
        return restart(mainClass, Arrays.asList(args));
    }

    public static final Process restart(final Action action, final Class<?> mainClass, final String... args) throws IOException {
        return restart(action, mainClass, Arrays.asList(args));
    }

    /**
     * Restart the VM. If this VM was launched using a native application (e.g. .exe or .app) then
     * this will be executed. Else the <code>mainClass</code> will be used.
     * 
     * @param mainClass the main() to use (if no native application was found).
     * @param args the program arguments to pass.
     * @return the new process, <code>null</code> if the program wasn't started.
     * @throws IOException if the VM couldn't be launched.
     * @see #NO_RESTART
     */
    public static final Process restart(final Class<?> mainClass, final List<String> args) throws IOException {
        return restart(Action.CLOSE, mainClass, args);
    }

    public static final Process restart(final Action action, final Class<?> mainClass, final List<String> args) throws IOException {
        if (Boolean.getBoolean(NO_RESTART))
            return null;
        final File wd = FileUtils.getWD();
        final List<String> command = getNativeCommand(args);
        if (command != null) {
            return ProcessStreams.handle(new ProcessBuilder(command).directory(wd).start(), action);
        } else {
            try {
                mainClass.getMethod("main", String[].class);
            } catch (NoSuchMethodException e) {
                throw new IllegalArgumentException(mainClass + " doesn't containt a main()", e);
            }
            return new VMLauncher() {
                @Override
                protected File getWD() {
                    return wd;
                }

                @Override
                protected File getPropFile(String mainClass) {
                    return null;
                }

                @Override
                protected Action getStreamAction() {
                    return action;
                }
            }.launch(mainClass.getName(), args);
        }
    }

    public static final String ENV_VMARGS = "JAVA_VMARGS";
    public static final String PROPS_VMARGS = "VMARGS";
    public static final String ENV_PROGARGS = "JAVA_PROGARGS";

    // handle DOS, Mac and Unix newlines
    private static final Pattern NL = Pattern.compile("\\p{Cntrl}+");

    private File wd;

    public VMLauncher() {
        this.wd = null;
    }

    public final File getLauncherWD() {
        if (this.wd == null) {
            final NativeLauncherFinder nativeAppLauncher = getNativeAppLauncher();
            final String appPath = nativeAppLauncher.getAppPath();
            if (appPath != null)
                this.wd = new File(appPath).getAbsoluteFile().getParentFile();
            // when launched with -jar there's only one item
            else if (nativeAppLauncher.getFirstItem().equals(nativeAppLauncher.getClassPath()) && new File(nativeAppLauncher.getFirstItem()).isFile())
                this.wd = new File(nativeAppLauncher.getFirstItem()).getParentFile();
            // support launch in an IDE
            else
                this.wd = FileUtils.getWD();
        }
        return this.wd;
    }

    private final List<String> split(String res) {
        res = res.trim();
        if (res.length() == 0) {
            return Collections.emptyList();
        } else {
            return Arrays.asList(NL.split(res));
        }
    }

    private final List<String> getProp(final File propFile, final String propName) {
        return this.getProp(this.getProps(propFile), propName);
    }

    private final Properties getProps(final File propFile) {
        if (propFile != null && propFile.canRead()) {
            try {
                return PropertiesUtils.createFromFile(propFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new Properties();
    }

    private final List<String> getProp(final Properties props, final String propName) {
        String res = "";
        if (props != null) {
            res = props.getProperty(propName, res);
        }
        return split(res);
    }

    protected final Process launch(final String mainClass) throws IOException {
        return this.launch(mainClass, Collections.<String> emptyList());
    }

    /**
     * Launch a new Java VM. This method will try to launch {@link #getJavaBinary() java} from the
     * same installation, if that fails it will use the system path for binaries. VM arguments can
     * be specified with :
     * <ol>
     * <li>the {@value #ENV_VMARGS} environment variable</li>
     * <li>the {@value #PROPS_VMARGS} property in {@link #getPropFile(String)}</li>
     * <li>the {@link #getVMArguments()} method</li>
     * </ol>
     * Program arguments :
     * <ol>
     * <li>the <code>progParams</code> parameter</li>
     * <li>the {@value #ENV_PROGARGS} environment variable</li>
     * </ol>
     * 
     * @param mainClass the main class.
     * @param progParams the program arguments for <code>mainClass</code>.
     * @return the new Process.
     * @throws IOException if the process couldn't be started.
     */
    protected final Process launch(final String mainClass, final List<String> progParams) throws IOException {
        final boolean debug = Boolean.getBoolean("launcher.debug");
        final String javaBinary = getJavaBinary();
        final File sameJava = new File(System.getProperty("java.home"), "bin/" + javaBinary);
        final String java = sameJava.canExecute() ? sameJava.getAbsolutePath() : javaBinary;
        final File propFile = this.getPropFile(mainClass);
        final Properties props = this.getProps(propFile);
        if (debug)
            System.err.println("propFile : " + propFile);

        final List<String> command = new ArrayList<String>();
        command.add(java);

        if (this.enableRemoteDebug(props)) {
            command.add(RemoteDebugArgs.getArgs());
        }
        command.addAll(this.getVMArguments());

        // for java the last specified property wins
        if (propFile != null) {
            final List<String> appProps = this.getProp(props, PROPS_VMARGS);
            command.addAll(appProps);
            final File userFile = new File(System.getProperty("user.home"), ".java/ilm/" + propFile.getName());
            final List<String> userProps = this.getProp(userFile, PROPS_VMARGS);
            command.addAll(userProps);
            if (debug) {
                System.err.println("appProps : " + appProps);
                System.err.println("userProps ( from " + userFile + ") : " + userProps);
            }
        }
        final String envVMArgs = System.getenv(ENV_VMARGS);
        if (envVMArgs != null)
            command.addAll(split(envVMArgs));

        command.add("-cp");
        command.add(getClassPath());
        command.add(mainClass);
        final String envProgArgs = System.getenv(ENV_PROGARGS);
        if (envProgArgs != null)
            command.addAll(split(envProgArgs));
        command.addAll(progParams);

        // inherit environment so that the next launch() can access the same variables
        final ProcessBuilder procBuilder = new ProcessBuilder(command).directory(getWD());
        this.modifyEnv(procBuilder.environment());
        if (debug) {
            System.err.println("Command line : " + procBuilder.command());
            System.err.println("Dir : " + procBuilder.directory());
            System.err.println("Std out and err :");
        }

        final Process res = procBuilder.start();
        ProcessStreams.handle(res, debug ? Action.REDIRECT : this.getStreamAction());

        return res;
    }

    protected void modifyEnv(Map<String, String> environment) {
    }

    protected Action getStreamAction() {
        return Action.CLOSE;
    }

    protected boolean enableRemoteDebug(Properties props) {
        final String prop = props.getProperty("remoteDebug");
        return prop == null ? remoteDebugDefault() : Boolean.parseBoolean(prop);
    }

    protected boolean remoteDebugDefault() {
        return false;
    }

    /**
     * The program to launch. This implementation returns <code>javaw</code> for Windows and
     * <code>java</code> for other OS.
     * 
     * @return the name of the binary.
     */
    protected String getJavaBinary() {
        return OSFamily.getInstance() == OSFamily.Windows ? "javaw" : "java";
    }

    protected List<String> getVMArguments() {
        return Arrays.asList("-Dfile.encoding=UTF-8", "-Xms100M", "-Xmx256M");
    }

    // by default in the same jar
    protected String getClassPath() {
        return ManagementFactory.getRuntimeMXBean().getClassPath();
    }

    // by default in the same directory
    protected File getWD() {
        return this.getLauncherWD();
    }

    protected File getPropFile(final String mainClass) {
        final String className = mainClass.substring(mainClass.lastIndexOf('.') + 1);
        return new File(getWD(), className + ".properties");
    }
}
