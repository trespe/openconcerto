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
 
 package org.openconcerto.erp.graph;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jopenchart.Axis;
import org.jopenchart.AxisLabel;
import org.jopenchart.DataModel1D;
import org.jopenchart.DataModel2D;
import org.jopenchart.DataModelMultiple;
import org.jopenchart.DataModelPoint;
import org.jopenchart.Label;
import org.jopenchart.barchart.VerticalBarChart;
import org.jopenchart.barchart.VerticalGroupBarChart;
import org.jopenchart.barchart.VerticalStackBarChart;
import org.jopenchart.gauge.AngularGauge;
import org.jopenchart.linechart.LineChart;
import org.jopenchart.piechart.PieChart;
import org.jopenchart.piechart.PieChartWithSeparatedLabels;
import org.jopenchart.scatterplot.PointChart;
import org.jopenchart.table.SimpleTable;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.PageSize;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfWriter;

public class GraphSamplePDF {

    /**
     * @param args
     * @throws DocumentException
     * @throws IOException
     */
    public static void main(String[] args) throws DocumentException, IOException {

        // ChartPanel panel = new ChartPanel(c);
        FileOutputStream baos = new FileOutputStream(new File("OpenConcerto_Chart.pdf"));
        Document document = new Document(PageSize.A4);
        // PdfWriter writer = new PdfWriter(document, baos);

        PdfWriter writer = PdfWriter.getInstance(document, baos);

        document.open();
        PdfContentByte cb = writer.getDirectContent();
        System.out.println(document.getPageSize());
        createPage1(document, cb);

        document.newPage();
        createPage2(document, cb);
        document.close();
        baos.close();
    }

    private static void createPage1(Document document, PdfContentByte cb) {
        Graphics2D graphics2D = cb.createGraphics(document.getPageSize().getWidth(), document.getPageSize().getHeight());
        int w = 180;
        int h = 200;
        int y = 140;
        Font font = new Font("Arial", Font.PLAIN, 12);
        graphics2D.setFont(font);
        int xLogo = 30;
        // AngularGauge c = new AngularGauge();
        // c.setMinValue(0);
        // c.setMaxValue(100);
        // c.setValue(30);
        // c.setChartRectangle(new Rectangle(0, 100, 150, 150));
        // c.render(graphics2D);

        //
        int cX = xLogo;
        int cY = y + 30;
        // Line 1
        table1(graphics2D, cX, cY);
        legend(graphics2D, cX, cY, "a) Classique");
        cX += w;

        table2(graphics2D, cX, cY);
        legend(graphics2D, cX, cY, "b) Design");
        cX += w;
        table3(graphics2D, cX, cY);
        legend(graphics2D, cX, cY, "c) Maximum par colonne");
        // Line 2
        cY += h;
        cX = xLogo;
        line1(graphics2D, cX, cY);
        legend(graphics2D, cX, cY, "a) Courbe simple");
        cX += w;

        line2(graphics2D, cX, cY);
        legend(graphics2D, cX, cY, "b) Lignes multiples");
        cX += w;
        line3(graphics2D, cX, cY);
        legend(graphics2D, cX, cY, "c) Quantitatif");

        // Line 3
        cY += h;
        cX = xLogo;
        piechart1(graphics2D, cX, cY);
        legend(graphics2D, cX, cY, "a) Simple");
        cX += w;

        piechart2(graphics2D, cX, cY);
        legend(graphics2D, cX, cY, "b) Anneau");
        cX += w;
        piechart3(graphics2D, cX, cY);
        legend(graphics2D, cX, cY, "c) Complet avec légende");
        // Logo

        graphics2D.setFont(new Font("Arial", Font.PLAIN, 24));
        graphics2D.setColor(new Color(66, 119, 167));

        graphics2D.drawString("Open", xLogo, 48);
        graphics2D.setColor(new Color(178, 81, 81));
        graphics2D.drawString("Concerto", xLogo + 59, 48);
        graphics2D.setColor(Color.BLACK);

        // Main title and copyright
        drawCopyrightAndFooter(graphics2D, xLogo, 1);

        // Titles
        graphics2D.setFont(graphics2D.getFont().deriveFont(12f));
        graphics2D.setFont(graphics2D.getFont().deriveFont(Font.BOLD));
        graphics2D.drawString("Tableaux", xLogo, y);
        y += h;
        graphics2D.drawString("Graphiques", xLogo, y);
        y += h;
        graphics2D.drawString("Diagrammes circulaires", xLogo, y);
        graphics2D.dispose();
    }

