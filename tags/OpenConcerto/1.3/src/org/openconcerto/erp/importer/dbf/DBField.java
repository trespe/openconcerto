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
 
 package org.openconcerto.erp.importer.dbf;

import java.text.*;
import java.util.Date;

public class DBField {
    private String name;
    private char type;
    private int length;
    private int decimalCount;

    public DBField(String name, char type, int length, int decimalCount) {
        if (name.length() > 10) {
            throw new IllegalArgumentException("The field name is more than 10 characters long: " + name);
        }
        if (type != 'C' && type != 'N' && type != 'L' && type != 'D' && type != 'F') {
            throw new IllegalArgumentException("The field type is not a valid. Got: " + type);
        }
        if (length < 1) {
            throw new IllegalArgumentException("The field length should be a positive integer. Got: " + length);
        }
        if (type == 'C' && length >= 255) {
            throw new IllegalArgumentException("The field length should be less than 255 characters for character fields. Got: " + length);
        }
        if (type == 'N' && length >= 21) {
            throw new IllegalArgumentException("The field length should be less than 21 digits for numeric fields. Got: " + length);
        }
        if (type == 'L' && length != 1) {
            throw new IllegalArgumentException("The field length should be 1 characater for logical fields. Got: " + length);
        }
        if (type == 'D' && length != 8) {
            throw new IllegalArgumentException("The field length should be 8 characaters for date fields. Got: " + length);
        }
        if (type == 'F' && length >= 21) {
            throw new IllegalArgumentException("The field length should be less than 21 digits for floating point fields. Got: " + length);
        }
        if (decimalCount < 0) {
            throw new IllegalArgumentException("The field decimal count should not be a negative integer. Got: " + decimalCount);
        }
        if ((type == 'C' || type == 'L' || type == 'D') && decimalCount != 0) {
            throw new IllegalArgumentException("The field decimal count should be 0 for character, logical, and date fields. Got: " + decimalCount);
        }
        if (decimalCount > length - 1) {
            throw new IllegalArgumentException("The field decimal count should be less than the length - 1. Got: " + decimalCount);
        }
        this.name = name;
        this.type = type;
        this.length = length;
        this.decimalCount = decimalCount;

    }

    public String getName() {
        return name;
    }

    public char getType() {
        return type;
    }

    public int getLength() {
        return length;
    }

    public int getDecimalCount() {
        return decimalCount;
    }

    public String format(Object obj) {
        if (type == 'N' || type == 'F') {
            if (obj == null) {
                obj = new Double(0.0D);
            }
            if (obj instanceof Number) {
                Number number = (Number) obj;
                StringBuffer stringbuffer = new StringBuffer(getLength());
                for (int i = 0; i < getLength(); i++) {
                    stringbuffer.append("#");

                }
                if (getDecimalCount() > 0) {
                    stringbuffer.setCharAt(getLength() - getDecimalCount() - 1, '.');
                }
                DecimalFormat decimalformat = new DecimalFormat(stringbuffer.toString());
                String s1 = decimalformat.format(number);
                int k = getLength() - s1.length();
                if (k < 0) {
                    throw new IllegalArgumentException("Value " + number + " cannot fit in pattern: '" + stringbuffer + "'.");
                }
                StringBuffer stringbuffer2 = new StringBuffer(k);
                for (int l = 0; l < k; l++) {
                    stringbuffer2.append(" ");

                }
                return stringbuffer2 + s1;
            } else {
                throw new IllegalArgumentException("Expected a Number, got " + obj.getClass() + ".");
            }
        }
        if (type == 'C') {
            if (obj == null) {
                obj = "";
            }
            if (obj instanceof String) {
                String s = (String) obj;
                if (s.length() > getLength()) {
                    throw new IllegalArgumentException("'" + obj + "' is longer than " + getLength() + " characters.");
                }
                StringBuffer stringbuffer1 = new StringBuffer(getLength() - s.length());
                for (int j = 0; j < getLength() - s.length(); j++) {
                    stringbuffer1.append(' ');

                }
                return s + stringbuffer1;
            } else {
                throw new IllegalArgumentException("Expected a String, got " + obj.getClass() + ".");
            }
        }
        if (type == 'L') {
            if (obj == null) {
                obj = new Boolean(false);
            }
            if (obj instanceof Boolean) {
                Boolean boolean1 = (Boolean) obj;
                return boolean1.booleanValue() ? "Y" : "N";
            } else {
                throw new IllegalArgumentException("Expected a Boolean, got " + obj.getClass() + ".");
            }
        } else if (type == 'D') {
            if (obj == null) {
                obj = new Date();
            }
            if (obj instanceof Date) {
                Date date = (Date) obj;
                SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyyMMdd");
                return simpledateformat.format(date);
            } else {
                throw new IllegalArgumentException("Expected a Date, got " + obj.getClass() + ".");
            }
        } else {
            throw new IllegalArgumentException("Unrecognized JDBFField type: " + type);
        }
    }

    public Object parse(String s) {
        s = s.trim();
        if (type == 'N' || type == 'F') {
            if (s.equals("")) {
                s = "0";
            }

            if (getDecimalCount() == 0) {
                return new Long(s);
            } else {
                return new Double(s);
            }

        }
        if (type == 'C') {
            return s;
        }
        if (type == 'L') {
            if (s.equals("Y") || s.equals("y") || s.equals("T") || s.equals("t")) {
                return new Boolean(true);
            }
            if (s.equals("N") || s.equals("n") || s.equals("F") || s.equals("f")) {
                return new Boolean(false);
            } else {
                throw new IllegalArgumentException("Unrecognized value for logical field: " + s);
            }
        }
        if (type == 'D') {
            SimpleDateFormat simpledateformat = new SimpleDateFormat("yyyyMMdd");
            try {
                if ("".equals(s)) {
                    return null;
                } else {
                    return simpledateformat.parse(s);
                }
            } catch (ParseException parseexception) {
                throw new IllegalArgumentException(parseexception);
            }
        } else {
            throw new IllegalArgumentException("Unrecognized JDBFField type: " + type);
        }
    }

    public String toString() {
        return name;
    }

}
