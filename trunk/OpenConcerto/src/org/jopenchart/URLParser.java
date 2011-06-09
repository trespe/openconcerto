package org.jopenchart;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JFrame;

import org.jopenchart.barchart.VerticalStackBarChart;
import org.jopenchart.linechart.LineChart;
import org.jopenchart.marker.CircleMarker;
import org.jopenchart.marker.ShapeMarker;
import org.jopenchart.piechart.PieChart;
import org.jopenchart.sample.model.DataModel1DDynamic;
import org.jopenchart.sample.model.DataModel1DFuzzy;

public class URLParser {
    final Map<String, String> parameters = new HashMap<String, String>();

    int[] valueOf = new int[128];

    String simpleEncoding = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    public URLParser(String url) {
        for (int i = 0; i < simpleEncoding.length(); i++) {
            valueOf[simpleEncoding.charAt(i)] = i;
        }
        final int s = url.indexOf('?');
        String allparams = url.substring(s);
        String buff = "";
        String key = "";
        String value = "";
        final int length = allparams.length();
        for (int i = 1; i < length; i++) {
            char c = allparams.charAt(i);
            if (c == '&') {
                value = buff;
                buff = "";
                System.out.println(key + " " + value);
                parameters.put(key, value);

            } else if (c == '=') {
                key = buff;
                buff = "";
            } else {
                buff += c;
            }
        }
        value = buff;
        parameters.put(key, value);
        System.out.println(key + " " + value);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // testURL();
        // testDynamicPieChart();
        // testDynamicChart();
        testDynamicBarChart();
    }

    private static void testDynamicBarChart() {
        VerticalStackBarChart c = new VerticalStackBarChart();
        c.setColor(new Color(0, 155, 100));

        Axis axis = new Axis("y");
        axis.addLabel(new AxisLabel("0%"));
        axis.addLabel(new AxisLabel("100%"));
        // c.setLeftAxis(axis);
        c.setDimension(new Dimension(400, 300));
        c.addModel(new DataModel1DDynamic(c));
        c.setLowerRange(0);
        c.setHigherRange(100);

        // c.setDataModel(new DataModel1DDynamic());
        ChartPanel panel = new ChartPanel(c);
        JFrame f = new JFrame("Test");
        f.setContentPane(panel);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(600, 500);
        f.setVisible(true);

    }

    private static void testDynamicChart() {
        LineChart c = new LineChart();
        c.setColor(new Color(0, 155, 100));
        c.setFillColor(Color.LIGHT_GRAY);
        Axis axis = new Axis("y");
        axis.addLabel(new AxisLabel("0%"));
        axis.addLabel(new AxisLabel("100%"));
        c.setLeftAxis(axis);
        c.setDimension(new Dimension(400, 300));
        c.setDataModel(new DataModel1DFuzzy());
        c.setLowerRange(0);
        c.setHigherRange(100);

        // c.setDataModel(new DataModel1DDynamic());
        ChartPanel panel = new ChartPanel(c);
        JFrame f = new JFrame("Test");
        f.setContentPane(panel);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(600, 500);
        f.setVisible(true);

    }

    private static void testDynamicPieChart() {
        PieChart c = new PieChart();
        // c.setColor(new Color(0, 155, 100));
        c.setColor(new Color(0, 0, 0));
        c.setInnerDimension(20, 20);
        c.setInnerColor(Color.WHITE);
        Axis axis = new Axis("y");
        axis.addLabel(new AxisLabel("0%"));
        axis.addLabel(new AxisLabel("100%"));

        c.setDimension(new Dimension(200, 125));
        Number[] d = { 50, 20, 80, 30 };
        c.setDataModel(new DataModel1D(d));

        // c.setDataModel(new DataModel1DDynamic());
        ChartPanel panel = new ChartPanel(c);
        JFrame f = new JFrame("Test");
        f.setContentPane(panel);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(600, 500);
        f.setVisible(true);

    }

