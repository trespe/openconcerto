package org.jopenchart;

public class AxisLabel extends Label {

    private int position = Integer.MIN_VALUE; // Percentage
    private Number value;

    public AxisLabel(String label) {
        super(label);
    }

    /*
     * public AxisLabel(String label, int position) { super(label); this.position = position; }
     */
    public AxisLabel(String label, Number value) {
        super(label);
        this.value = value;
    }

    public final int getPosition() {
        return position;
    }

    public final void setPosition(int position) {
        this.position = position;
    }

    public final void removePosition() {
        this.position = Integer.MIN_VALUE;
    }

    public Number getValue() {
        return value;
    }

}