    private static void createPage2(Document document, PdfContentByte cb) {
        Graphics2D graphics2D = cb.createGraphics(document.getPageSize().getWidth(), document.getPageSize().getHeight());
        int w = 180;
        int h = 200;
        int y = 140;
        Font font = new Font("Arial", Font.PLAIN, 12);
        graphics2D.setFont(font);
        int xLogo = 30;

        //
        int cX = xLogo;
        int cY = y + 30;
        // Line 1
        barchart1(graphics2D, cX, cY);
        legend(graphics2D, cX, cY, "a) Simple");
        cX += w;

        barchart2(graphics2D, cX, cY);
        legend(graphics2D, cX, cY, "b) Groupé");
        cX += w;
        barchart3(graphics2D, cX, cY);
        legend(graphics2D, cX, cY, "c) Empillé");
        // Line 2
        cY += h;
        cX = xLogo;
        gauge1(graphics2D, cX, cY);
        legend(graphics2D, cX, cY, "a) Valeur");
        cX += w;

        gauge2(graphics2D, cX, cY);
        legend(graphics2D, cX, cY, "b) Pourcentage");
        cX += w;
        gauge3(graphics2D, cX, cY);
        legend(graphics2D, cX, cY, "c) Qualitatif");

        // Line 3
        cY += h;
        cX = xLogo;
        cloud1(graphics2D, cX, cY);
        legend(graphics2D, cX, cY, "a) Points");
        cX += w;

        cloud2(graphics2D, cX, cY);
        legend(graphics2D, cX, cY, "b) Bulles");
        cX += w;
        cloud3(graphics2D, cX, cY);
        legend(graphics2D, cX, cY, "c) Zones");
        // Logo

        graphics2D.setFont(new Font("Arial", Font.PLAIN, 24));
        graphics2D.setColor(new Color(66, 119, 167));

        graphics2D.drawString("Open", xLogo, 48);
        graphics2D.setColor(new Color(178, 81, 81));
        graphics2D.drawString("Concerto", xLogo + 59, 48);
        graphics2D.setColor(Color.BLACK);

        drawCopyrightAndFooter(graphics2D, xLogo, 2);
        // Titles
        graphics2D.setFont(graphics2D.getFont().deriveFont(12f));
        graphics2D.setFont(graphics2D.getFont().deriveFont(Font.BOLD));
        graphics2D.drawString("Diagrammes en bâtons", xLogo, y);
        y += h;
        graphics2D.drawString("Jauge", xLogo, y);
        y += h;
        graphics2D.drawString("Nuages", xLogo, y);
        graphics2D.dispose();
    }

    public static void drawCopyrightAndFooter(Graphics2D graphics2D, int xLogo, int page) {
        // Main title and copyright
        graphics2D.setFont(graphics2D.getFont().deriveFont(16f));
        graphics2D.drawString("Exemples de représentation de données", xLogo, 90);
        graphics2D.setColor(Color.BLACK);
        graphics2D.setFont(graphics2D.getFont().deriveFont(6f));
        graphics2D.drawString("© ILM Informatique. Tous droits réservés. OpenConcerto est une marque déposée.", 30, 810);
        graphics2D.setFont(graphics2D.getFont().deriveFont(10f));
        graphics2D.drawString("Page " + page + " / 2", 500, 810);
    }