    private static void testURL() {
        String url = "http://chart.apis.google.com/chart?cht=lc&chs=400x200&chd=s:9fohmnytenefoh__m_ny_tene&chxt=x,y&chxl=0:|Apr|May|June|1:|0|50+Kb";
        url = "http://chart.apis.google.com/chart?cht=lc&chs=200x125&chd=s:helloWorld&chxt=x,y&chxl=0:|Mar|Apr|May|June|July|1:||50+Kb";
        url = "http://chart.apis.google.com/chart?cht=p&chd=s:world5&chs=200x125&chl=A|B|C|D|E|F";
        // "http://chart.apis.google.com/chart?cht=lc&chs=200x125&chd=s:fooZaroo";
        url = "http://chart.apis.google.com/chart?cht=bvs&chs=200x125&chd=s:hello,world&chco=cc0000,00aa00&chbh=20";
        url = "http://chart.apis.google.com/chart?cht=lc&chd=s:cEAELFJHHHKUju9uuXUc&chco=76A4FB&chls=2.0,0.0,0.0&chxt=x,y&chxl=0:|0|1|2|3|4|5|1:|0|50|100&chs=200x125&chg=20,50";
        // url =
        // "http://chart.apis.google.com/chart?cht=lc&chd=s:cEA&chls=2.0,0.0,0.0&chxt=x,y&chxl=0:|0|1|2|3|4|5|1:|0|50|100&chs=200x125&chg=20,50";
        url = "http://chart.apis.google.com/chart?cht=lc&chd=s:93zyvneTTOMJMLIJFHEAECFJGHDBFCFIERcgnpy45879,IJKNUWUWYdnswz047977315533zy1246872tnkgcaZQONHCECAAAAEII&chls=3,6,3|1,1,0&chs=200x125";
        url = "http://chart.apis.google.com/chart?cht=lc&chd=s:9gounjqGJD&chco=008000&chls=2.0,4.0,1.0&chs=200x125&chxt=x&chxl=0:||c|d|a|o|x|v|V|a|&chm=a,990066,0,3.0,9.0|c,FF0000,0,1.0,20.0|d,80C65A,0,2.0,20.0|o,FF9900,0,4.0,20.0|s,3399CC,0,5.0,10.0|v,BBCCED,0,6.0,1.0|V,3399CC,0,7.0,1.0|x,FFCC33,0,8.0,20.0|h,000000,0,0.30,0.5";

        URLParser p = new URLParser(url);
        Chart c = p.getChart();
        ChartPanel panel = new ChartPanel(c);
        JFrame f = new JFrame("Test");
        f.setContentPane(panel);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setSize(600, 500);
        f.setVisible(true);
    }

    public Chart getChart() {

        String type = parameters.get("cht");
        if (type == null) {
            return null;
        }
        if (type.equals("lc")) {
            LineChart chart = new LineChart();
            chart.setDimension(getDimension(parameters.get("chs")));
            setRange(chart, parameters.get("chd"));
            chart.setMultipleData(getMultipleData(chart, parameters.get("chd")));
            chart.setColors(getColors(parameters.get("chco")));
            if (parameters.get("chls") != null) {
                chart.setStrokes(getStrokes(parameters.get("chls")));
            }
            List<Axis> axis = getAxis(parameters.get("chxt"), parameters.get("chxl"));
            for (Axis axis2 : axis) {
                if (axis2.getLabel().equals("x")) {
                    chart.setBottomAxis(axis2);
                } else if (axis2.getLabel().equals("y")) {
                    chart.setLeftAxis(axis2);
                }
            }
            setGrid(chart, parameters.get("chg"));
            chart.addMarkers(getMarkers(parameters.get("chm")));
            return chart;
        } else if (type.equals("p")) {
            PieChart chart = new PieChart();
            chart.setDimension(getDimension(parameters.get("chs")));
            chart.setData(getData(chart, parameters.get("chd")));
            if (parameters.get("chl") != null) {
                List<Label> labels = getLabels(parameters.get("chl"));
                for (Label label : labels) {
                    chart.addLabel(label);
                }
            }
            return chart;
            // params.get("chxl")
        } else if (type.equals("bvs")) {
            VerticalStackBarChart chart = new VerticalStackBarChart();
            chart.setDimension(getDimension(parameters.get("chs")));
            // chart.setMultipleData(getMultipleData(chart,
            // parameters.get("chd")));
            chart.setColors(getColors(parameters.get("chco")));

            chart.setBarWidth(getBarWidth(parameters.get("chbh")));
            chart.setSpaceBetweenBars(getSpaceBetweenBars(parameters.get("chbh")));
            chart.setSpaceBetweenGroups(getSpaceBetweenGroups(parameters.get("chbh")));

            return chart;
            // params.get("chxl")
        }
        return null;
    }

