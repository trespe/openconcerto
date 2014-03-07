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
 
 package org.openconcerto.erp.modules;

import org.openconcerto.erp.config.Log;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.logging.Level;

import javax.swing.table.AbstractTableModel;

import net.jcip.annotations.Immutable;

public class ModuleTableModel extends AbstractTableModel {

    static public enum Problem {
        /**
         * The module is required but missing.
         */
        REQUIRED_MISSING,
        /**
         * The module is installed but lacks at least one dependency.
         */
        MISSING_DEP
    }

    // ref needed if factory is null
    static private final boolean areDepsMet(final ModuleFactory factory, final InstallationState installationState) {
        for (final Dependency dep : factory.getDependencies().values()) {
            boolean depMet = false;
            for (final String reqID : dep.getRequiredIDs()) {
                final ModuleFactory f = installationState.getInstalledFactories().get(reqID);
                if (f != null && dep.isRequiredFactoryOK(f)) {
                    depMet = true;
                    break;
                }
            }
            if (!depMet)
                return false;
        }
        return true;
    }

    @Immutable
    static final class ModuleRow {
        private final ModuleReference ref;
        private final ModuleFactory factory;
        private final boolean local, remote, registered, running;
        private final boolean dbRequired, adminRequired;
        private final Set<Problem> problems;

        public ModuleRow(ModuleReference ref, ModuleFactory f, InstallationState installationState, boolean registered, boolean running, boolean dbRequired, boolean adminRequired) {
            super();
            if (ref == null)
                throw new NullPointerException("Null reference");
            this.ref = ref;
            this.factory = f;
            assert this.factory == null || this.factory.getReference().equals(this.ref);
            this.local = installationState.getLocal().contains(ref);
            this.remote = installationState.getRemote().contains(ref);
            this.registered = registered;
            this.running = running;
            this.dbRequired = dbRequired;
            this.adminRequired = adminRequired;
            final Set<Problem> pbs = new HashSet<Problem>(Problem.values().length);
            final boolean isInstalled = this.isInstalledLocally() || this.isInstalledRemotely();
            if (!this.isAvailable() && isInstalled) {
                pbs.add(Problem.REQUIRED_MISSING);
            }
            // if installed and not available, we cannot know its dependencies but it will be marked
            // REQUIRED_MISSING
            if (this.isAvailable() && isInstalled && !areDepsMet(this.factory, installationState)) {
                pbs.add(Problem.MISSING_DEP);
            }
            this.problems = pbs.size() == 0 ? Collections.<Problem> emptySet() : Collections.unmodifiableSet(pbs);
        }

        public ModuleReference getRef() {
            return this.ref;
        }

        public final String getName() {
            return this.factory != null ? this.factory.getName() : this.getRef().getID();
        }

        public boolean isAvailable() {
            return this.factory != null;
        }

        public boolean isInstalledLocally() {
            return this.local;
        }

        public boolean isInstalledRemotely() {
            return this.remote;
        }

        public boolean isRegistered() {
            return this.registered;
        }

        public boolean isRunning() {
            return this.running;
        }

        public boolean isDBRequired() {
            return this.dbRequired;
        }

        public boolean isAdminRequired() {
            return this.adminRequired;
        }

        public Set<Problem> getProblems() {
            return this.problems;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + this.ref.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            final ModuleRow other = (ModuleRow) obj;
            return this.ref.equals(other.ref);
        }
    }

    // TODO add DESC and ERROR
    static enum Columns {
        CB, NAME, VERSION, STATE, LOCAL, REMOTE, DB_REQUIRED, ADMIN_REQUIRED
    }

    static private final EnumSet<Columns> BOOLEAN_COLS = EnumSet.of(Columns.CB, Columns.LOCAL, Columns.REMOTE, Columns.DB_REQUIRED, Columns.ADMIN_REQUIRED);

    private List<ModuleRow> list;
    private final Set<ModuleRow> selection;

    private boolean valid;

    public ModuleTableModel() {
        this.selection = new HashSet<ModuleRow>();
        this.list = Collections.emptyList();
        this.valid = false;
    }

    final void clear() {
        this.selection.clear();
        this.list = Collections.emptyList();
        this.fireTableDataChanged();
    }