    private static void legend(Graphics2D graphics2D, int cX, int cY, String string) {
        Font font = new Font("Arial", Font.ITALIC, 8);
        graphics2D.setColor(Color.BLACK);
        graphics2D.setFont(font);
        graphics2D.drawString(string, cX, cY - 10);

    }

    private static void piechart1(Graphics2D graphics2D, int cX, int cY) {
        PieChart c = new PieChart();
        List<Color> colors = new ArrayList<Color>();
        colors.add(new Color(178, 81, 81));
        colors.add(new Color(66, 119, 167));

        c.setColors(colors);
        c.addLabel(new Label("PME"));
        c.addLabel(new Label("Grands comptes"));
        c.addLabel(new Label("TPE"));

        c.setDimension(new Dimension(160, 120));

        ArrayList<Number> l = new ArrayList<Number>();
        l.add(50);
        l.add(20);
        l.add(10);
        c.setData(l);
        AffineTransform saveAT = graphics2D.getTransform();
        Font font = new Font("Arial", Font.PLAIN, 10);

        graphics2D.setFont(font);
        graphics2D.transform(AffineTransform.getTranslateInstance(cX, cY));

        c.render(graphics2D);

        graphics2D.setTransform(saveAT);
    }

    private static void piechart3(Graphics2D graphics2D, int cX, int cY) {
        PieChart c = new PieChartWithSeparatedLabels();

        c.addLabel(new Label("Open"));
        c.addLabel(new Label("Concerto"));
        c.addLabel(new Label("GPL"));

        c.setDimension(new Dimension(90, 80));

        ArrayList<Number> l = new ArrayList<Number>();
        l.add(60);
        l.add(50);
        l.add(20);

        c.setData(l);

        c.setInnerColor(Color.BLACK);
        c.setInnerDimension(2, 2);
        AffineTransform saveAT = graphics2D.getTransform();
        Font font = new Font("Arial", Font.PLAIN, 10);

        graphics2D.setFont(font);
        graphics2D.transform(AffineTransform.getTranslateInstance(cX, cY));

        c.render(graphics2D);

        graphics2D.setTransform(saveAT);
    }

    private static void piechart2(Graphics2D graphics2D, int cX, int cY) {
        PieChart c = new PieChartWithSeparatedLabels();

        c.addLabel(new Label("V1"));
        c.addLabel(new Label("V2"));
        c.addLabel(new Label("V3"));
        c.addLabel(new Label("V4"));
        c.addLabel(new Label("V5"));
        c.addLabel(new Label("V6"));
        c.addLabel(new Label("V7"));
        c.addLabel(new Label("V8"));
        c.addLabel(new Label("V9"));
        c.addLabel(new Label("V10"));
        c.setDimension(new Dimension(130, 160));

        ArrayList<Number> l = new ArrayList<Number>();
        l.add(60);
        l.add(50);
        l.add(20);
        l.add(20);
        l.add(30);
        l.add(20);
        l.add(20);
        l.add(40);
        l.add(20);
        l.add(20);
        l.add(20);
        c.setData(l);

        c.setInnerColor(Color.BLACK);
        c.setInnerDimension(40, 40);

        List<Color> colors = new ArrayList<Color>();
        // colors.add(new Color(178, 81, 81));
        colors.add(new Color(66, 119, 167));

        c.setColors(colors);

        AffineTransform saveAT = graphics2D.getTransform();
        Font font = new Font("Arial", Font.PLAIN, 10);

        graphics2D.setFont(font);
        graphics2D.transform(AffineTransform.getTranslateInstance(cX, cY));

        c.render(graphics2D);

        graphics2D.setTransform(saveAT);
    }

