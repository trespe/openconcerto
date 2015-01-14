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
 
 package org.openconcerto.erp.core.sales.product.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

public class PriceByQty {

    private long qty;
    private BigDecimal price;
    private Date startDate;

    public PriceByQty(long qty, BigDecimal price, Date startDate) {
        this.qty = qty;
        this.price = price;
        this.startDate = startDate;
    }

    public static BigDecimal getPriceForQty(int qty, List<PriceByQty> list) {
        BigDecimal result = null;
        Collections.sort(list, new Comparator<PriceByQty>() {

            @Override
            public int compare(PriceByQty o1, PriceByQty o2) {

                final int i = (int) (o1.qty - o2.qty);
                if (i != 0) {
                    return i;
                }
                return o1.startDate.compareTo(o2.startDate);
            }
        });
        Date now = new Date(System.currentTimeMillis());
        for (PriceByQty priceByQty : list) {
            if (priceByQty.qty > qty) {
                break;
            }
            if (priceByQty.startDate.before(now)) {
                result = priceByQty.price;
            }
        }
        return result;
    }

    public static void main(String[] args) {
        List<PriceByQty> l = new ArrayList<PriceByQty>();
        final long currentTimeMillis = System.currentTimeMillis();
        l.add(new PriceByQty(50, new BigDecimal(11), new Date(currentTimeMillis)));
        l.add(new PriceByQty(1, new BigDecimal(14), new Date(currentTimeMillis + 2000)));
        l.add(new PriceByQty(1, new BigDecimal(13), new Date(currentTimeMillis - 5000)));
        l.add(new PriceByQty(1, new BigDecimal(12), new Date(currentTimeMillis)));

        System.out.println(getPriceForQty(0, l));
        System.out.println(getPriceForQty(1, l));
        System.out.println(getPriceForQty(49, l));
        System.out.println(getPriceForQty(50, l));
    }
}
