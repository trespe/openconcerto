package org.openconcerto.modules.finance.payment.ebics;

public class OrderId {
    private static final char[] LETTERS = { 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };
    private static final char[] NUMBERS_LETTERS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S',
            'T', 'U', 'V', 'W', 'X', 'Y', 'Z' };
    // OrderIDtype = [A-Z]{1}[A-Z0-9]{3}
    // do not create new OrderType more than 4 time per hour in order to not reach the limit

    private final String code;

    public OrderId(String code) {
        this.code = code;
        if (code.length() != 4) {
            throw new IllegalArgumentException("OrderId must contains 4 characters");
        }
        if (!isLetter(code.charAt(0)) || !isLetterOrNumber(code.charAt(1)) || !isLetterOrNumber(code.charAt(2)) || !isLetterOrNumber(code.charAt(3))) {
            throw new IllegalArgumentException("OrderId must match [A-Z]{1}[A-Z0-9]{3}");
        }

    }

    public String getCode() {
        return code;
    }

    public OrderId getNext() {
        int index0 = indexOf(code.charAt(0), LETTERS);
        int index1 = indexOf(code.charAt(1), NUMBERS_LETTERS);
        int index2 = indexOf(code.charAt(2), NUMBERS_LETTERS);
        int index3 = indexOf(code.charAt(3), NUMBERS_LETTERS);

        index3++;
        if (index3 >= NUMBERS_LETTERS.length) {
            index3 = 0;
            index2++;
            if (index2 >= NUMBERS_LETTERS.length) {
                index2 = 0;
                index1++;
                if (index1 >= NUMBERS_LETTERS.length) {
                    index1 = 0;
                    index0++;
                    if (index0 >= LETTERS.length) {
                        throw new IllegalArgumentException("No next OrderId for " + this.code);
                    }
                }

            }
        }
        final StringBuilder b = new StringBuilder(4);
        b.append(LETTERS[index0]);
        b.append(NUMBERS_LETTERS[index1]);
        b.append(NUMBERS_LETTERS[index2]);
        b.append(NUMBERS_LETTERS[index3]);
        return new OrderId(b.toString());

    }

    private final int indexOf(final char c, final char[] array) {
        for (int i = 0; i < array.length; i++) {
            if (array[i] == c)
                return i;
        }
        throw new IllegalStateException("Unable to find index of " + c);
    }

    private final boolean isLetter(final char c) {
        for (int i = 0; i < LETTERS.length; i++) {
            if (LETTERS[i] == c)
                return true;
        }
        return false;
    }

    private final boolean isLetterOrNumber(char c) {
        for (int i = 0; i < NUMBERS_LETTERS.length; i++) {
            if (NUMBERS_LETTERS[i] == c)
                return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "OrderId " + this.code;
    }

}