    private static void line1(Graphics2D graphics2D, int cX, int cY) {
        LineChart chart = new LineChart();

        chart.setLowerRange(0);
        chart.setHigherRange(150);

        List<Number> data = new ArrayList<Number>();
        data.add(15);
        data.add(5);
        data.add(20);
        data.add(50);
        data.add(60);
        data.add(90);
        data.add(95);
        data.add(60);
        data.add(83);
        data.add(102);
        data.add(123);
        data.add(143);
        chart.setData(data);
        final Axis axis = new Axis("CA");
        axis.addLabel(new AxisLabel("0", 0));
        axis.addLabel(new AxisLabel("50 000", 50));
        axis.addLabel(new AxisLabel("100 000", 100));
        axis.setLabelVisible(true);
        chart.setLeftAxis(axis);

        final Axis axis2 = new Axis("CA");
        axis2.addLabel(new AxisLabel("0%", 0));
        axis2.addLabel(new AxisLabel("50%", 6));
        axis2.addLabel(new AxisLabel("100%", 11));
        axis2.setLabelVisible(true);
        axis2.setMarkerLenght(4);
        axis.setMarkerLenght(4);
        chart.setGridXStep(50D);
        chart.setBottomAxis(axis2);

        chart.setDimension(new Dimension(160, 130));
        AffineTransform saveAT = graphics2D.getTransform();
        Font font = new Font("Arial", Font.PLAIN, 10);

        graphics2D.setFont(font);
        graphics2D.transform(AffineTransform.getTranslateInstance(cX, cY));

        chart.render(graphics2D);

        graphics2D.setTransform(saveAT);
    }

    private static void line2(Graphics2D graphics2D, int cX, int cY) {
        LineChart chart = new LineChart();

        List<Color> colors = new ArrayList<Color>();
        colors.add(new Color(66, 119, 167));
        colors.add(new Color(178, 81, 81));
        chart.setColors(colors);
        chart.setLowerRange(0);
        chart.setHigherRange(150);

        final List<Number> data = new ArrayList<Number>();
        data.add(3);
        data.add(5);
        data.add(20);
        data.add(50);
        data.add(60);
        data.add(90);
        data.add(95);
        data.add(60);
        data.add(83);
        data.add(102);
        data.add(133);
        data.add(143);
        final List<Number> data2 = new ArrayList<Number>();
        data2.add(24);
        data2.add(8);
        data2.add(10);
        data2.add(60);
        data2.add(100);
        data2.add(120);
        data2.add(130);
        data2.add(80);
        data2.add(90);
        data2.add(50);
        data2.add(30);
        data2.add(40);
        final List<List<Number>> multipleData = new ArrayList<List<Number>>();
        multipleData.add(data2);
        multipleData.add(data);

        chart.setDataModel(new DataModelMultiple(multipleData));

        final Axis axis = new Axis("CA");
        axis.addLabel(new AxisLabel("", 0));
        axis.addLabel(new AxisLabel("5", 25));
        axis.addLabel(new AxisLabel("10", 50));
        axis.addLabel(new AxisLabel("15", 75));
        axis.addLabel(new AxisLabel("20", 100));

        axis.setLabelVisible(true);
        chart.setLeftAxis(axis);

        final Axis axis2 = new Axis("CA");
        axis2.addLabel(new AxisLabel("2010", 0));
        axis2.addLabel(new AxisLabel("2011", 6));
        axis2.addLabel(new AxisLabel("2012", 11));
        axis2.setLabelVisible(true);
        axis2.setMarkerLenght(4);

        chart.setBottomAxis(axis2);

        // chart.setChartRectangle(new Rectangle(0, 100, 150, 150));
        chart.setDimension(new Dimension(160, 130));
        AffineTransform saveAT = graphics2D.getTransform();
        Font font = new Font("Arial", Font.PLAIN, 10);

        graphics2D.setFont(font);
        graphics2D.transform(AffineTransform.getTranslateInstance(cX, cY));

        chart.render(graphics2D);

        graphics2D.setTransform(saveAT);
    }

