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
 
 package org.openconcerto.sql.utils;

import org.openconcerto.sql.model.DBRoot;
import org.openconcerto.sql.model.SQLField;
import org.openconcerto.sql.model.SQLTable;
import org.openconcerto.utils.FileUtils;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.imageio.ImageIO;

public class AppCodeGenerator {
    final private String name;
    final private String packageName;
    final private File projectDir;
    final private File srcDir;
    private Set<String> sqlElements = new HashSet<String>();
    private Map<String, SQLTable> tablesDir = new HashMap<String, SQLTable>();// Nom de classe -

    // SQLTable

    public AppCodeGenerator(String applicationName, String packageName, File projetDirectory) {
        this.name = applicationName;
        this.packageName = packageName;
        this.projectDir = projetDirectory;
        if (!projetDirectory.exists()) {
            throw new IllegalArgumentException("The project directory doesn't exist : " + projetDirectory.getAbsolutePath());
        }
        if (!projetDirectory.isDirectory()) {
            throw new IllegalArgumentException(projetDirectory.getAbsolutePath() + " is not a directory");
        }
        this.srcDir = new File(projectDir, "src/" + packageName.replace('.', '/'));
        srcDir.mkdirs();
        new File(srcDir, "config").mkdir();
    }

    public void createFromRoot(DBRoot root) {
        long t1 = System.currentTimeMillis();
        System.out.println("Creating Mapping");
        createMapping(root);
        System.out.println("Creating Model");
        createRowBacked(root);
        System.out.println("Creating UI");
        createAutoLayoutedJComponent(root);
        createActions(root);
        createApp(root);

        createConfiguration(root);
        System.out.println("Done in " + (System.currentTimeMillis() - t1) + " ms");
    }