    public final void reload() throws IOException, SQLException {
        final ModuleManager mngr = ModuleManager.getInstance();
        final InstallationState installationState = new InstallationState(mngr);
        final Map<ModuleReference, ModuleFactory> available = new HashMap<ModuleReference, ModuleFactory>();
        for (final Entry<String, SortedMap<ModuleVersion, ModuleFactory>> e : mngr.getFactories().entrySet()) {
            for (final Entry<ModuleVersion, ModuleFactory> e2 : e.getValue().entrySet()) {
                available.put(new ModuleReference(e.getKey(), e2.getKey()), e2.getValue());
            }
        }
        // we're reloading so sync preferences
        final List<ModuleReference> adminRequired = mngr.getAdminRequiredModules(true);
        final List<ModuleReference> dbRequired = mngr.getDBRequiredModules();
        final Set<ModuleReference> running = new HashSet<ModuleReference>();
        for (final Entry<String, AbstractModule> e : mngr.getRunningModules().entrySet()) {
            running.add(new ModuleReference(e.getKey(), e.getValue().getFactory().getVersion()));
        }
        final Collection<ModuleReference> registered = mngr.getRegisteredModules();

        // if some installed modules lack their factory, we cannot know if they conflict with
        // modules to install (see DepSolverResultMM.computeReferencesToRemove())
        this.valid = true;

        // all known references
        final Set<ModuleReference> s = new HashSet<ModuleReference>(installationState.getLocalOrRemote());
        s.addAll(available.keySet());
        final List<ModuleRow> l = new ArrayList<ModuleRow>(s.size());
        for (final ModuleReference ref : s) {
            final ModuleRow row = new ModuleRow(ref, available.get(ref), installationState, registered.contains(ref), running.contains(ref), dbRequired.contains(ref), adminRequired.contains(ref));
            l.add(row);
            this.valid &= row.getProblems().size() == 0;
        }

        // sort alphabetically and then highest version first
        Collections.sort(l, new Comparator<ModuleRow>() {
            @Override
            public int compare(ModuleRow r1, ModuleRow r2) {
                final ModuleReference o1 = r1.getRef();
                final ModuleReference o2 = r2.getRef();
                return ModuleReference.COMP_ID_ASC_VERSION_DESC.compare(o1, o2);
            }
        });
        this.list = Collections.unmodifiableList(l);
        this.selection.retainAll(this.list);
        this.fireTableDataChanged();
    }

    public final boolean isValid() {
        return this.valid;
    }

    public final Collection<ModuleRow> getCheckedRows() {
        return Collections.unmodifiableSet(this.selection);
    }

    @Override
    public int getColumnCount() {
        return Columns.values().length;
    }

    @Override
    public final int getRowCount() {
        return this.list.size();
    }

    protected final ModuleRow getFactory(int i) {
        return this.list.get(i);
    }

    protected final ModuleRow getRow(final ModuleReference ref) {
        if (ref.getVersion() == null)
            throw new IllegalStateException("Null version");
        for (final ModuleRow r : this.list) {
            if (r.getRef().equals(ref))
                return r;
        }
        return null;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return columnIndex == Columns.CB.ordinal();
    }

    @Override
    public String getColumnName(int column) {
        final Columns col = Columns.values()[column];
        switch (col) {
        case NAME:
            return "Nom";
        case VERSION:
            return "Version";
        case STATE:
            return "Etat";
        case LOCAL:
            return "Installé sur le poste";
        case REMOTE:
            return "Installé sur le serveur";
        case DB_REQUIRED:
            return "Requis par le système";
        case ADMIN_REQUIRED:
            return "Requis par l'administrateur";
        default:
            return "";
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        final ModuleRow f = this.getFactory(rowIndex);
        final Columns col = Columns.values()[columnIndex];
        try {
            switch (col) {
            case CB:
                return this.selection.contains(f);
            case NAME:
                return f.getName();
            case VERSION:
                return f.getRef().getVersion();
            case STATE:
                final String s;
                if (f.isRunning())
                    s = "Démarré";
                else if (f.isRegistered())
                    s = "Chargé";
                else if (f.isAvailable())
                    s = "Disponible";
                else
                    s = "Non disponible";
                final Set<Problem> pbs = f.getProblems();
                if (pbs.size() == 0)
                    return s;
                final StringBuilder sb = new StringBuilder(64);
                sb.append("<html><body>");
                sb.append(s);
                // now add error tags
                // space at the end for background
                sb.append(" <font bgcolor=\"RED\" color=\"WHITE\">&nbsp;");
                if (pbs.contains(Problem.REQUIRED_MISSING))
                    sb.append("Module&nbsp;requis ");
                if (pbs.contains(Problem.MISSING_DEP))
                    sb.append("Dépendances&nbsp;manquantes ");
                // leave last space for background
                sb.append("</font></body></html>");
                return sb.toString();
            case LOCAL:
                return f.isInstalledLocally();
            case REMOTE:
                return f.isInstalledRemotely();
            case DB_REQUIRED:
                return f.isDBRequired();
            case ADMIN_REQUIRED:
                return f.isAdminRequired();
            default:
                return "";
            }
        } catch (Exception e) {
            Log.get().log(Level.SEVERE, "row:" + rowIndex + " column:" + columnIndex, e);
            return e.getMessage();
        }
    }

    @Override
    public void setValueAt(Object value, int rowIndex, int columnIndex) {
        if (columnIndex == Columns.CB.ordinal()) {
            if ((Boolean) value)
                this.selection.add(this.getFactory(rowIndex));
            else
                this.selection.remove(this.getFactory(rowIndex));
        }
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (BOOLEAN_COLS.contains(Columns.values()[columnIndex])) {
            return Boolean.class;
        } else {
            return String.class;
        }
    }
}
