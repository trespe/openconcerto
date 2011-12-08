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
 
 /*
 * Created on 21 mai 2005
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package org.openconcerto.laf;

import java.awt.Component;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.UIDefaults;
import javax.swing.plaf.metal.MetalLookAndFeel;

public class ILookAndFeel extends MetalLookAndFeel {

	/* (non-Javadoc)
	 * @see javax.swing.plaf.metal.MetalLookAndFeel#createDefaultTheme()
	 */
	protected void createDefaultTheme() {
		// TODO Auto-generated method stub
		super.createDefaultTheme();
	}

	/* (non-Javadoc)
	 * @see javax.swing.plaf.metal.MetalLookAndFeel#getDefaults()
	 */
	public UIDefaults getDefaults() {
		// TODO Auto-generated method stub
		return super.getDefaults();
	}

	/* (non-Javadoc)
	 * @see javax.swing.plaf.metal.MetalLookAndFeel#getDescription()
	 */
	public String getDescription() {
		
		return "It will make you feel Java power";
	}

	/* (non-Javadoc)
	 * @see javax.swing.plaf.metal.MetalLookAndFeel#getDisabledIcon(javax.swing.JComponent, javax.swing.Icon)
	 */
	/*public Icon getDisabledIcon(JComponent arg0, Icon arg1) {
		// TODO Auto-generated method stub
		return super.getDisabledIcon(arg0, arg1);
	}*/

	/* (non-Javadoc)
	 * @see javax.swing.plaf.metal.MetalLookAndFeel#getDisabledSelectedIcon(javax.swing.JComponent, javax.swing.Icon)
	 */
	/*public Icon getDisabledSelectedIcon(JComponent arg0, Icon arg1) {
		// TODO Auto-generated method stub
		return super.getDisabledSelectedIcon(arg0, arg1);
	}*/

	/* (non-Javadoc)
	 * @see javax.swing.plaf.metal.MetalLookAndFeel#getID()
	 */
	public String getID() {
		return "ILAF";
	}

	/* (non-Javadoc)
	 * @see javax.swing.plaf.metal.MetalLookAndFeel#getName()
	 */
	public String getName() {
		return "ILAF";
	}

	/* (non-Javadoc)
	 * @see javax.swing.plaf.metal.MetalLookAndFeel#getSupportsWindowDecorations()
	 */
	public boolean getSupportsWindowDecorations() {
		// TODO Auto-generated method stub
		return super.getSupportsWindowDecorations();
	}

	/* (non-Javadoc)
	 * @see javax.swing.plaf.metal.MetalLookAndFeel#initClassDefaults(javax.swing.UIDefaults)
	 */
	protected void initClassDefaults(UIDefaults table) {
		
		 super.initClassDefaults(table);
		    table.putDefaults(new Object[]
		    {
		      "ButtonUI", "org.openconcerto.laf.IButtonUI",
		      "ScrollBarUI", "org.openconcerto.laf.IScrollBarUI",
		      "ComboBoxUI", "org.openconcerto.laf.IComboBoxUI",
		      /*,
		      "CheckBoxUI", "com.stefankrause.xplookandfeel.XPCheckBoxUI",
		      "TextFieldUI", "com.stefankrause.xplookandfeel.XPTextFieldUI",
		      "FormattedTextFieldUI", "com.stefankrause.xplookandfeel.XPTextFieldUI",
		      "SliderUI", "com.stefankrause.xplookandfeel.XPSliderUI",
		      "SpinnerUI", "com.stefankrause.xplookandfeel.XPSpinnerUI",
		      "ToolBarUI", "com.stefankrause.xplookandfeel.XPToolBarUI",
		      "MenuBarUI", "com.stefankrause.xplookandfeel.XPMenuBarUI",
		      "MenuUI", "com.stefankrause.xplookandfeel.XPMenuUI",
		      "MenuItemUI", "com.stefankrause.xplookandfeel.XPMenuItemUI",
		 	  "CheckBoxMenuItemUI", "com.stefankrause.xplookandfeel.XPCheckBoxMenuItemUI",
		      "RadioButtonMenuItemUI", "com.stefankrause.xplookandfeel.XPRadioButtonMenuItemUI",

		      "TabbedPaneUI", "com.stefankrause.xplookandfeel.XPTabbedPaneUI",
		      "ToggleButtonUI", "com.stefankrause.xplookandfeel.XPButtonUI",
		      "ScrollPaneUI", "com.stefankrause.xplookandfeel.XPScrollPaneUI",
		      "ProgressBarUI", "com.stefankrause.xplookandfeel.XPProgressBarUI",
		      "InternalFrameUI", "com.stefankrause.xplookandfeel.XPInternalFrameUI",
		      "RadioButtonUI", "com.stefankrause.xplookandfeel.XPRadioButtonUI",
		       "PopupMenuSeparatorUI","com.stefankrause.xplookandfeel.XPPopupMenuSeparatorUI",
		      "SplitPaneUI","com.stefankrause.xplookandfeel.XPSplitPaneUI",
		      "FileChooserUI", "com.stefankrause.xplookandfeel.XPFileChooserUI",
		  */  });
	}

	/* (non-Javadoc)
	 * @see javax.swing.plaf.metal.MetalLookAndFeel#initComponentDefaults(javax.swing.UIDefaults)
	 */
	protected void initComponentDefaults(UIDefaults arg0) {
		// TODO Auto-generated method stub
		super.initComponentDefaults(arg0);
	}

	/* (non-Javadoc)
	 * @see javax.swing.plaf.metal.MetalLookAndFeel#initSystemColorDefaults(javax.swing.UIDefaults)
	 */
	protected void initSystemColorDefaults(UIDefaults arg0) {
		// TODO Auto-generated method stub
		super.initSystemColorDefaults(arg0);
	}

	/* (non-Javadoc)
	 * @see javax.swing.plaf.metal.MetalLookAndFeel#isNativeLookAndFeel()
	 */
	public boolean isNativeLookAndFeel() {
		return false;
	}

	/* (non-Javadoc)
	 * @see javax.swing.plaf.metal.MetalLookAndFeel#isSupportedLookAndFeel()
	 */
	public boolean isSupportedLookAndFeel() {
		return true;
	}

	/* (non-Javadoc)
	 * @see javax.swing.plaf.metal.MetalLookAndFeel#provideErrorFeedback(java.awt.Component)
	 */
	public void provideErrorFeedback(Component arg0) {
		// TODO Auto-generated method stub
		super.provideErrorFeedback(arg0);
	}

}