    private static void line3(Graphics2D graphics2D, int cX, int cY) {
        LineChart chart = new LineChart();

        chart.setLowerRange(-150);
        chart.setHigherRange(150);
        List<Color> colors = new ArrayList<Color>();
        colors.add(new Color(178, 81, 81));
        colors.add(new Color(66, 119, 167));

        chart.setColors(colors);
        chart.setFillColor(new Color(178, 81, 81));

        chart.setGridYStep(15.0D);

        List<Number> data = new ArrayList<Number>();
        data.add(0);
        data.add(5);
        data.add(20);
        data.add(50);
        data.add(60);
        data.add(90);
        data.add(95);
        data.add(60);
        data.add(83);
        data.add(105);
        data.add(60);
        data.add(45);
        chart.setData(data);
        final Axis axis = new Axis("CA");
        axis.addLabel(new AxisLabel("0", 0));
        axis.addLabel(new AxisLabel("7 kg", 150));
        axis.addLabel(new AxisLabel("3 kg", 64));
        final AxisLabel label = new AxisLabel("6 kg", 128);
        label.setColor(new Color(178, 81, 81));
        axis.addLabel(label);

        axis.setLabelVisible(true);
        chart.setLeftAxis(axis);

        final Axis axis2 = new Axis("CA");

        axis2.setMarkerLenght(4);
        axis2.setLabelVisible(true);
        axis2.addLabel(new AxisLabel("Mars", 0));
        axis2.addLabel(new AxisLabel("Avril", 6));
        axis2.addLabel(new AxisLabel("Mai", 11));

        chart.setBottomAxis(axis2);

        // chart.setChartRectangle(new Rectangle(0, 100, 150, 150));
        chart.setDimension(new Dimension(160, 130));
        AffineTransform saveAT = graphics2D.getTransform();
        Font font = new Font("Arial", Font.PLAIN, 10);

        graphics2D.setFont(font);
        graphics2D.transform(AffineTransform.getTranslateInstance(cX, cY));

        chart.render(graphics2D);

        graphics2D.setTransform(saveAT);
    }

    private static void table1(Graphics2D graphics2D, int cX, int cY) {
        SimpleTable chart = new SimpleTable(new DataModel2D(4, 3));

        chart.setDimension(new Dimension(160, 130));
        AffineTransform saveAT = graphics2D.getTransform();
        Font font = new Font("Arial", Font.PLAIN, 10);

        graphics2D.setFont(font);
        graphics2D.transform(AffineTransform.getTranslateInstance(cX, cY));

        chart.render(graphics2D);

        graphics2D.setTransform(saveAT);
    }

    private static void table2(Graphics2D graphics2D, int cX, int cY) {
        SimpleTable chart = new SimpleTable(new DataModel2D(4, 3));
        chart.setGridColor(null);
        chart.setLabelColorBackground(null);
        Color col = new Color(232, 242, 254);
        for (int i = 0; i < 3; i++) {
            chart.setBackgoundColor(col, 0, i);
            chart.setBackgoundColor(col, 2, i);
        }

        chart.setDimension(new Dimension(160, 110));
        AffineTransform saveAT = graphics2D.getTransform();
        Font font = new Font("Arial", Font.PLAIN, 10);

        graphics2D.setFont(font);
        graphics2D.transform(AffineTransform.getTranslateInstance(cX, cY));

        chart.render(graphics2D);

        graphics2D.setTransform(saveAT);
    }

    private static void table3(Graphics2D graphics2D, int cX, int cY) {
        SimpleTable chart = new SimpleTable(new DataModel2D(6, 3));
        chart.setGridColor(Color.WHITE);
        chart.setLabelColorBackground(new Color(154, 12, 81));
        chart.setLabelColorText(Color.white);
        chart.setBackgoundColor(Color.ORANGE, 2, 0);
        chart.setForegoundColor(Color.WHITE, 2, 0);
        chart.setBackgoundColor(Color.ORANGE, 2, 2);
        chart.setForegoundColor(Color.WHITE, 2, 2);
        chart.setBackgoundColor(Color.ORANGE, 5, 1);
        chart.setForegoundColor(Color.WHITE, 5, 1);

        chart.setDimension(new Dimension(160, 130));
        AffineTransform saveAT = graphics2D.getTransform();
        Font font = new Font("Arial", Font.PLAIN, 10);

        graphics2D.setFont(font);
        graphics2D.transform(AffineTransform.getTranslateInstance(cX, cY));

        chart.render(graphics2D);

        graphics2D.setTransform(saveAT);
    }