    private List<ShapeMarker> getMarkers(String string) {
        if (string == null) {
            return Collections.EMPTY_LIST;
        }
        final String[] params = splitPipe(string);
        List<ShapeMarker> list = new ArrayList<ShapeMarker>();
        for (int i = 0; i < params.length; i++) {

            list.add(getMarker(params[i]));
        }
        return list;
    }

    private ShapeMarker getMarker(String string) {
        final String[] params = string.split(",");
        // if(params[0].equals("o")){
        CircleMarker c = new CircleMarker();
        c.setColor(Color.decode("#" + params[1]));
        c.setDataSetIndex(Integer.parseInt(params[2]));
        c.setData(Double.parseDouble(params[3]));
        c.setSize(Float.parseFloat(params[4]));
        // }
        return c;
    }

    private List<Stroke> getStrokes(String string) {
        List<Stroke> list = new ArrayList<Stroke>();
        System.err.println("full:" + string);
        final String[] params = splitPipe(string);
        for (int i = 0; i < params.length; i++) {
            System.err.println("f:" + params[i]);
            list.add(getSroke(params[i]));
        }
        return list;
    }

    private Stroke getSroke(String string) {
        System.err.println("s:" + string);
        final String[] params = string.split(",");
        float thickness = Float.parseFloat(params[0]);
        float lineLength = Float.parseFloat(params[1]);
        float blankLenght = Float.parseFloat(params[2]);
        return new BasicStroke(thickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND, 0, new float[] { lineLength, blankLenght }, 0);
    }

    private void setGrid(LineChart chart, String string) {
        if (string == null)
            return;
        final String[] params = string.split(",");
        if (params.length > 0) {
            Double dx = Double.parseDouble(params[0]);
            chart.setGridXStep(dx);

            if (params.length > 1) {
                Double dy = Double.parseDouble(params[1]);
                chart.setGridYStep(dy);

                if (params.length > 2) {
                    Float li = Float.parseFloat(params[2]);
                    chart.setGridSegment(li.floatValue(), 3);

                    if (params.length > 3) {
                        Float bl = Float.parseFloat(params[3]);
                        chart.setGridSegment(li.floatValue(), bl.floatValue());
                    }
                }
            }
        }
    }

    private List<List<Number>> getMultipleData(LineChart chart, String string) {
        List<List<Number>> r = new ArrayList<List<Number>>();

        if (string.startsWith("s:")) {
            if (chart.getLowerRange() == null) {
                chart.setLowerRange(0);
            }
            if (chart.getHigherRange() == null) {
                chart.setHigherRange(61);
            }
            string = string.substring(2);
            final String[] params = string.split(",");

            for (int i = 0; i < params.length; i++) {
                String p = params[i];
                r.add(getSimpleEncodedData(p));
            }
        }
        return r;
    }

    private Integer getSpaceBetweenBars(String string) {
        final String[] params = string.split(",");
        if (params.length > 1) {
            return Integer.valueOf(params[1]);
        }
        return null;

    }

    private Integer getSpaceBetweenGroups(String string) {
        final String[] params = string.split(",");
        if (params.length > 2) {
            return Integer.valueOf(params[2]);
        }
        return null;

    }

    private Integer getBarWidth(String string) {
        final String[] params = string.split(",");
        if (params.length > 0) {
            return Integer.valueOf(params[0]);
        }
        return null;

    }

