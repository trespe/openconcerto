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
 
 package org.openconcerto.map.ui;

import org.openconcerto.map.model.MapPoint;
import org.openconcerto.map.model.MapPointSelection;
import org.openconcerto.map.model.Region;
import org.openconcerto.map.model.Ville;
import org.openconcerto.utils.ArrayListOfInt;
import org.openconcerto.utils.checks.MutableValueObject;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JPanel;


public class VilleRendererPanel extends JPanel implements MutableValueObject<String> {
    /**
     * 
     */
    private static final long serialVersionUID = -6477685676332334863L;

    private static final Color COLOR_POINT_VILLE = new Color(220, 80, 80);

    private static final BasicStroke STROKE_CONTOUR = new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    private static final BasicStroke BASIC_STROKE = new BasicStroke();

    double dx, dy;

    private int startX;

    private int startY;

    double offsetX = Ville.getMinXLambert();

    double offsetY = Ville.getMinYLambert();

    private int currentMouseX;
    private int currentMouseY;

    private List<VilleRendererListener> listenersVille = new Vector<VilleRendererListener>();

    private Set<Ville> alwayVisible = new HashSet<Ville>();
    private Map<Ville, Integer> matching = new HashMap<Ville, Integer>();
    private Set<Ville> hightlightVisible = new HashSet<Ville>();

    private boolean gridActicvated;

    private List<RegionPointsCache> cacheRegions = new ArrayList<RegionPointsCache>();
    private List<Ville> cacheVilles = new ArrayList<Ville>();

    // MODE
    public static final int MODE_MOVE = 0;
    public static final int MODE_DRAW = 1;
    private ArrayListOfInt drawX = new ArrayListOfInt();
    private ArrayListOfInt drawY = new ArrayListOfInt();
    private final MapPointSelection selectedPoints = new MapPointSelection();
    private int mode = MODE_MOVE;

    protected boolean quickdraw;

    private MapPoint highLightedPoint;

    private ArrayList<ModeListener> modeListeners = new ArrayList<ModeListener>();

    private Map<Ville, Color> colors = new HashMap<Ville, Color>();

    private final PropertyChangeSupport supp;

    private final List<Double> zoomValues = new ArrayList<Double>();

    int currentZoomIndex = 1;