    private static void barchart1(Graphics2D graphics2D, int cX, int cY) {
        VerticalBarChart c = new VerticalBarChart();
        c.setColor(new Color(0, 155, 100));
        // c.setBackgroundRenderer(new SolidAreaRenderer(Color.pink));
        Axis axis = new Axis("y");
        axis.addLabel(new AxisLabel("0"));
        axis.addLabel(new AxisLabel("500 €"));
        axis.addLabel(new AxisLabel("1000 €"));
        c.setLeftAxis(axis);

        Axis axisX = new Axis("x");
        axisX.addLabel(new AxisLabel("J", 1));
        axisX.addLabel(new AxisLabel("F", 2));
        axisX.addLabel(new AxisLabel("M", 3));
        axisX.addLabel(new AxisLabel("A", 4));
        axisX.addLabel(new AxisLabel("M", 5));
        axisX.addLabel(new AxisLabel("J", 6));
        axisX.addLabel(new AxisLabel("J", 7));
        axisX.addLabel(new AxisLabel("A", 8));
        axisX.addLabel(new AxisLabel("S", 9));
        axisX.addLabel(new AxisLabel("O", 10));
        axisX.addLabel(new AxisLabel("N", 11));
        axisX.addLabel(new AxisLabel("D", 12));
        c.setBottomAxis(axisX);
        c.setBarWidth(8);
        c.setSpaceBetweenBars(3);
        c.addModel(new DataModel1D(new Float[] { 5f, 10f, 50f, 30f, 200f, 20f, 30f, 120f, 180f, 70f, 10f, 120f }));

        c.setDimension(new Dimension(160, 140));
        AffineTransform saveAT = graphics2D.getTransform();
        Font font = new Font("Arial", Font.PLAIN, 10);

        graphics2D.setFont(font);
        graphics2D.transform(AffineTransform.getTranslateInstance(cX, cY));

        c.render(graphics2D);

        graphics2D.setTransform(saveAT);
    }

    private static void barchart2(Graphics2D graphics2D, int cX, int cY) {
        VerticalGroupBarChart c = new VerticalGroupBarChart();
        List<Color> colors = new ArrayList<Color>();
        colors.add(Color.decode("#4A79A5"));
        colors.add(Color.decode("#639ACE"));
        colors.add(Color.decode("#94BAE7"));
        c.setColors(colors);

        Axis axis = new Axis("y");
        axis.addLabel(new AxisLabel("0"));
        axis.addLabel(new AxisLabel("100"));
        axis.addLabel(new AxisLabel("200"));
        c.setLeftAxis(axis);

        Axis axisX = new Axis("x");
        axisX.addLabel(new AxisLabel("Janvier", 1));
        axisX.addLabel(new AxisLabel("Février", 2));
        axisX.addLabel(new AxisLabel("Mars", 3));

        c.setBottomAxis(axisX);

        c.setBarWidth(8);
        c.addModel(new DataModel1D(new Float[] { 5f, 10f, 50f }));
        c.addModel(new DataModel1D(new Float[] { 20f, 90f, 55f }));
        c.addModel(new DataModel1D(new Float[] { 28f, 9f, 60f }));

        c.setDimension(new Dimension(160, 140));
        AffineTransform saveAT = graphics2D.getTransform();
        Font font = new Font("Arial", Font.PLAIN, 10);

        graphics2D.setFont(font);
        graphics2D.transform(AffineTransform.getTranslateInstance(cX, cY));

        c.render(graphics2D);

        graphics2D.setTransform(saveAT);
    }

