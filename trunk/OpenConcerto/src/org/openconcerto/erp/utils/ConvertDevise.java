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
 
 package org.openconcerto.erp.utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class ConvertDevise {

	/**
	 * Convertit un prix ht en ttc
	 * 
	 * @param ht
	 *            la valeur hors taxe à convertir
	 * @param taxe
	 *            taux de la tva à appliquer (ex : 7, 20)
	 * @param scale
	 *            précision à appliquer sur le ttc retourné
	 * @return le ttc avec la précision scale
	 */
	public static final BigDecimal getTtcFromHt(BigDecimal ht, BigDecimal taxe,
			int scale) {

		BigDecimal tauxB = taxe.movePointLeft(2).add(BigDecimal.ONE);
		BigDecimal result = ht.multiply(tauxB, MathContext.DECIMAL128)
				.setScale(scale, RoundingMode.HALF_UP);
		return result;

	}

	/**
	 * Convertit un prix ttc en ht
	 * 
	 * @param tts
	 *            la valeur tts à convertir
	 * @param taxe
	 *            taux de la tva à appliquer (ex : 7, 20)
	 * @param scale
	 *            précision à appliquer sur le ht retourné
	 * @return le ht avec la précision scale
	 */
	public static final BigDecimal getHtFromTtc(BigDecimal ttc,
			BigDecimal taxe, int scale) {
		if (taxe.signum() == 0) {
			return ttc.setScale(scale, RoundingMode.HALF_UP);
		}

		BigDecimal tauxB = taxe.movePointLeft(2).add(BigDecimal.ONE);
		BigDecimal result = ttc.divide(tauxB, MathContext.DECIMAL128).setScale(
				scale, RoundingMode.HALF_UP);
		return result;

	}

	public static void main(String[] args) {

		BigDecimal ttcFromHt = getTtcFromHt(BigDecimal.ONE, new BigDecimal(20),
				6);
		System.err.println(ttcFromHt);
		System.err.println(getHtFromTtc(ttcFromHt, new BigDecimal(20), 6));

	}
}
