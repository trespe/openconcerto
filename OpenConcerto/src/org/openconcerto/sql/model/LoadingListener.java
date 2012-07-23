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
 
 package org.openconcerto.sql.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.jcip.annotations.ThreadSafe;

/**
 * The listener interface for receiving loading events.
 */
public interface LoadingListener {

    static public abstract class LoadingEvent {
        private final DBStructureItem<?> source;
        private final boolean starting;

        public LoadingEvent(final DBStructureItem<?> source) {
            this(source, true);
        }

        protected LoadingEvent(final DBStructureItem<?> source, boolean starting) {
            super();
            this.source = source;
            this.starting = starting;
        }

        public final DBStructureItem<?> getSource() {
            return this.source;
        }

        public final boolean isStarting() {
            return this.starting;
        }

        public abstract LoadingEvent createFinishingEvent();
    }

    static public class GraphLoadingEvent extends LoadingEvent {

        public GraphLoadingEvent(DBSystemRoot source) {
            super(source);
        }

        protected GraphLoadingEvent(DBSystemRoot source, boolean starting) {
            super(source, starting);
        }

        public final DBSystemRoot getSystemRoot() {
            return (DBSystemRoot) this.getSource();
        }

        // this method allows to keep fireLoading() package-private 
        public final GraphLoadingEvent fireEvent() {
            getSystemRoot().fireLoading(this);
            return this;
        }

        @Override
        public GraphLoadingEvent createFinishingEvent() {
            return new GraphLoadingEvent(getSystemRoot(), false);
        }

        public final void fireFinishingEvent() {
            getSystemRoot().fireLoading(this.createFinishingEvent());
        }

        @Override
        public String toString() {
            return this.getClass().getSimpleName() + (this.isStarting() ? " starting" : " finishing") + " loading of " + this.getSource();
        }

    }

    static public class StructureLoadingEvent extends LoadingEvent {
        private final Set<String> children;

        public StructureLoadingEvent(final DBStructureItemJDBC source, Set<String> children) {
            this(source, true, children);
        }

        protected StructureLoadingEvent(final DBStructureItemJDBC source, boolean starting, Set<String> children) {
            super(source, starting);
            this.children = children;
        }

        public final Set<String> getChildren() {
            return this.children;
        }

        @Override
        public final StructureLoadingEvent createFinishingEvent() {
            if (!this.isStarting())
                throw new IllegalStateException("Already a finishing event");
            return new StructureLoadingEvent((DBStructureItemJDBC) this.getSource(), false, this.children);
        }

        @Override
        public String toString() {
            final String children = this.getChildren() == null ? "all children" : this.getChildren().toString();
            return this.getClass().getSimpleName() + (this.isStarting() ? " starting" : " finishing") + " loading " + children + " of " + this.getSource();
        }
    }

    @ThreadSafe
    static public class LoadingChangeSupport {
        private final DBSystemRoot source;
        private final List<LoadingListener> loadingListeners;

        public LoadingChangeSupport(DBSystemRoot source) {
            this.source = source;
            this.loadingListeners = new ArrayList<LoadingListener>(4);
        }

        public synchronized final void addLoadingListener(LoadingListener l) {
            this.loadingListeners.add(l);
        }

        public synchronized final void removeLoadingListener(LoadingListener l) {
            this.loadingListeners.remove(l);
        }

        public synchronized void fireLoading(LoadingEvent evt) {
            assert evt.getSource().getDBSystemRoot() == this.source;
            for (final LoadingListener l : this.loadingListeners) {
                l.loading(evt);
            }
        }
    }

    void loading(LoadingEvent evt);
}
