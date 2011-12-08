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
 
 package org.openconcerto.utils;

import org.openconcerto.utils.DesktopEnvironment.Gnome;
import org.openconcerto.utils.DesktopEnvironment.KDE;
import org.openconcerto.utils.DesktopEnvironment.Mac;
import org.openconcerto.utils.DesktopEnvironment.Windows;
import org.openconcerto.utils.io.PercentEncoder;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class EmailClient {

    public static enum EmailClientType {
        Thunderbird, AppleMail, Outlook
    }

    private static EmailClient PREFERRED = null;

    /**
     * Find the preferred email client.
     * 
     * @return the preferred email client, never <code>null</code>.
     * @throws IOException if an error occurs.
     */
    public static final EmailClient getPreferred() throws IOException {
        if (PREFERRED == null) {
            PREFERRED = findPreferred();
            // should at least return MailTo
            assert PREFERRED != null;
        }
        return PREFERRED;
    }

    /**
     * Clear the preferred client.
     */
    public static final void resetPreferred() {
        PREFERRED = null;
    }

    // XP used tabs, but not 7
    // MULTILINE since there's several lines in addition to the wanted one
    private static final Pattern registryPattern = Pattern.compile("\\s+REG_SZ\\s+(.*)$", Pattern.MULTILINE);
    private static final Pattern cmdLinePattern = Pattern.compile("(\"(.*?)\")|([^\\s\"]+)\\b");
    private static final Pattern dictPattern;
    private static final String AppleMailBundleID = "com.apple.mail";
    private static final String ThunderbirdBundleID = "org.mozilla.thunderbird";
    static {
        final String rolePattern = "(?:LSHandlerRoleAll\\s*=\\s*\"([\\w\\.]+)\";\\s*)?";
        dictPattern = Pattern.compile("\\{\\s*" + rolePattern + "LSHandlerURLScheme = mailto;\\s*" + rolePattern + "\\}");
    }

    private final static String createEncodedParam(final String name, final String value) throws IOException {
        return createParam(name, PercentEncoder.encode(value, "UTF-8"), false);
    }

    private final static String createParam(final String name, final String value) {
        return createParam(name, value, true);
    }

    private final static String createParam(final String name, final String value, final boolean quote) {
        return createParam(name, value, quote ? "'" : "", "=");
    }

    private final static String createASParam(final String name, final String value) {
        return createParam(name, value, "\"", ":");
    }

    private final static String createVBParam(final String name, final String value) {
        final String switchName = "/" + name + ":";
        if (value == null || value.length() == 0)
            return switchName;
        // we need to encode the value since when invoking cscript.exe we cannot pass "
        // since all arguments are re-parsed
        final String encoded = PercentEncoder.encodeUTF16(value);
        assert encoded.indexOf('"') < 0 : "Encoded contains a double quote, this will confuse cscript";
        return switchName + '"' + encoded + '"';
    }

    private final static String createParam(final String name, final String value, final String quote, final String sep) {
        return name + sep + quote + value + quote;
    }

    /**
     * Create a mailto URI.
     * 
     * @param to the recipient, can be <code>null</code>.
     * @param subject the subject, can be <code>null</code>.
     * @param body the body of the email, can be <code>null</code>.
     * @param attachment a file to attach, can be <code>null</code>.
     * @return the mailto URI.
     * @throws IOException if an encoding error happens.
     * @see <a href="http://tools.ietf.org/html/rfc2368">RFC 2368</a>
     */
    public final static URI getMailToURI(final String to, final String subject, final String body, final File attachment) throws IOException {
        // mailto:p.dupond@example.com?subject=Sujet%20du%20courrier&cc=pierre@example.org&bcc=jacques@example.net&body=Bonjour

        // Outlook doesn't support the to header as mandated by 2. of the RFC
        final String encodedTo = to == null ? "" : PercentEncoder.encode(to, "UTF-8");
        final List<String> l = new ArrayList<String>(4);
        if (subject != null)
            l.add(createEncodedParam("subject", subject));
        if (body != null)
            l.add(createEncodedParam("body", body));
        if (attachment != null)
            l.add(createEncodedParam("attachment", attachment.getAbsolutePath()));
        final String query = CollectionUtils.join(l, "&");
        try {
            return new URI("mailto:" + encodedTo + "?" + query);
        } catch (URISyntaxException e) {
            throw new IOException("Couldn't create mailto URI", e);
        }
    }

    private final static String getTBParam(final String to, final String subject, final String body, final File attachment) {
        // "to='john@example.com,kathy@example.com',cc='britney@example.com',subject='dinner',body='How about dinner tonight?',attachment='file:///C:/'";

        final List<String> l = new ArrayList<String>(4);
        if (to != null)
            l.add(createParam("to", to));
        if (subject != null)
            l.add(createParam("subject", subject));
        if (body != null)
            l.add(createParam("body", body));
        if (attachment != null) {
            // Thunderbird doesn't parse java URI file:/C:/
            final String tbURL = "file://" + attachment.toURI().getRawPath();
            l.add(createParam("attachment", tbURL, false));
        }

        return CollectionUtils.join(l, ",");
    }

    private final static String getAppleMailParam(final String subject, final String body) {
        final List<String> l = new ArrayList<String>(3);
        l.add("visible:true");
        if (subject != null)
            l.add(createASParam("subject", subject));
        if (body != null)
            l.add(createASParam("content", body));

        return CollectionUtils.join(l, ", ");
    }

    // @param cmdLine "C:\Program Files\Mozilla Thunderbird\thunderbird.exe" -osint -compose "%1"
    // @param toReplace "%1"
    private static String[] tbCommand(final String cmdLine, final String toReplace, final String to, final String subject, final String body, final File attachment) {
        final String composeArg = getTBParam(to, subject, body, attachment);

        final List<String> arguments = new ArrayList<String>();
        final Matcher cmdMatcher = cmdLinePattern.matcher(cmdLine);
        while (cmdMatcher.find()) {
            final String quoted = cmdMatcher.group(2);
            final String unquoted = cmdMatcher.group(3);
            assert quoted == null ^ unquoted == null : "Both quoted and unquoted, or neither quoted nor quoted: " + quoted + " and " + unquoted;
            final String arg = quoted != null ? quoted : unquoted;

            final boolean replace = arg.equals(toReplace);
            // e.g. on Linux
            if (replace && !arguments.contains("-compose"))
                arguments.add("-compose");
            arguments.add(replace ? composeArg : arg);
        }

        return arguments.toArray(new String[arguments.size()]);
    }

    /**
     * Open a composing window in the default email client.
     * 
     * @param to the recipient, can be <code>null</code>.
     * @param subject the subject, can be <code>null</code>.
     * @param body the body of the email, can be <code>null</code>.
     * @param attachment a file to attach, can be <code>null</code>.
     * @throws IOException if a program cannot be executed.
     * @throws InterruptedException if the thread is interrupted while waiting for a native program.
     */
    public void compose(final String to, final String subject, final String body, final File attachment) throws IOException, InterruptedException {
        // check now as letting the native commands do is a lot less reliable
        if (attachment != null && !attachment.exists())
            throw new IOException("Attachment doesn't exist: '" + attachment.getAbsolutePath() + "'");

        final boolean handled;
        // was only trying native if necessary, but mailto url has length limitations and can have
        // encoding issues
        handled = composeNative(to, subject, body, attachment);

        if (!handled) {
            final URI mailto = getMailToURI(to, subject, body, attachment);
            java.awt.Desktop.getDesktop().mail(mailto);
        }
    }

    static private String cmdSubstitution(String... args) throws IOException {
        return DesktopEnvironment.cmdSubstitution(Runtime.getRuntime().exec(args));
    }

    private static EmailClient findPreferred() throws IOException {
        final DesktopEnvironment de = DesktopEnvironment.getDE();
        if (de instanceof Windows) {
            // Tested on XP and 7
            // <SANS NOM> REG_SZ "C:\Program Files\Mozilla
            // Thunderbird\thunderbird.exe" -osint -compose "%1"
            final String out = cmdSubstitution("reg", "query", "HKEY_CLASSES_ROOT\\mailto\\shell\\open\\command");

            final Matcher registryMatcher = registryPattern.matcher(out);
            if (registryMatcher.find()) {
                final String cmdLine = registryMatcher.group(1);
                if (cmdLine.contains("thunderbird")) {
                    return new ThunderbirdCommandLine(cmdLine, "%1");
                } else if (cmdLine.toLowerCase().contains("outlook")) {
                    return Outlook;
                }
            }
        } else if (de instanceof Mac) {
            // (
            // {
            // LSHandlerRoleAll = "com.apple.mail";
            // LSHandlerURLScheme = mailto;
            // }
            // )
            final String bundleID;
            final String dict = cmdSubstitution("defaults", "read", "com.apple.LaunchServices", "LSHandlers");
            final Matcher dictMatcher = dictPattern.matcher(dict);
            if (dictMatcher.find()) {
                // LSHandlerRoleAll can be before or after LSHandlerURLScheme
                final String before = dictMatcher.group(1);
                final String after = dictMatcher.group(2);
                assert before == null ^ after == null : "Both before and after, or neither before nor after: " + before + " and " + after;
                bundleID = before != null ? before : after;
            } else
                // the default
                bundleID = AppleMailBundleID;

            if (bundleID.equals(AppleMailBundleID)) {
                return AppleMail;
            } else if (bundleID.equals(ThunderbirdBundleID)) {
                // doesn't work if Thunderbird is already open:
                // https://bugzilla.mozilla.org/show_bug.cgi?id=424155
                // https://bugzilla.mozilla.org/show_bug.cgi?id=472891
                // MAYBE find out if launched and let handled=false
                final String url = cmdSubstitution("osascript", "-e", "tell application id \"com.apple.Finder\" to URL of application file id \"" + bundleID + "\"").trim();
                final File appDir;
                try {
                    appDir = new File(new URI(url).getPath());
                } catch (URISyntaxException e) {
                    throw new IOException("Couldn't get the path of " + url, e);
                }
                final File exe = new File(appDir, "Contents/MacOS/thunderbird-bin");

                return new ThunderbirdPath(exe);
            }
        } else if (de instanceof Gnome) {
            // evolution %s
            final String cmdLine = cmdSubstitution("gconftool", "-g", "/desktop/gnome/url-handlers/mailto/command");
            if (cmdLine.contains("thunderbird")) {
                return new ThunderbirdCommandLine(cmdLine, "%s");
            }
        } else if (de instanceof KDE) {
            // TODO look for EmailClient=/usr/bin/thunderbird in
            // ~/.kde/share/config/emaildefaults or /etc/kde (ou /usr/share/config qui est un
            // lien symbolique vers /etc/kde)
        }

        return MailTo;
    }

    public static final EmailClient MailTo = new EmailClient(null) {
        @Override
        public boolean composeNative(String to, String subject, String body, File attachment) {
            return false;
        }
    };

    public static final EmailClient Outlook = new EmailClient(EmailClientType.Outlook) {
        @Override
        protected boolean composeNative(String to, String subject, String body, File attachment) throws IOException, InterruptedException {
            final File vbs = FileUtils.getFile(EmailClient.class.getResource("OutlookEmail.vbs"));
            final List<String> l = new ArrayList<String>(6);
            l.add("cscript");
            l.add(vbs.getAbsolutePath());
            if (to != null)
                l.add(createVBParam("to", to));
            if (subject != null)
                l.add(createVBParam("subject", subject));
            // at least set a parameter otherwise the usage get displayed
            l.add(createVBParam("unicodeStdIn", "1"));
            if (attachment != null)
                l.add(attachment.getAbsolutePath());

            final Process process = new ProcessBuilder(l).start();
            // VBScript only knows ASCII and UTF-16
            final Writer writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), "UTF-16"));
            writer.write(body);
            writer.close();
            final int returnCode = process.waitFor();
            if (returnCode != 0)
                throw new IllegalStateException("Non zero return code: " + returnCode);
            return true;
        }
    };

    public static final EmailClient AppleMail = new EmailClient(EmailClientType.AppleMail) {
        @Override
        protected boolean composeNative(String to, String subject, String body, File attachment) throws IOException, InterruptedException {
            final Process process = Runtime.getRuntime().exec(new String[] { "osascript" });
            final PrintStream w = new PrintStream(new BufferedOutputStream(process.getOutputStream()));
            // use ID to handle application renaming (always a slight delay after a rename for
            // this to work, though)
            w.println("tell application id \"" + AppleMailBundleID + "\"");
            w.println(" set theMessage to make new outgoing message with properties {" + getAppleMailParam(subject, body) + "}");
            if (to != null)
                w.println(" tell theMessage to make new to recipient with properties {address:\"" + to + "\"}");
            if (attachment != null) {
                w.println(" tell content of theMessage to make new attachment with properties {file name:\"" + attachment.getAbsolutePath() + "\"} at after last paragraph");
            }
            w.println("end tell");
            w.close();

            final int returnCode = process.waitFor();
            if (returnCode != 0)
                throw new IllegalStateException("Non zero return code: " + returnCode);
            return true;
        }
    };

    public static abstract class Thunderbird extends EmailClient {

        public static Thunderbird createFromExe(final File exe) {
            return new ThunderbirdPath(exe);
        }

        public static Thunderbird createFromCommandLine(final String cmdLine, final String toReplace) {
            return new ThunderbirdCommandLine(cmdLine, toReplace);
        }

        protected Thunderbird() {
            super(EmailClientType.Thunderbird);
        }
    }

    private static final class ThunderbirdCommandLine extends Thunderbird {

        private final String cmdLine;
        private final String toReplace;

        private ThunderbirdCommandLine(final String cmdLine, final String toReplace) {
            this.cmdLine = cmdLine;
            this.toReplace = toReplace;
        }

        @Override
        protected boolean composeNative(String to, String subject, String body, File attachment) throws IOException {
            Runtime.getRuntime().exec(tbCommand(this.cmdLine, this.toReplace, to, subject, body, attachment));
            // don't wait for Thunderbird to quit if it wasn't launched
            // (BTW return code of 1 means the program was already launched)
            return true;
        }
    }

    private static final class ThunderbirdPath extends Thunderbird {

        private final File exe;

        private ThunderbirdPath(File exe) {
            this.exe = exe;
        }

        @Override
        protected boolean composeNative(String to, String subject, String body, File attachment) throws IOException {
            final String composeArg = getTBParam(to, subject, body, attachment);
            Runtime.getRuntime().exec(new String[] { this.exe.getPath(), "-compose", composeArg });
            return true;
        }
    }

    private final EmailClientType type;

    public EmailClient(EmailClientType type) {
        this.type = type;
    }

    public final EmailClientType getType() {
        return this.type;
    }

    protected abstract boolean composeNative(final String to, final String subject, final String body, final File attachment) throws IOException, InterruptedException;

    public final static void main(String[] args) throws Exception {
        if (args.length == 1 && "--help".equals(args[0])) {
            System.out.println("Usage: java [-Dparam=value] " + EmailClient.class.getName() + " [EmailClientType args]");
            System.out.println("\tEmailClientType: mailto or " + Arrays.asList(EmailClientType.values()));
            System.out.println("\tparam: to, subject, body, file");
            return;
        }

        final EmailClient client = createFromString(args);
        final String to = System.getProperty("to", "p.dupond@example.com");
        final String subject = System.getProperty("subject", "Sujé € du courrier &; \"autre");
        final String body = System.getProperty("body", "Bonjour");
        final String filePath = System.getProperty("file");
        final File f = filePath == null || filePath.length() == 0 ? null : new File(filePath);
        client.compose(to, subject, body, f);
    }

    private static final EmailClient createFromString(final String... args) throws IOException {
        // switch doesn't support null
        if (args.length == 0)
            return getPreferred();
        else if ("mailto".equals(args[0]))
            return MailTo;

        final EmailClientType t = EmailClientType.valueOf(args[0]);
        switch (t) {
        case Outlook:
            return Outlook;
        case AppleMail:
            return AppleMail;
        case Thunderbird:
            if (args.length == 2)
                return Thunderbird.createFromExe(new File(args[1]));
            else if (args.length == 3)
                return Thunderbird.createFromCommandLine(args[1], args[2]);
            else
                throw new IllegalArgumentException(t + " needs 1 or 2 arguments");
        default:
            throw new IllegalStateException("Unknown type " + t);
        }
    }
}