    private static void barchart3(Graphics2D graphics2D, int cX, int cY) {
        VerticalStackBarChart c = new VerticalStackBarChart();
        c.setColor(new Color(0, 155, 100));
        // c.setBackgroundRenderer(new SolidAreaRenderer(Color.pink));
        Axis axis = new Axis("y");
        axis.addLabel(new AxisLabel("0 %"));
        axis.addLabel(new AxisLabel("100 %"));
        axis.addLabel(new AxisLabel("200 %"));
        c.setLeftAxis(axis);

        c.addModel(new DataModel1D(new Float[] { 5f, 10f, 50f, 30f, 190f, 20f, 30f }));
        c.addModel(new DataModel1D(new Float[] { 20f, 90f, 50f, 70f, 10f, 180f, 70f }));

        c.setDimension(new Dimension(160, 125));
        AffineTransform saveAT = graphics2D.getTransform();
        Font font = new Font("Arial", Font.PLAIN, 10);

        graphics2D.setFont(font);
        graphics2D.transform(AffineTransform.getTranslateInstance(cX, cY));

        c.render(graphics2D);

        graphics2D.setTransform(saveAT);
    }

    private static void gauge1(Graphics2D graphics2D, int cX, int cY) {
        AngularGauge c = new AngularGauge();
        c.setMinValue(0);
        c.setMaxValue(800);
        c.setValue(324);
        c.setChartRectangle(new Rectangle(0, 0, 140, 140));
        AffineTransform saveAT = graphics2D.getTransform();
        Font font = new Font("Arial", Font.PLAIN, 38);

        graphics2D.setFont(font);
        graphics2D.transform(AffineTransform.getTranslateInstance(cX, cY));

        c.render(graphics2D);

        graphics2D.setTransform(saveAT);
    }

    private static void gauge2(Graphics2D graphics2D, int cX, int cY) {
        AngularGauge c = new AngularGauge() {
            @Override
            public String getTextValue() {
                return super.getTextValue() + "%";
            }
        };
        c.setMinValue(0);
        c.setMaxValue(100);
        c.setValue(80);

        c.setColor(new Color(44, 120, 126));
        c.setChartRectangle(new Rectangle(0, 0, 140, 140));
        AffineTransform saveAT = graphics2D.getTransform();
        Font font = new Font("Arial", Font.PLAIN, 34);

        graphics2D.setFont(font);
        graphics2D.transform(AffineTransform.getTranslateInstance(cX, cY));

        c.render(graphics2D);

        graphics2D.setTransform(saveAT);
    }

    private static void gauge3(Graphics2D graphics2D, int cX, int cY) {
        AngularGauge c = new AngularGauge() {
            @Override
            public String getTextValue() {
                return "Hausse";
            }
        };
        c.setMinValue(0);
        c.setMaxValue(100);
        c.setValue(68);
        c.setBackgroundValueColor(null);
        c.setColor(new Color(66, 119, 167));
        c.setChartRectangle(new Rectangle(0, 0, 140, 140));
        AffineTransform saveAT = graphics2D.getTransform();
        Font font = new Font("Arial", Font.PLAIN, 24);

        graphics2D.setFont(font);
        graphics2D.transform(AffineTransform.getTranslateInstance(cX, cY));

        c.render(graphics2D);

        graphics2D.setTransform(saveAT);
    }

    private static void cloud1(Graphics2D graphics2D, int cX, int cY) {
        PointChart chart = new PointChart(new DataModelPoint(15));

        chart.setLowerRange(0);
        chart.setHigherRange(150);

        final Axis axis = new Axis("CA");
        axis.addLabel(new AxisLabel("0", 0));
        axis.addLabel(new AxisLabel("50 000", 50));
        axis.addLabel(new AxisLabel("100 000", 100));
        axis.setLabelVisible(true);
        chart.setLeftAxis(axis);

        final Axis axis2 = new Axis("CA");
        axis2.addLabel(new AxisLabel("0%", 0));
        axis2.addLabel(new AxisLabel("50%", 5.5));
        axis2.addLabel(new AxisLabel("100%", 11));
        axis2.setLabelVisible(true);
        axis2.setMarkerLenght(4);
        axis.setMarkerLenght(4);
        chart.setGridXStep(50D);
        chart.setBottomAxis(axis2);

        chart.setDimension(new Dimension(160, 130));
        AffineTransform saveAT = graphics2D.getTransform();
        Font font = new Font("Arial", Font.PLAIN, 10);

        graphics2D.setFont(font);
        graphics2D.transform(AffineTransform.getTranslateInstance(cX, cY));

        chart.render(graphics2D);

        graphics2D.setTransform(saveAT);
    }