    public VilleRendererPanel() {
        // zoom
        double first = 3328;
        for (int i = 0; i < 6; i++) {
            zoomValues.add(first);
            first = first / 2.0;
        }

        imgMarker = new ImageIcon(StatusPanel.class.getResource("marker.png")).getImage();

        this.supp = new PropertyChangeSupport(this);
        this.setBackground(Color.white);

        this.addMouseWheelListener(new MouseWheelListener() {

            public void mouseWheelMoved(MouseWheelEvent e) {
                if (VilleRendererPanel.this.mode == MODE_MOVE) {
                    // like google maps
                    if (e.getWheelRotation() < 0) {
                        zoomIn();
                    } else {
                        zoomOut();
                    }
                }
            }
        });

        this.addMouseMotionListener(new MouseMotionListener() {

            public void mouseDragged(MouseEvent e) {
                if (VilleRendererPanel.this.startX > 0) {

                    if (VilleRendererPanel.this.mode == MODE_MOVE) {
                        VilleRendererPanel.this.dx += e.getX() - VilleRendererPanel.this.startX;
                        VilleRendererPanel.this.dy += e.getY() - VilleRendererPanel.this.startY;
                    }
                    VilleRendererPanel.this.startX = e.getX();
                    VilleRendererPanel.this.startY = e.getY();
                    if (VilleRendererPanel.this.mode == MODE_DRAW) {
                        VilleRendererPanel.this.drawX.add(VilleRendererPanel.this.startX);
                        VilleRendererPanel.this.drawY.add(VilleRendererPanel.this.startY);
                    }
                    VilleRendererPanel.this.quickdraw = true;
                    repaint();
                }

            }

            public void mouseMoved(MouseEvent e) {
                VilleRendererPanel.this.currentMouseX = e.getX();
                VilleRendererPanel.this.currentMouseY = e.getY();
                if (VilleRendererPanel.this.mode == MODE_MOVE) {
                    fireVilleRendererListener();
                }
            }

        });
        this.addMouseListener(new MouseAdapter() {

            public void mousePressed(MouseEvent e) {
                requestFocus();
                if (VilleRendererPanel.this.mode == MODE_DRAW) {
                    setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                } else {

                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }

                VilleRendererPanel.this.startX = e.getX();
                VilleRendererPanel.this.startY = e.getY();
                if (VilleRendererPanel.this.mode == MODE_DRAW) {
                    VilleRendererPanel.this.drawX.clear();
                    VilleRendererPanel.this.drawY.clear();
                    VilleRendererPanel.this.drawX.add(VilleRendererPanel.this.startX);
                    VilleRendererPanel.this.drawY.add(VilleRendererPanel.this.startY);
                    repaint();
                }

            }

            public void mouseReleased(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
                VilleRendererPanel.this.startX = -1;
                VilleRendererPanel.this.startY = -1;
                VilleRendererPanel.this.quickdraw = false;
                if (VilleRendererPanel.this.mode == MODE_DRAW) {
                    final MapPointSelection newSel = new MapPointSelection();
                    for (int i = 0; i < VilleRendererPanel.this.drawX.size(); i++) {
                        long xx = XToLongitude(VilleRendererPanel.this.drawX.get(i));
                        long yy = YTolatitude(VilleRendererPanel.this.drawY.get(i));
                        MapPoint p = new MapPoint(xx, yy);
                        newSel.add(p);
                    }
                    setSelectedPoints(newSel);
                    VilleRendererPanel.this.drawX.clear();
                    VilleRendererPanel.this.drawY.clear();

                    setMode(MODE_MOVE);
                }
                repaint();

            }
        });
        this.addKeyListener(new KeyListener() {

            public void keyPressed(KeyEvent e) {
                // System.out.println("u:" + e.getKeyCode());
                // if (e.getKeyCode() == 127) {
                // List<Region> regions = Region.getRegions();
                //
                // for (Region region : regions) {
                //
                // List l = region.getPoints();
                // for (int i = 0; i < l.size(); i++) {
                // MapPoint element = (MapPoint) l.get(i);
                // if (element.getX() == highLightedPoint.getX() &&
                // element.getY() ==
                // highLightedPoint.getY()) {
                // l.remove(element);
                // }
                // }
                // }
                // rebuildCacheRegions();
                //
                // } else if (e.getKeyCode() == 83) {
                // Region.saveFile();
                // }
                // highLightedPoint = null;
                // repaint();

            }

            public void keyReleased(KeyEvent e) {
            }

            public void keyTyped(KeyEvent e) {
            }
        });

        rebuildCacheRegions();
        rebuildVisibleVilleCache();
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    public void setMode(int mode) {
        this.mode = mode;
        fireModeChanged();
    }

    public int getMode() {
        return this.mode;
    }

    public void addModeListener(ModeListener l) {
        this.modeListeners.add(l);
    }

    private void fireModeChanged() {
        for (ModeListener l : this.modeListeners) {
            l.modeChanged();
        }
    }

    public void setHighlight(Ville v) {
        this.hightlightVisible.add(v);
        repaint();
    }

    public void clearMatching() {
        this.matching.clear();
    }

    public void incrementMacthing(Ville v) {
        Integer i = this.matching.get(v);
        if (i == null) {
            this.matching.put(v, Integer.valueOf(1));
        } else {
            this.matching.put(v, Integer.valueOf(i + 1));
        }
    }

    public void setAlwayVisible(Ville v) {
        this.alwayVisible.add(v);
        rebuildVisibleVilleCache();
        repaint();
    }

    public void clearAlwayVisible() {
        for (Ville element : this.alwayVisible) {
            this.colors.remove(element);
        }
        this.alwayVisible.clear();
        rebuildVisibleVilleCache();
        repaint();
    }

    protected void setZoomIndex(int index) {
        if (index != this.currentZoomIndex) {
            long xCenter = XToLongitude(getWidth() / 2);
            long yCenter = YTolatitude(getHeight() / 2);
            this.currentZoomIndex = index;
            rebuildCacheRegions();
            rebuildVisibleVilleCache();
            centerScreenXYLambert(xCenter, yCenter);
            this.zoomListener.zoomChanged(index);
        }
    }

    @Override
    public void repaint() {
        // TODO Auto-generated method stub
        super.repaint();
        System.out.println("VilleRendererPanel.repaint()");
    }

    /**
	 *
	 */
    private void rebuildCacheRegions() {
        this.cacheRegions.clear();
        final List<Region> l = Region.getRegions();
        final double currentZoomValue = getCurrentZoomValue();
        final int size = l.size();
        for (int i = 0; i < size; i++) {
            Region region = l.get(i);
            if (i == 138 || i == 147 || i == 148 || i == 149) {
                continue;
            }
            this.cacheRegions.add(new RegionPointsCache(region, currentZoomValue));
        }
    }

    public void centerScreenXYLambert(long xLambert, long yLambert) {
        if (xLambert == Long.MAX_VALUE) {
            xLambert = 664843;
        }
        if (yLambert == Long.MAX_VALUE) {
            yLambert = 6872333;
        }

        System.out.println("\nCenterScreenLatLong: " + xLambert + "," + yLambert);
        System.out.println("Panel:" + this.getWidth() + " " + this.getHeight());
        System.out.println("Old OffSet: " + this.offsetX + "," + this.offsetY);
        this.dx = 0;
        this.dy = 0;
        this.offsetX = xLambert;
        this.offsetY = yLambert;

        System.out.println("New OffSet: " + this.offsetX + "," + this.offsetY);
        // dx=XToLongitude( this.getWidth() / 2);
        // dy=0;

        this.offsetX = XToLongitude(-this.getWidth() / 2);
        this.offsetY = YTolatitude((1.5) * this.getHeight());
        System.out.println("New OffSet: " + this.offsetX + "," + this.offsetY);
        repaint();
        // System.out.println(this.offsetX);
    }

    final Color BG_COLOR = new Color(154, 178, 204);

    private Image imgMarker;

    private ZoomListener zoomListener;

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        //

        g.setColor(BG_COLOR);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());

