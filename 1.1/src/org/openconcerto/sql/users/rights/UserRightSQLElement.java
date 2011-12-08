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
 
 package org.openconcerto.sql.users.rights;

import static java.util.Arrays.asList;
import org.openconcerto.sql.element.ConfSQLElement;
import org.openconcerto.sql.element.SQLComponent;
import org.openconcerto.sql.element.SQLElement;
import org.openconcerto.sql.element.UISQLComponent;
import org.openconcerto.sql.model.SQLRow;
import org.openconcerto.sql.model.SQLRowValues;
import org.openconcerto.sql.sqlobject.ElementComboBox;
import org.openconcerto.sql.sqlobject.SQLRequestComboBox;
import org.openconcerto.utils.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class UserRightSQLElement extends ConfSQLElement {

    public UserRightSQLElement() {
        super("USER_RIGHT", "un droit utilisateur", "droits utilisateurs");
    }

    protected List<String> getListFields() {
        // don't display USER to avoid undefined
        return asList("ID_RIGHT", "OBJECT", "HAVE_RIGHT");
    }

    protected List<String> getComboFields() {
        return getListFields();
    }

    @Override
    protected String getParentFFName() {
        return "ID_USER_COMMON";
    }

    @Override
    // r/o since we set it programmatically to prevent the edition of superuser rights
    public Set<String> getReadOnlyFields() {
        return Collections.singleton("ID_USER_COMMON");
    }

    public SQLComponent createComponent() {
        return new UserRightComp(this);
    }

    public static final class UserRightComp extends UISQLComponent {

        private int userID = SQLRow.NONEXISTANT_ID;

        private UserRightComp(SQLElement element) {
            super(element, 2, 1);
        }

        @Override
        protected Set<String> createRequiredNames() {
            return CollectionUtils.createSet("ID_RIGHT", "HAVE_RIGHT");
        }

        public void addViews() {
            final SQLRequestComboBox user = new SQLRequestComboBox();
            this.addView(user, "ID_USER_COMMON", "0");
            user.getRequest().setUndefLabel("Par défaut");
            final ElementComboBox right = new ElementComboBox();
            right.setListIconVisible(false);
            this.addView(right, "ID_RIGHT");
            this.addView("HAVE_RIGHT");
            this.addView("OBJECT", "0");
        }

        @Override
        protected SQLRowValues createDefaults() {
            if (this.userID >= SQLRow.MIN_VALID_ID)
                return new SQLRowValues(getTable()).put("ID_USER_COMMON", this.userID);
            else
                return null;
        }

        public final void setUserID(int userID) {
            this.userID = userID;
        }
    }
}