    private void createApp(DBRoot root) {
        FileOutputStream fop;
        try {
            fop = new FileOutputStream(new File(srcDir, "config/AppOk.java"));
            final PrintStream out = new PrintStream(fop);
            out.println("package " + packageName + ".config;");
            out.println();
            out.println("import org.openconcerto.sql.Configuration;");
            out.println("import org.openconcerto.sql.request.ComboSQLRequest;");
            out.println("import org.openconcerto.ui.FrameUtil;");
            out.println("import org.openconcerto.utils.JImage;");
            out.println("import org.openconcerto.utils.OSXAdapter;");
            out.println("");
            out.println("import java.awt.Color;");
            out.println("import java.awt.Container;");
            out.println("import java.awt.GridBagConstraints;");
            out.println("import java.awt.GridBagLayout;");
            out.println("import java.awt.Image;");
            out.println("import java.awt.Toolkit;");
            out.println("import java.awt.event.WindowAdapter;");
            out.println("import java.awt.event.WindowEvent;");
            out.println("import java.util.ArrayList;");
            out.println("import java.util.List;");

            out.println("import javax.swing.ImageIcon;");
            out.println("import javax.swing.JFrame;");
            out.println("import javax.swing.JMenu;");
            out.println("import javax.swing.JMenuBar;");
            out.println("import javax.swing.JMenuItem;");
            out.println("import javax.swing.JPanel;");
            out.println("import javax.swing.JSeparator;");
            out.println("import javax.swing.SwingUtilities;");
            out.println("import javax.swing.ToolTipManager;");
            out.println("import javax.swing.UIManager;");

            for (String element : sqlElements) {
                out.println("import " + packageName + ".action." + element.replaceAll("SQLElement", "CreateAction") + ";");

            }

            for (String element : sqlElements) {
                out.println("import " + packageName + ".action.list." + element.replaceAll("SQLElement", "ListAction") + ";");
            }

            out.println("public class AppOk extends JFrame {");

            out.println("    private static List<Image> frameIcon;");
            out.println("    // Check that we are on Mac OS X. This is crucial to loading and using the OSXAdapter class.");
            out.println("    private static final boolean MAC_OS_X = System.getProperty(\"os.name\").toLowerCase().startsWith(\"mac os x\");");
            out.println();

            out.println("    public static void main(String[] args) throws Exception {");
            out.println("        final long startTime = System.currentTimeMillis();");
            out.println("        // Mac, only works with Aqua laf");
            out.println("        System.setProperty(\"apple.laf.useScreenMenuBar\", \"true\");");
            out.println("        // Instant tooltips");
            out.println("        ToolTipManager.sharedInstance().setInitialDelay(0);");
            out.println("        // SpeedUp Linux");
            out.println("        System.setProperty(\"sun.java2d.pmoffscreen\", \"false\");");
            out.println("        ");
            out.println("        System.setProperty(\"org.openconcerto.editpanel.noborder\", \"true\");");
            out.println("        System.setProperty(\"org.openconcerto.editpanel.separator\", \"true\");");
            out.println("        System.setProperty(\"org.openconcerto.sql.listPanel.deafEditPanel\", \"true\");");
            out.println("        System.setProperty(\"org.openconcerto.ui.addComboButton\", \"true\");");
            out.println("        System.setProperty(\"org.openconcerto.sql.structure.useXML\", \"true\");");
            out.println("        // don't put any suffix, rely on Animator");
            out.println("        System.setProperty(\"org.openconcerto.sql.requiredSuffix\", \"\");");
            out.println("        System.setProperty(\"org.openconcerto.ui.addComboButton\", \"true\");");
            out.println("        System.setProperty(\"org.openconcerto.ui.removeSwapSearchCheckBox\", \"true\");");
            out.println("        // configure Swing UI");
            out.println("        final String nimbusClassName = FrameUtil.getNimbusClassName();");
            out.println("        if (nimbusClassName == null || !System.getProperties().getProperty(\"os.name\").toLowerCase().contains(\"linux\")) {");
            out.println("            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());");
            out.println("        } else {");
            out.println("            UIManager.setLookAndFeel(nimbusClassName);");
            out.println("            UIManager.put(\"control\", new Color(240, 240, 240));");
            out.println("            UIManager.put(\"Table.showGrid\", Boolean.TRUE);");
            out.println("            UIManager.put(\"FormattedTextField.background\", new Color(240, 240, 240));");
            out.println("        }");
            out.println("        Toolkit.getDefaultToolkit().setDynamicLayout(true);");
            out.println();
            out.println("        // frameworks configuration");
            out.println("        ComboSQLRequest.setDefaultFieldSeparator(\" \");");
            out.println("        final AppPropsConfiguration conf = AppPropsConfiguration.create();");
            out.println("        conf.setupLogging();");
            out.println("        Configuration.setInstance(conf);");
            out.println("        conf.getBase();");

            out.println("        System.err.println(\"Application started in \" + (System.currentTimeMillis() - startTime) + \" ms\");");
            out.println("        SwingUtilities.invokeLater(new Runnable() {");
            out.println("            public void run() {");
            out.println("                AppOk appOk = new AppOk();");
            out.println("                appOk.pack();");
            out.println("                appOk.setVisible(true);");
            out.println("            }");
            out.println("        });");
            out.println("    }");

            out.println("    public AppOk() {");
            out.println("        this.setTitle(\"" + this.name + "\");");
            out.println("        this.setJMenuBar(createMenu());");
            out.println("        final Container co = this.getContentPane();");
            out.println("        co.setLayout(new GridBagLayout());");
            out.println("        final GridBagConstraints c = new GridBagConstraints();");
            out.println("        c.fill = GridBagConstraints.HORIZONTAL;");
            out.println("        c.gridx = 0;");
            out.println("        c.gridy = 0;");
            out.println("        c.weightx = 1;");
            out.println("        c.weighty = 0;");
            out.println("        final JImage image = new JImage(this.getClass().getResource(\"logo.png\"));");
            out.println("        image.setBackground(Color.WHITE);");
            out.println("        image.check();");
            out.println("        co.add(image, c);");
            out.println("        c.weighty = 0;");
            out.println("        c.gridy++;");
            out.println("        c.fill = GridBagConstraints.BOTH;");
            out.println("        co.add(new JSeparator(JSeparator.HORIZONTAL), c);");
            out.println("        c.gridy++;");
            out.println("        c.weighty = 1;");
            out.println("        co.add(new JPanel(), c);");
            out.println("        ");
            out.println("        registerForMacOSXEvents();");
            out.println("        this.addWindowListener(new WindowAdapter() {");
            out.println("            public void windowClosing(WindowEvent arg0) {");
            out.println("                quit();");
            out.println("            }");
            out.println("        });");
            out.println("    }");

            out.println("    // Generic registration with the Mac OS X application menu");
            out.println("    // Checks the platform, then attempts to register with the Apple EAWT");
            out.println("    // See OSXAdapter.java to see how this is done without directly referencing any Apple APIs");
            out.println("    public void registerForMacOSXEvents() {");
            out.println("        if (MAC_OS_X) {");
            out.println("            try {");
            out.println("                // Generate and register the OSXAdapter, passing it a hash of all the methods we");
            out.println("                // wish to use as delegates for various com.apple.eawt.ApplicationListener methods");
            out.println("                OSXAdapter.setQuitHandler(this, getClass().getDeclaredMethod(\"quit\", new Class[0]));");
            out.println("                OSXAdapter.setAboutHandler(this, getClass().getDeclaredMethod(\"about\", new Class[0]));");
            out.println("                OSXAdapter.setPreferencesHandler(this, getClass().getDeclaredMethod(\"preferences\", new Class[0]));");
            out.println("                // no OSXAdapter.setFileHandler() for now");
            out.println("            } catch (Exception e) {");
            out.println("                System.err.println(\"Error while loading the OSXAdapter:\");");
            out.println("                e.printStackTrace();");
            out.println("            }");
            out.println("        }");
            out.println("    }");

            out.println("    // used by OSXAdapter");
            out.println("    public final void preferences() {");
            out.println("    }");
            out.println("    ");
            out.println("    public final void about() {");
            out.println("    }");
            out.println("    ");
            out.println("    public boolean quit() {");
            out.println("        return true;");
            out.println("    }");
            out.println("    ");
            out.println("    public List<Image> getFrameIcon() {");
            out.println("        if (frameIcon == null) {");
            out.println("            frameIcon = new ArrayList<Image>();");
            out.println("            frameIcon.add(new ImageIcon(getClass().getResource(\"16.png\")).getImage());");
            out.println("            frameIcon.add(new ImageIcon(getClass().getResource(\"32.png\")).getImage());");
            out.println("            frameIcon.add(new ImageIcon(getClass().getResource(\"48.png\")).getImage());");
            out.println("            frameIcon.add(new ImageIcon(getClass().getResource(\"96.png\")).getImage());");
            out.println("        }");
            out.println("        return frameIcon;");
            out.println("    }");

            out.println("    public JMenuBar createMenu() {");
            out.println("        JMenuBar result = new JMenuBar();");

            out.println("        JMenu menuCreation;");
            out.println("        JMenuItem item;");
            int c = 0;
            for (String element : sqlElements) {
                if (c % 20 == 0) {
                    out.println("        menuCreation = new JMenu(\"Saisies" + ((c / 20) + 1) + "\");");
                    out.println("        result.add(menuCreation);");
                }
                out.println("        item = new JMenuItem(new " + element.replaceAll("SQLElement", "CreateAction") + "());");
                out.println("        menuCreation.add(item);");
                c++;
            }
            c = 0;
            out.println("        JMenu menuListe;");
            for (String element : sqlElements) {
                if (c % 20 == 0) {
                    out.println("        menuListe = new JMenu(\"Listes" + ((c / 20) + 1) + "\");");
                    out.println("        result.add(menuListe);");
                }

                out.println("        item = new JMenuItem(new " + element.replaceAll("SQLElement", "ListAction") + "());");
                out.println("        menuListe.add(item);");
                c++;
            }

            out.println("        result.add(menuListe);");
            out.println("        return result;");
            out.println("   }");
            out.println("}");
            fop.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void createActions(DBRoot root) {
        File dir = new File(srcDir, "action/list");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        FileOutputStream fop;
        try {
            fop = new FileOutputStream(new File(srcDir, "action/CreateFrameAbstractAction.java"));
            final PrintStream out = new PrintStream(fop);

            out.print("package ");
            if (packageName != null && packageName.length() > 0) {
                out.print(packageName + ".");
            }
            out.print("action");
            out.println(";");
            out.println();

            out.println("import org.openconcerto.sql.Configuration;");
            out.println("import org.openconcerto.ui.FrameUtil;");
            out.println("import org.openconcerto.ui.state.WindowStateManager;");

            out.println("import java.awt.Dimension;");
            out.println("import java.awt.event.ActionEvent;");
            out.println("import java.io.File;");

            out.println("import javax.swing.AbstractAction;");
            out.println("import javax.swing.Action;");
            out.println("import javax.swing.JFrame;");

            out.println("public abstract class CreateFrameAbstractAction extends AbstractAction {");

            out.println("    protected CreateFrameAbstractAction() {");
            out.println("        super();");
            out.println("    }");

            out.println("    protected CreateFrameAbstractAction(String name) {");
            out.println("        super(name);");
            out.println("    }");

            out.println("     public void actionPerformed(ActionEvent e) {");
            out.println("         final JFrame frame = createFrame();");

            out
                    .println("     WindowStateManager stateManager = new WindowStateManager(frame, new File(Configuration.getInstance().getConfDir(), \"Configuration\" + File.separator + \"Frame\" + File.separator + this.getValue(Action.NAME).toString() + \".xml\"), true);");

            out.println("     frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);");
            out.println("     frame.pack();");
            out.println("     // Evite les saute de Layout");
            out.println("     frame.setMinimumSize(new Dimension(frame.getWidth(), frame.getHeight()));");
            out.println("     stateManager.loadState();");
            out.println("     FrameUtil.show(frame);");

            out.println("}");

            out.println("    abstract public JFrame createFrame();");
            out.println("}");
            out.close();
            fop.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        // Create
        for (String element : sqlElements) {
            String className = element.replaceAll("SQLElement", "CreateAction");

            String tableName = tablesDir.get(element).getName();
            String name = tableName.toLowerCase();
            try {
                fop = new FileOutputStream(new File(srcDir, "action/" + className + ".java"));
                final PrintStream out = new PrintStream(fop);

                out.print("package ");
                if (packageName != null && packageName.length() > 0) {
                    out.print(packageName + ".");
                }
                out.print("action");
                out.println(";");
                out.println();
                out.println("import org.openconcerto.sql.Configuration;");
                out.println("import org.openconcerto.sql.view.EditFrame;");

                out.println("import javax.swing.Action;");
                out.println("import javax.swing.JFrame;");

                out.println("public class " + className + " extends CreateFrameAbstractAction {");
                out.println("    public " + className + "() {");
                out.println("        super();");
                out.println("        this.putValue(Action.NAME, \"Nouveau " + name + "\");");
                out.println("    }");

                out.println("    public JFrame createFrame() {");
                out.println("        return new EditFrame(Configuration.getInstance().getDirectory().getElement(\"" + tableName + "\"));");
                out.println("    }");
                out.println("}");
                out.close();
                fop.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Listes
        for (String element : sqlElements) {
            String className = element.replaceAll("SQLElement", "ListAction");

            String tableName = tablesDir.get(element).getName();
            String name = tableName.toLowerCase();
            try {
                fop = new FileOutputStream(new File(srcDir, "action/list/" + className + ".java"));
                final PrintStream out = new PrintStream(fop);

                out.print("package ");
                if (packageName != null && packageName.length() > 0) {
                    out.print(packageName + ".");
                }
                out.print("action.list");
                out.println(";");
                out.println();
                out.println("import " + packageName + ".action.CreateFrameAbstractAction;");

                out.println("import org.openconcerto.sql.Configuration;");
                out.println("import org.openconcerto.sql.view.IListFrame;");
                out.println("import org.openconcerto.sql.view.ListeAddPanel;");

                out.println("import javax.swing.Action;");
                out.println("import javax.swing.JFrame;");

                out.println("public class " + className + " extends CreateFrameAbstractAction {");

                out.println("    public " + className + "() {");
                out.println("        super();");
                out.println("        this.putValue(Action.NAME, \"Liste " + name + "\");");
                out.println("    }");

                out.println("    public JFrame createFrame() {");

                out.println("        return new IListFrame(new ListeAddPanel(Configuration.getInstance().getDirectory().getElement(\"" + tableName + "\")));");
                out.println("}");

                out.println("}");
                out.close();
                fop.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    private void createConfiguration(DBRoot root) {
        FileOutputStream fop;
        try {
            fop = new FileOutputStream(new File(srcDir, "config/AppPropsConfiguration.java"));
            final PrintStream out = new PrintStream(fop);

            out.print("package ");
            if (packageName != null && packageName.length() > 0) {
                out.print(packageName + ".");
            }
            out.print("config");
            out.println(";");
            out.println();
            for (String element : sqlElements) {
                out.println("import " + this.packageName + ".element." + element + ";");
            }
            out.println("import org.openconcerto.sql.PropsConfiguration;");
            out.println("import org.openconcerto.sql.ShowAs;");
            out.println("import org.openconcerto.sql.element.SQLElementDirectory;");
            out.println();
            out.println("import java.io.File;");
            out.println("import java.io.IOException;");
            out.println("import java.io.InputStream;");
            out.println("import java.util.Properties;");
            out.println();
            out.println("import javax.swing.JFrame;");
            out.println("import javax.swing.JOptionPane;");
            out.println();

            out.println("public class AppPropsConfiguration extends PropsConfiguration {");
            out.println();

            out.println("    public static AppPropsConfiguration create() {");

            out.println("        final File wd = new File(System.getProperty(\"user.dir\"));");
            out.println("        final Properties defaults = new Properties();");
            out.println("        String parent = wd.getParent();");
            out.println("        if (parent == null) {");
            out.println("            parent = \".\";");
            out.println("        }");
            out.println("        defaults.setProperty(\"wd\", parent);");
            out.println("        defaults.setProperty(\"base.root\", \"" + root.getName() + "\");");
            out.println("        defaults.setProperty(\"app.name\", \"" + this.name + "\");");
            out.println("        File mainPropertiesFile = new File(wd + \"/Configuration\", \"main.properties\");");
            out.println("        try {");
            out.println("            return new AppPropsConfiguration(mainPropertiesFile, defaults);");
            out.println("        } catch (IOException e) {");
            out.println("            final String error = \"Impossible de lire le fichier de configuration: \" + mainPropertiesFile.getAbsolutePath();");
            out.println("            JOptionPane.showMessageDialog(new JFrame(), error);");
            out.println("            return null;");
            out.println("        }");
            out.println("    }");

            out.println("    private AppPropsConfiguration(File f, Properties defaults) throws IOException {");
            out.println("        super(f, defaults);");
            out.println("    }");

            out.println("    public AppPropsConfiguration(InputStream f, Properties defaults) throws IOException {");
            out.println("        super(f, defaults);");
            out.println("    }");

            out.println("    @Override");
            out.println("    protected String getLogin() {");
            out.println("        return \"maillard\";");
            out.println("    }");

            out.println("    @Override");
            out.println("    protected String getPassword() {");
            out.println("        return \"guigui\";");
            out.println("    }");

            out.println("    protected File getMappingFile() {");
            out.println("        return new File(\"mapping.xml\");");
            out.println("    }");

            out.println("    @Override");
            out.println("    protected ShowAs createShowAs() {");
            out.println("        final ShowAs showAs = super.createShowAs();");
            out.println("        return showAs;");
            out.println("    }");

            out.println("    @Override");
            out.println("    protected SQLElementDirectory createDirectory() {");
            out.println("        final SQLElementDirectory dir = super.createDirectory();");
            for (String element : sqlElements) {
                out.println("        dir.addSQLElement(new " + element + "());");
            }

            out.println("        return dir;");
            out.println("    }");

            out.println("}");
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Properties props = new Properties();
        props.put("server.wan.addr", "91.121.182.197");
        props.put("server.wan.only", "false");
        props.put("server.wan.port", "9999");
        props.put("server.ip", "192.168.1.16");
        props.put("log.level.org.openconcerto.sql", "CONFIG");
        props.put("app.name", this.name);
        props.put("server.driver", "postgresql");
        props.put("systemRoot", root.getBase().getName());

        new File("Configuration").mkdir();
        try {
            props.store(new FileOutputStream("Configuration/main.properties"), "Generated configuration");
        } catch (IOException e) {
            e.printStackTrace();
        }

        BufferedImage buf = new BufferedImage(400, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) buf.getGraphics();

        g.setBackground(Color.WHITE);
        g.clearRect(0, 0, 400, 64);
        g.setColor(new Color(11, 91, 171));
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(g.getFont().deriveFont(48f));
        g.drawString(this.name, 10, 50);
        g.dispose();
        try {
            ImageIO.write(buf, "png", new File(this.srcDir, "config/logo.png"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void createAutoLayoutedJComponent(DBRoot root) {
        Set<SQLTable> tables = root.getGraph().getAllTables();
        File dir = new File(projectDir, "src/" + packageName.replace('.', '/') + "/element");
        dir.mkdirs();
        try {
            for (SQLTable table : tables) {
                String className = ClassGenerator.getStandardClassName(table.getName()) + "SQLElement";
                sqlElements.add(className);
                tablesDir.put(className, table);
                FileOutputStream fop;

                fop = new FileOutputStream(new File(dir, className + ".java"));

                PrintStream out = new PrintStream(new BufferedOutputStream(fop));
                List<SQLField> f = table.getOrderedFields();

                ClassGenerator.generateAutoLayoutedJComponent(table, f, className, out, packageName + ".element");
                out.close();
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void createRowBacked(DBRoot root) {
        Set<SQLTable> tables = root.getGraph().getAllTables();
        File dir = new File(srcDir, "model");
        dir.mkdirs();
        try {
            for (SQLTable table : tables) {
                String className = ClassGenerator.getStandardClassName(table.getName());
                String s = RowBackedCodeGenerator.getCode(table, className, packageName + ".model");
                FileUtils.write(s, new File(dir, className + ".java"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createMapping(DBRoot root) {
        final Set<SQLTable> tables = root.getGraph().getAllTables();
        try {
            final FileOutputStream fop = new FileOutputStream(new File(srcDir, "config/mapping.xml"));
            final PrintStream out = new PrintStream(fop);
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
            out.println("<ROOT>");
            for (SQLTable table : tables) {
                List<SQLField> f = table.getOrderedFields();
                f.remove(table.getArchiveField());
                f.remove(table.getOrderField());
                f.remove(table.getKey());

                ClassGenerator.generateMappingXML(table, f, out);

            }
            out.println("</ROOT>");

            out.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}