        // Image img=new ImageIcon("zoom/"+(this.currentZoomIndex+5)+".png").getImage();
        // g.drawImage(img, -60, -200, null);
        // 
        Graphics2D g2 = (Graphics2D) g;
        Stroke oStroke = g2.getStroke();
        g.setColor(Color.BLACK);
        drawRegion(g2);

        List<Ville> onScreenVille = this.getOnScreenVille();
        drawVille(g2, onScreenVille);

        drawPaintedArea(g2);

        drawEchelle(g2);
        if (this.highLightedPoint != null) {
            g2.setColor(Color.RED);
            int x = longitudeToX(this.highLightedPoint.getX());
            int y = latitudeToY(this.highLightedPoint.getY());
            g2.drawRect(x, y, 2, 2);

        }
        g2.setStroke(oStroke);

    }

    private List<Ville> getOnScreenVille() {
        final List<Ville> l = new ArrayList<Ville>();
        final long minXLambertViewable = XToLongitude(0);
        final long minYLambertViewable = YTolatitude(0);
        final long maxXLambertViewable = XToLongitude(this.getWidth());
        final long maxYLambertViewable = YTolatitude(this.getHeight());
        final int vCount = this.cacheVilles.size();
        for (int i = 0; i < vCount; i++) {
            final Ville v = this.cacheVilles.get(i);
            if (v.getXLambert() < minXLambertViewable)
                continue;
            if (v.getXLambert() > maxXLambertViewable)
                continue;
            if (v.getYLambert() > minYLambertViewable)
                continue;
            if (v.getYLambert() < maxYLambertViewable)
                continue;
            l.add(v);
        }

        return l;
    }

    private void drawPaintedArea(Graphics2D g2) {

        if (this.drawX.size() > 2) {

            g2.setColor(new Color(255, 0, 0, 128));
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setStroke(new BasicStroke(1.0f));
            final int[] toArrayX = this.drawX.toArray();
            final int[] toArrayY = this.drawY.toArray();
            g2.fillPolygon(toArrayX, toArrayY, this.drawX.size());
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(3.0f));
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.drawPolyline(toArrayX, toArrayY, this.drawX.size());
            g2.setColor(Color.GRAY);
            g2.drawLine(toArrayX[0], toArrayY[0], toArrayX[this.drawX.size() - 1], toArrayY[this.drawX.size() - 1]);
        } else if (this.getSelectedPoints().size() > 2) {
            g2.setColor(new Color(255, 0, 0, 128));
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g2.setStroke(new BasicStroke(1.0f));
            ArrayListOfInt x = new ArrayListOfInt(this.getSelectedPoints().size());
            ArrayListOfInt y = new ArrayListOfInt(this.getSelectedPoints().size());
            for (int i = 0; i < this.getSelectedPoints().size(); i++) {
                MapPoint element = this.getSelectedPoints().get(i);
                x.add(longitudeToX(element.getX()));
                y.add(latitudeToY(element.getY()));
            }

            final int[] toArrayX = x.toArray();
            final int[] toArrayY = y.toArray();
            g2.fillPolygon(toArrayX, toArrayY, toArrayX.length);
            g2.setColor(Color.BLACK);
            g2.setStroke(new BasicStroke(3.0f));
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            g2.drawPolygon(toArrayX, toArrayY, toArrayX.length);
            // g2.setColor(Color.GRAY);
            // g2.drawLine(toArrayX[0], toArrayY[0], toArrayX[drawX.size() - 1],
            // toArrayY[drawX.size() - 1]);

        }
    }

    private void drawEchelle(Graphics2D g) {

        int width = 100;
        if (isGridActivated()) {
            g.setColor(new Color(12, 35, 250));
            float dash[] = { 10.0f };
            g.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, dash, 0.0f));
            final int maxX = 1 + this.getWidth() / width;
            final int maxY = 1 + this.getHeight() / width;
            for (int i = 0; i < maxX; i++) {
                g.drawLine(width / 2 + i * width, 0, width / 2 + i * width, this.getHeight());
            }
            for (int j = 0; j < maxY; j++) {
                g.drawLine(0, width / 2 + j * width, this.getWidth(), width / 2 + j * width);
            }
        }
        g.setStroke(BASIC_STROKE);

        int posX = width / 2;
        int posY = this.getHeight() - 20;
        int height = 5;

        // Fond
        g.setColor(new Color(255, 255, 255, 200));
        g.fillRect(posX - 10, posY - 12, width + 20, height + 30);
        //
        g.setColor(Color.BLACK);
        g.drawLine(posX, posY, posX, posY + height);
        g.drawLine(posX, posY + height, posX + width, posY + height);
        g.drawLine(posX + width, posY, posX + width, posY + height);
        g.drawString("0", posX - 2, posY - 2);
        double f = width * getCurrentZoomValue() / 930D;

        final String echelle = String.valueOf(Double.valueOf(f).intValue());
        Rectangle2D rect = this.getFont().getStringBounds(echelle, g.getFontRenderContext());
        g.drawString(echelle, (int) (posX + width - rect.getWidth() / 2), posY - 2);

        g.drawString("Kms", posX + width / 2 - 10, posY + 16);

    }

    public void setGridActivated(boolean b) {
        this.gridActicvated = b;
        repaint();
    }

    private boolean isGridActivated() {
        return this.gridActicvated;
    }

    /**
     * Ajoute sauf si doublon
     * */
    private void addToCachedVille(Ville v) {
        if (!this.cacheVilles.contains(v))
            this.cacheVilles.add(v);
    }

    private void rebuildVisibleVilleCache() {

        final int zoom = this.currentZoomIndex;
        this.cacheVilles.clear();
        final List<Ville> villes = Ville.getVilles();
        this.cacheVilles.addAll(this.alwayVisible);

        if (zoom == 0) {
            addToCachedVille(Ville.getVilleFromVilleEtCode("Paris (75000)"));
            addToCachedVille(Ville.getVilleFromVilleEtCode("Lille (59800)"));
            addToCachedVille(Ville.getVilleFromVilleEtCode("Marseille (13000)"));
            addToCachedVille(Ville.getVilleFromVilleEtCode("Lyon (69000)"));
            addToCachedVille(Ville.getVilleFromVilleEtCode("Bordeaux (33300)"));
            addToCachedVille(Ville.getVilleFromVilleEtCode("Nantes (44200)"));
            addToCachedVille(Ville.getVilleFromVilleEtCode("Toulouse (31500)"));
            addToCachedVille(Ville.getVilleFromVilleEtCode("Le Havre (76620)"));
            addToCachedVille(Ville.getVilleFromVilleEtCode("Strasbourg (67000)"));
            addToCachedVille(Ville.getVilleFromVilleEtCode("Limoges (87280)"));
            addToCachedVille(Ville.getVilleFromVilleEtCode("Dijon (21000)"));
            addToCachedVille(Ville.getVilleFromVilleEtCode("Reims (51100)"));
            addToCachedVille(Ville.getVilleFromVilleEtCode("Rennes (35000)"));
            addToCachedVille(Ville.getVilleFromVilleEtCode("Orléans (45100)"));
            addToCachedVille(Ville.getVilleFromVilleEtCode("Pau (64000)"));
            Ville.sortByPopulation(this.cacheVilles);
            return;
        }
        int popMin = 0;
        if (zoom == 1) {
            popMin = 100000;
        } else if (zoom == 2) {
            popMin = 50000;
        } else if (zoom == 3) {
            popMin = 20000;
        } else if (zoom == 4) {
            popMin = 4000;
        } else {
            popMin = 1000;
        }
        // 5

        // Ajoutes les villes dont la population correspond au zoom

        final int vCount = villes.size();
        for (int i = 0; i < vCount; i++) {
            final Ville v = villes.get(i);
            if (v.getPopulation() >= popMin)
                addToCachedVille(v);
        }

        // Retires les villes en superposition
        if (zoom == 1) {
            removeIfNotAlwaysVisible(Ville.getVilleFromVilleEtCode("Villeurbanne (69100)"));
            removeIfNotAlwaysVisible(Ville.getVilleFromVilleEtCode("Aix-en-provence (13100)"));
        } else if (zoom == 2) {
            removeIfNotAlwaysVisible(Ville.getVilleFromVilleEtCode("Villeurbanne (69100)"));
            removeIfNotAlwaysVisible(Ville.getVilleFromVilleEtCode("Villeneuve-d'ascq (59491)"));
            removeIfNotAlwaysVisible(Ville.getVilleFromVilleEtCode("Sartrouville (78500)"));
            removeIfNotAlwaysVisible(Ville.getVilleFromVilleEtCode("Versailles (78000)"));
            removeIfNotAlwaysVisible(Ville.getVilleFromVilleEtCode("Vénissieux (69200)"));
            removeIfNotAlwaysVisible(Ville.getVilleFromVilleEtCode("Pessac (33600)"));
            removeIfNotAlwaysVisible(Ville.getVilleFromVilleEtCode("Antibes (06160)"));
            removeIfNotAlwaysVisible(Ville.getVilleFromVilleEtCode("La Seyne-sur-mer (83500)"));
            removeIfNotAlwaysVisible(Ville.getVilleFromVilleEtCode("Mérignac (33700)"));
            removeIfNotAlwaysVisible(Ville.getVilleFromVilleEtCode("Tourcoing (59200)"));
            removeIfNotAlwaysVisible(Ville.getVilleFromVilleEtCode("Roubaix (59100)"));
        }
        Ville.sortByPopulation(this.cacheVilles);
    }

    private void removeIfNotAlwaysVisible(Ville v) {
        if (!this.alwayVisible.contains(v))
            this.cacheVilles.remove(v);
    }

    private void drawVille(Graphics2D g2, List<Ville> villes) {
        List<Rectangle2D> rectangles = new ArrayList<Rectangle2D>();
        final int vCount = villes.size();
        final Font font = this.getFont();
        List<Ville> labelToDraw = new ArrayList<Ville>();
        g2.setColor(Color.BLACK);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(1.2f));
        for (int i = 0; i < vCount; i++) {
            final Ville v = villes.get(i);
            final int roundedX = (int) Math.round(longitudeToX(v.getXLambert()));
            final int roundedY = (int) Math.round(latitudeToY(v.getYLambert()));

            // Label
            final String label = v.getName();
            final Rectangle2D rect = font.getStringBounds(label, g2.getFontRenderContext());
            rect.setRect(roundedX + rect.getX() - 4 - rect.getWidth() / 2, roundedY - rect.getHeight() - 4, rect.getWidth() + 8, rect.getHeight());

            boolean overlap = false;
            final int size = rectangles.size();
            for (int j = 0; j < size; j++) {
                Rectangle2D r = rectangles.get(j);
                if (rect.intersects(r)) {
                    overlap = true;
                    break;
                }
            }

            if (!overlap) {
                rectangles.add(new Rectangle2D.Double(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight() * 2));

            }

            if (this.alwayVisible.contains(v) || !overlap) {
                // Rond
                Color c = this.colors.get(v);
                if (c == null)
                    c = COLOR_POINT_VILLE;
                g2.setColor(c);

                int tailleOvale = 12;
                final long population = v.getPopulation();
                if (population < 1000) {
                    tailleOvale = 4;
                } else if (population < 10000) {
                    tailleOvale = 6;
                } else if (population < 100000) {
                    tailleOvale = 10;
                } else if (population < 250000) {
                    tailleOvale = 12;
                    if (this.currentZoomIndex > 2) {
                        tailleOvale = 20;
                    }
                } else {
                    tailleOvale = 18;
                    if (this.currentZoomIndex > 2) {
                        tailleOvale = 24;
                    }

                }

                final int demiOvale = tailleOvale / 2;
                g2.fillOval(roundedX - demiOvale, roundedY - demiOvale, tailleOvale, tailleOvale);

                g2.setColor(Color.DARK_GRAY);
                g2.drawOval(roundedX - demiOvale, roundedY - demiOvale, tailleOvale, tailleOvale);

            }
            if (!overlap || (this.alwayVisible.contains(v) && this.currentZoomIndex == this.getMaxZoomIndex())) {
                labelToDraw.add(v);

            }
        }

        for (Ville v : this.hightlightVisible) {
            if (v != null) {
                double x1 = longitudeToX(v.getXLambert());
                double y1 = latitudeToY(v.getYLambert());
                int roundedX = (int) Math.round(x1);
                int roundedY = (int) Math.round(y1);
                roundedX -= 7;
                roundedY -= 34;
                g2.drawImage(imgMarker, roundedX, roundedY, null);
            }
        }
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        for (Ville v : labelToDraw) {
            if (hightlightVisible.contains(v)) {
                g2.setColor(new Color(255, 255, 0, 200));
            } else {
                if (this.quickdraw) {
                    g2.setColor(new Color(255, 255, 255));
                } else {
                    g2.setColor(new Color(255, 255, 255, 200));
                }
            }
            final int roundedX = (int) Math.round(longitudeToX(v.getXLambert()));
            final int roundedY = (int) Math.round(latitudeToY(v.getYLambert()));

            // Label
            String label = v.getName();
            if (this.matching.get(v) != null) {
                label += " (" + this.matching.get(v) + ")";
            }
            final Rectangle2D rect = font.getStringBounds(label, g2.getFontRenderContext());
            rect.setRect(roundedX + rect.getX() - 4 - rect.getWidth() / 2, roundedY - rect.getHeight() - 4, rect.getWidth() + 8, rect.getHeight());

            g2.fillRect((int) rect.getX(), (int) rect.getY(), (int) rect.getWidth(), (int) rect.getHeight());
            final int x = (int) (rect.getX() + 4);
            final int y = (int) (rect.getY() + rect.getHeight() - 4);

            g2.setColor(Color.BLACK);

            g2.drawString(label, x, y);

        }

    }

    /**
     * @param v
     * @return
     */
    private int longitudeToX(long longitude) {
        return (int) (this.dx + ((longitude - this.offsetX) / getCurrentZoomValue()));
    }

    private double getCurrentZoomValue() {
        return this.zoomValues.get(this.currentZoomIndex);
    }

    private long XToLongitude(double x) {
        return (long) (getCurrentZoomValue() * (x - this.dx) + this.offsetX);

    }

    /**
     * @param v
     * @return
     */
    private int latitudeToY(long lat) {
        return (int) (this.dy - ((lat - this.offsetY) / getCurrentZoomValue()) + this.getHeight());
    }

    private long YTolatitude(double y) {
        return (long) (getCurrentZoomValue() * (this.dy - y + this.getHeight()) + this.offsetY);
    }

    private void drawRegion(Graphics2D g2) {
        int regionP = 0;

        int rCount = this.cacheRegions.size();

        long minXLambertViewable = XToLongitude(0);
        long minYLambertViewable = YTolatitude(0);
        long maxXLambertViewable = XToLongitude(this.getWidth());
        final int finalHeight = this.getHeight();
        long maxYLambertViewable = YTolatitude(finalHeight);

        g2.setColor(Color.BLACK);

        int moveX = (int) (this.dx - this.offsetX / getCurrentZoomValue());
        int moveY = (int) (this.dy + this.getHeight() + this.offsetY / getCurrentZoomValue());
        for (int i = 0; i < rCount; i++) {

            RegionPointsCache r = this.cacheRegions.get(i);
            int regionPoints = r.size();
            if (regionPoints < 50)
                continue;
            // Ne pas tracer les regions nons visibles
            // Max
            if (r.getMaxX() < minXLambertViewable)
                continue;
            if (r.getMaxY() < maxYLambertViewable)
                continue;
            // Min
            if (r.getMinX() > maxXLambertViewable)
                continue;
            if (r.getMinY() > minYLambertViewable)
                continue;

            // final ArrayList<MapPoint> points = r.getPoints();
            // Ne pas tracers les regions minuscules

            int[] x = new int[regionPoints];
            int[] y = new int[regionPoints];
            System.arraycopy(r.absX, 0, x, 0, regionPoints);
            System.arraycopy(r.absY, 0, y, 0, regionPoints);

            regionToPoly(moveX, moveY, x, y);
            int pointCount = x.length;
            /*
             * for (int index = 0; index < pointCount; index++) { System.err.println(x[index] + ","
             * + y[index]); }
             */
            regionP += pointCount;
            if (pointCount > 3) {
                // Remplissage de la regions
                g2.setStroke(BASIC_STROKE);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

                g2.setColor(new Color(242, 240, 232));
                // g2.setColor(new Color(135 + (50 + 12 * i) % 80, 45 + (50 + 36
                // * i) % 30, 120 +
                // (50 + 35 * i) % 40));

                g2.fillPolygon(x, y, pointCount - 1);

                // Contour
                if (!this.quickdraw)
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.LIGHT_GRAY);
                g2.setStroke(STROKE_CONTOUR);
                // for (int ii = 0; ii < pointCount; ii++) {
                // g2.drawRect(x[ii], y[ii], 3, 3);
                // }
                g2.drawPolygon(x, y, pointCount - 1);
            }

            // int xx = (longitudeToX(r.getMaxX()) + longitudeToX(r.getMinX()))
            // / 2;
            // int yy = (latitudeToY(r.getMaxY()) + latitudeToY(r.getMinY())) /
            // 2;
            // g2.setColor(Color.white);
            // g2.drawString(r.getRegion().toString(), xx, yy);

        }

    }

    private void regionToPoly(int moveX, int moveY, int[] x, int[] y) {
        int stop = x.length;
        for (int j = 0; j < stop; j++) {

            // double x1 = longitudeToX(p.getX());
            x[j] += moveX;

            // double y1 = latitudeToY(p.getY());
            y[j] = moveY - y[j];

        }
    }

    public String getStatus() {
        String s = "Zoom:" + getCurrentZoomValue() + " dx:" + this.dx + ",dy" + this.dy + " Mouse:" + this.getCurrentMouseX() + "," + this.getCurrentMouseY();
        s += " Long: " + this.XToLongitude(getCurrentMouseX()) + ", " + this.YTolatitude(getCurrentMouseY());
        s += " | OffsetX: " + this.offsetX + ", " + this.offsetY;
        return s;
    }

    public int getCurrentMouseX() {
        return this.currentMouseX;
    }

    public void setCurrentMouseX(int currentMouseX) {
        this.currentMouseX = currentMouseX;
    }

    public int getCurrentMouseY() {
        return this.currentMouseY;
    }

    public void setCurrentMouseY(int currentMouseY) {
        this.currentMouseY = currentMouseY;
    }

    public void addVilleRendererListener(VilleRendererListener l) {
        this.listenersVille.add(l);
    }

    public void fireVilleRendererListener() {
        for (int i = 0; i < this.listenersVille.size(); i++) {
            this.listenersVille.get(i).selectionChanged(this);
        }
    }

    public MapPointSelection getSelectedPoints() {
        return this.selectedPoints;
    }

    public void setSelectedPoints(MapPointSelection selectedPoints) {
        this.selectedPoints.mutateTo(selectedPoints);
        // FIXME Guillaume ne bouger que si nécessaire, centrer sur le centre
        // pas sur min
        this.centerScreenXYLambert(selectedPoints.getMinX(), selectedPoints.getMinY());
        this.supp.firePropertyChange("selectedPoints", null, this.selectedPoints);
        repaint();
    }

    public void centerOn(Ville v) {
        System.out.println("Centering on:" + v);
        if (v != null)
            this.centerScreenXYLambert(v.getXLambert(), v.getYLambert());
    }

    public void setAlwayVisible(Ville v, Color color) {
        this.colors.put(v, color);
        this.setAlwayVisible(v);
    }

    public void setMatchingVille(List<Ville> v, Color c) {

        for (Ville ville : v) {
            this.alwayVisible.add(ville);
            this.colors.put(ville, c);
            incrementMacthing(ville);
        }

        rebuildVisibleVilleCache();
        repaint();
    }

    /**
     * 
     */
    public void zoomIn() {
        if (currentZoomIndex < getMaxZoomIndex()) {
            this.setZoomIndex(currentZoomIndex + 1);
        }
    }

    /**
     * 
     */
    public void zoomOut() {
        if (currentZoomIndex > 0) {
            this.setZoomIndex(currentZoomIndex - 1);
        }
    }

    public int getMaxZoomIndex() {
        return this.zoomValues.size() - 1;
    }

    public String getValue() {
        return this.getSelectedPoints().toDBString();
    }

    public void resetValue() {
        this.getSelectedPoints().clear();
    }

    public void setValue(String val) {
        this.setSelectedPoints(new MapPointSelection(val));
    }

    public void addValueListener(final PropertyChangeListener l) {
        this.supp.addPropertyChangeListener(l);
    }

    public void rmValueListener(PropertyChangeListener l) {
        this.supp.removePropertyChangeListener(l);
    }

    public void addZoomListener(ZoomListener z) {
        this.zoomListener = z;

    }
}