    private static void cloud2(Graphics2D graphics2D, int cX, int cY) {
        PointChart chart = new PointChart(new DataModelPoint(6));

        chart.setLowerRange(0);
        chart.setHigherRange(150);

        final Axis axis = new Axis("CA");
        axis.addLabel(new AxisLabel("0", 0));
        axis.addLabel(new AxisLabel("100", 50));
        axis.addLabel(new AxisLabel("200", 100));
        axis.setLabelVisible(true);
        axis.setMarkerLenght(4);
        chart.setLeftAxis(axis);

        final Axis axis2 = new Axis("CA");
        axis2.addLabel(new AxisLabel("0", 0));
        axis2.addLabel(new AxisLabel("50%", 6));
        axis2.addLabel(new AxisLabel("100%", 11));
        axis2.setLabelVisible(true);
        axis2.setMarkerLenght(4);
        chart.setBottomAxis(axis2);
        chart.setPointSize(20);
        chart.setColor(new Color(66, 119, 167));
        chart.setType(PointChart.TYPE_CIRCLE);

        chart.setDimension(new Dimension(160, 130));
        AffineTransform saveAT = graphics2D.getTransform();
        Font font = new Font("Arial", Font.PLAIN, 10);

        graphics2D.setFont(font);
        graphics2D.transform(AffineTransform.getTranslateInstance(cX, cY));

        chart.render(graphics2D);

        graphics2D.setTransform(saveAT);
    }

    private static void cloud3(Graphics2D graphics2D, int cX, int cY) {
        PointChart chart = new PointChart(new DataModelPoint(7));

        chart.setLowerRange(0);
        chart.setHigherRange(150);

        final Axis axis = new Axis("CA");
        axis.addLabel(new AxisLabel("0", 0));
        axis.addLabel(new AxisLabel("1", 50));
        axis.addLabel(new AxisLabel("2", 100));
        axis.setLabelVisible(true);
        axis.setMarkerLenght(4);
        chart.setLeftAxis(axis);

        final Axis axis2 = new Axis("CA");
        axis2.addLabel(new AxisLabel("2012", 0));
        axis2.addLabel(new AxisLabel("2013", 6));
        axis2.addLabel(new AxisLabel("2014", 11));
        axis2.setLabelVisible(true);
        axis2.setMarkerLenght(4);
        chart.setBottomAxis(axis2);
        chart.setPointSize(40);

        chart.setColor(new Color(66, 119, 167, 125));
        chart.addColor(new Color(66, 119, 167, 125));
        chart.addColor(new Color(66, 119, 167, 125));
        chart.addColor(new Color(66, 119, 167, 125));
        chart.addColor(new Color(66, 119, 167, 125));
        chart.addColor(new Color(66, 119, 167, 125));
        chart.addColor(new Color(66, 119, 167, 125));
        chart.addColor(new Color(66, 119, 167, 125));
        chart.setType(PointChart.TYPE_PLAIN);

        chart.setDimension(new Dimension(160, 130));
        AffineTransform saveAT = graphics2D.getTransform();
        Font font = new Font("Arial", Font.PLAIN, 10);

        graphics2D.setFont(font);
        graphics2D.transform(AffineTransform.getTranslateInstance(cX, cY));

        chart.render(graphics2D);

        graphics2D.setTransform(saveAT);
    }

}