    private List<Color> getColors(String string) {
        if (string == null) {
            return Collections.EMPTY_LIST;
        }
        String[] colors = string.split(",");
        List<Color> result = new ArrayList<Color>(colors.length);
        for (int i = 0; i < colors.length; i++) {
            String color = colors[i];
            Color c = Color.BLACK;
            try {
                c = Color.decode("#" + color);
            } catch (Exception e) {
                throw new IllegalArgumentException("Cannot parse color: " + color);
            }

            result.add(c);
        }
        return result;
    }

    private List<Label> getLabels(String labels) {

        String t[] = splitPipe(labels);
        List<Label> result = new ArrayList<Label>(t.length);
        for (int j = 0; j < t.length; j++) {
            String label = t[j];
            label = label.replace('+', ' ');
            result.add(new Label(label));

        }
        return result;
    }

    private void setRange(LineChart chart, String string) {
        int index = string.indexOf("s:");

        if (index == 0) {
            chart.setLowerRange(0);
            chart.setHigherRange(61);

        }

    }

    private List<Axis> getAxis(String string, String string2) {
        List<Axis> r = new ArrayList<Axis>(1);
        System.out.println("getAxis");
        System.out.println("s1:" + string);
        System.out.println("s2:" + string2);
        String[] axisTypes = null;
        String[] axisLabels = null;
        if (string != null) {
            axisTypes = string.split(",");
            for (int i = 0; i < axisTypes.length; i++) {
                String t = axisTypes[i];
                r.add(new Axis(t));
            }
        }
        if (string2 != null) {
            axisLabels = string2.split("[0-9]+:");

            for (int i = 0; i < axisLabels.length - 1; i++) {
                String labels = axisLabels[i + 1];
                System.out.println("=================================" + string2);
                System.out.println(i + ":" + labels);
                Axis axis = r.get(i);
                String t[] = splitPipe(labels);
                for (int j = 1; j < t.length; j++) {
                    String label = t[j];

                    label = label.replace('+', ' ');
                    axis.addLabel(new AxisLabel(label));

                }
                System.out.println(axis.getLabels());
            }
        }
        return r;
    }

    private String[] splitPipe(String labels) {
        List<String> l = new ArrayList<String>();
        String t = "";
        for (int i = 0; i < labels.length(); i++) {
            char c = labels.charAt(i);
            if (c == '|') {
                l.add(t);
                t = "";
            } else {
                t += c;
            }
        }
        if (t.length() > 0) {
            l.add(t);
        }
        return l.toArray(new String[0]);
    }

    private List<Number> getData(Chart chart, String string) {
        List<Number> r = null;
        int index = string.indexOf("s:");

        if (index == 0) {
            string = string.substring(2);
            r = getSimpleEncodedData(string);
        }

        return r;
    }

    private List<Number> getSimpleEncodedData(String string) {
        List<Number> r;
        r = new ArrayList<Number>();
        for (int i = 0; i < string.length(); i++) {
            char c1 = string.charAt(i);
            if (c1 == '_') {
                r.add(null);
            } else {
                int value = valueOf[c1];
                r.add(Integer.valueOf(value));
            }
        }
        return r;
    }

    private Dimension getDimension(String string) {
        int i = string.indexOf('x');
        if (i > 0) {
            String ws = string.substring(0, i);
            String hs = string.substring(i + 1);
            return new Dimension(Integer.valueOf(ws), Integer.valueOf(hs));
        }
        return null;
    }
    /*
     * String simpleEncoding = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
     * 
     * function simpleEncode(values,maxValue) {
     * 
     * var chartData = ['s:']; for (var i = 0; i < values.length; i++) { var currentValue =
     * values[i]; if (!isNaN(currentValue) && currentValue >= 0) {
     * chartData.push(simpleEncoding.charAt(Math.round((simpleEncoding.length-1) * currentValue /
     * maxValue))); } else { chartData.push('_'); } } return chartData.join(''); }
     */
}
