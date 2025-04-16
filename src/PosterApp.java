import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

public class PosterApp extends JFrame {
    CanvasPanel canvasPanel;
    ImagePanel imagePanel;
    ShapePanel shapePanel;
    ButtonPanel buttonPanel;

    public PosterApp() {
        setTitle("Poster app");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);

        setLayout(new BorderLayout());

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(300, 600));

        imagePanel = new ImagePanel(this);
        shapePanel = new ShapePanel(this);

        leftPanel.add(imagePanel, BorderLayout.CENTER);
        leftPanel.add(shapePanel, BorderLayout.SOUTH);

        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        canvasPanel = new CanvasPanel(this);
        buttonPanel = new ButtonPanel(this);
        rightPanel.add(canvasPanel, BorderLayout.CENTER);
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.CENTER);

        setVisible(true);
    }

    void updateButtonPanel(boolean isSelected) {
        buttonPanel.updateButtonStates(isSelected);
    }

    public static void main(String[] args) {
        PosterApp app = new PosterApp();
        while (true) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                System.out.println("Program interrupted");
            }
            app.repaint();
        }
    }
}



class CanvasPanel extends JPanel {
    PosterApp posterApp;
    ArrayList<Drawable> drawableObjects;
    Drawable selected;

    Point lastMouse;
    enum DragMode { NONE, MOVE, RESIZE, ROTATE }
    DragMode dragMode = DragMode.NONE;

    Rectangle resizeHandle;
    Ellipse2D rotateHandle;
    int activeHandle = -1;

    public CanvasPanel(PosterApp posterApp) {
        this.posterApp = posterApp;
        drawableObjects = new ArrayList<>();
        setBackground(Color.WHITE);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                lastMouse = e.getPoint();
                activeHandle = -1;

                if (selected != null) {
                    updateHandles();
                    if (resizeHandle != null && resizeHandle.contains(lastMouse)) {
                        dragMode = DragMode.RESIZE;
                        return;
                    }
                    if (rotateHandle != null && rotateHandle.contains(lastMouse)) {
                        dragMode = DragMode.ROTATE;
                        return;
                    }
                }

                selected = null;
                posterApp.updateButtonPanel(false);
                for (Drawable drawable : drawableObjects) {
                    if(drawable.contains(lastMouse)) {
                        selected = drawable;
                        dragMode = DragMode.MOVE;
                        updateHandles();
                        posterApp.updateButtonPanel(true);
                    }
                }

                if(e.getButton() == MouseEvent.BUTTON3 && selected != null) {
                    drawableObjects.remove(selected);
                    selected = null;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                dragMode = DragMode.NONE;
                activeHandle = -1;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                if (selected != null && dragMode == DragMode.MOVE) {
                    int dx = e.getX() - lastMouse.x;
                    int dy = e.getY() - lastMouse.y;
                    selected.translate(dx, dy);
                    lastMouse = e.getPoint();
                    updateHandles();
                    repaint();
                } else if (selected != null && dragMode == DragMode.RESIZE) {
                    Rectangle bounds = selected.getBounds();
                    Point2D anchor = new Point2D.Double(bounds.x, bounds.y);

                    double newWidth = e.getX() - anchor.getX();
                    double newHeight = e.getY() - anchor.getY();

                    double sx = newWidth / bounds.width;
                    double sy = newHeight / bounds.height;

                    if (sx > 0 && sy > 0) {
                        selected.scale(sx, sy, anchor.getX(), anchor.getY());
                        lastMouse = e.getPoint();
                        updateHandles();
                        repaint();
                    }
                } else if (selected != null && dragMode == DragMode.ROTATE) {
                    Point2D anchor = selected.getCenter();
                    double angle = Math.atan2(e.getY() - anchor.getY(), e.getX() - anchor.getX()) -
                            Math.atan2(lastMouse.getY() - anchor.getY(), lastMouse.getX() - anchor.getX());
                    selected.rotate(angle, anchor.getX(), anchor.getY());
                    lastMouse = e.getPoint();
                    updateHandles();
                    repaint();
                }
            }
        });
    }

    private void updateHandles() {
        if (selected == null) return;
        Rectangle bounds = selected.getBounds();
        int size = 8;
        resizeHandle = new Rectangle(bounds.x + bounds.width - size / 2, bounds.y + bounds.height - size / 2, size, size);

        int offset = 30;
        rotateHandle = new Ellipse2D.Double(bounds.getCenterX() - size / 2, bounds.y - offset - size / 2, size, size);
    }

    public void addDrawable(Drawable drawable) {
        drawableObjects.add(drawable);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        for (Drawable drawable : drawableObjects) {
            drawable.Draw(g2d);
        }

        if (selected != null) {
            updateHandles();
            if (resizeHandle != null) {
                g2d.setColor(Color.BLUE);
                g2d.fill(resizeHandle);
            }
            if (rotateHandle != null) {
                g2d.setColor(Color.ORANGE);
                g2d.fill(rotateHandle);
            }
            Rectangle bounds = selected.getBounds();
            g2d.setColor(Color.RED);
            g2d.drawRect(bounds.x - 5, bounds.y - 5, bounds.width + 10, bounds.height + 10);

            g2d.setColor(Color.GRAY);
            int cx = bounds.x + bounds.width / 2;
            int cy = bounds.y + bounds.height / 2;
            g2d.fillOval(cx - 6, cy - 6, 12, 12);
        }
    }
}

class ImagePanel extends JPanel {
    PosterApp posterApp;
    ArrayList<BufferedImage> images = new ArrayList<>();
    BufferedImage draggingImage = null;
    Point dragOffset = null;

    public ImagePanel(PosterApp posterApp) {
        this.posterApp = posterApp;
        setLayout(new BorderLayout());

        JPanel gridPanel = new JPanel(new GridLayout(0, 3, 5, 5));
        JScrollPane scrollPane = new JScrollPane(gridPanel);
        add(scrollPane, BorderLayout.CENTER);

        File imageDir = new File("images");
        if (imageDir.exists() && imageDir.isDirectory()) {
            for (File file : imageDir.listFiles()) {
                try {
                    BufferedImage img = ImageIO.read(file);
                    if (img != null) {
                        images.add(img);

                        JLabel label = new JLabel(new ImageIcon(img.getScaledInstance(80, 80, Image.SCALE_SMOOTH)));
                        gridPanel.add(label);

                        label.addMouseListener(new MouseAdapter() {
                            @Override
                            public void mousePressed(MouseEvent e) {
                                draggingImage = img;
                                dragOffset = e.getPoint();
                            }

                            @Override
                            public void mouseReleased(MouseEvent e) {
                                if (draggingImage != null) {
                                    Point canvasLocation = posterApp.canvasPanel.getLocationOnScreen();
                                    Dimension canvasSize = posterApp.canvasPanel.getSize();

                                    Rectangle canvasBounds = new Rectangle(canvasLocation, canvasSize);
                                    Point releasePoint = new Point(e.getXOnScreen(), e.getYOnScreen());

                                    if (canvasBounds.contains(releasePoint)) {
                                        int x = releasePoint.x - canvasLocation.x - dragOffset.x;
                                        int y = releasePoint.y - canvasLocation.y - dragOffset.y;

                                        ImageDrawable drawable = new ImageDrawable(draggingImage, x, y);
                                        posterApp.canvasPanel.addDrawable(drawable);
                                    }

                                    draggingImage = null;
                                }
                            }
                        });
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}

class ShapePanel extends JPanel {
    PosterApp posterApp;
    Point dragOffset = null;
    Shape draggingShape = null;
    ColorPanel colorPanel;

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(0, 150);
    }

    public ShapePanel(PosterApp posterApp) {
        this.posterApp = posterApp;
        setLayout(new BorderLayout());

        JPanel shapeContainer = new JPanel(null);
        shapeContainer.setPreferredSize(new Dimension(0, 80));

        JPanel circlePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(colorPanel.getSelectedColor());
                g2d.fillOval(5, 5, 40, 40);
            }
        };
        circlePanel.setBounds(20, 10, 50, 50);
        shapeContainer.add(circlePanel);

        JPanel squarePanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setColor(colorPanel.getSelectedColor());
                g2d.fillRect(5, 5, 40, 40);
            }
        };
        squarePanel.setBounds(100, 10, 50, 50);
        shapeContainer.add(squarePanel);

        add(shapeContainer, BorderLayout.CENTER);

        colorPanel = new ColorPanel();
        add(colorPanel, BorderLayout.SOUTH);

        MouseAdapter shapeDragHandler = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Component comp = e.getComponent();
                dragOffset = e.getPoint();
                if (comp == circlePanel) {
                    draggingShape = new Ellipse2D.Double(e.getXOnScreen() - dragOffset.x, e.getYOnScreen() - dragOffset.y, 50, 50);
                } else if (comp == squarePanel) {
                    draggingShape = new Rectangle2D.Double(e.getXOnScreen() - dragOffset.x, e.getYOnScreen() - dragOffset.y, 50, 50);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (draggingShape != null) {
                    Point canvasLocation = posterApp.canvasPanel.getLocationOnScreen();
                    Dimension canvasSize = posterApp.canvasPanel.getSize();

                    Rectangle canvasBounds = new Rectangle(canvasLocation, canvasSize);
                    Point releasePoint = new Point(e.getXOnScreen(), e.getYOnScreen());

                    if (canvasBounds.contains(releasePoint)) {
                        int x = releasePoint.x - canvasLocation.x - dragOffset.x;
                        int y = releasePoint.y - canvasLocation.y - dragOffset.y;

                        Shape shape = null;
                        if (draggingShape instanceof Ellipse2D) {
                            shape = new Ellipse2D.Double(x, y, 50, 50);
                        } else if (draggingShape instanceof Rectangle2D) {
                            shape = new Rectangle2D.Double(x, y, 50, 50);
                        }

                        if (shape != null) {
                            posterApp.canvasPanel.addDrawable(new ShapeDrawable(shape, colorPanel.getSelectedColor()));
                        }
                    }

                    draggingShape = null;
                }
            }
        };

        circlePanel.addMouseListener(shapeDragHandler);
        circlePanel.addMouseMotionListener(shapeDragHandler);
        squarePanel.addMouseListener(shapeDragHandler);
        squarePanel.addMouseMotionListener(shapeDragHandler);
    }
}

class ColorPanel extends JPanel {
    private final JTextField redField;
    private final JTextField greenField;
    private final JTextField blueField;

    public ColorPanel() {
        setLayout(new GridLayout(3, 2));
        add(new JLabel("Red:"));
        redField = new JTextField("0");
        add(redField);

        add(new JLabel("Green:"));
        greenField = new JTextField("0");
        add(greenField);

        add(new JLabel("Blue:"));
        blueField = new JTextField("0");
        add(blueField);
    }

    public Color getSelectedColor() {
        try {
            int r = Math.min(255, Math.max(0, Integer.parseInt(redField.getText())));
            int g = Math.min(255, Math.max(0, Integer.parseInt(greenField.getText())));
            int b = Math.min(255, Math.max(0, Integer.parseInt(blueField.getText())));
            return new Color(r, g, b);
        } catch (NumberFormatException e) {
            return Color.BLACK;
        }
    }
}

class ButtonPanel extends JPanel {
    PosterApp posterApp;

    JButton upButton, downButton, leftButton, rightButton;
    JButton rotateCWButton, rotateCCWButton;
    JButton layerUpButton, layerDownButton;

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(0, 50);
    }

    public ButtonPanel(PosterApp posterApp) {
        this.posterApp = posterApp;
        setLayout(new GridLayout(1, 8));

        leftButton = new JButton("←");
        upButton = new JButton("↑");
        downButton = new JButton("↓");
        rightButton = new JButton("→");

        rotateCCWButton = new JButton("⟲");
        rotateCWButton = new JButton("⟳");

        layerUpButton = new JButton("⇧");
        layerDownButton = new JButton("⇩");

        add(leftButton);
        add(upButton);
        add(downButton);
        add(rightButton);
        add(rotateCCWButton);
        add(rotateCWButton);
        add(layerUpButton);
        add(layerDownButton);

        leftButton.addActionListener(e -> moveSelected(-1, 0));
        rightButton.addActionListener(e -> moveSelected(1, 0));
        upButton.addActionListener(e -> moveSelected(0, -1));
        downButton.addActionListener(e -> moveSelected(0, 1));

        rotateCWButton.addActionListener(e -> rotateSelected(Math.toRadians(1)));
        rotateCCWButton.addActionListener(e -> rotateSelected(-Math.toRadians(1)));

        layerUpButton.addActionListener(e -> changeLayer(1));
        layerDownButton.addActionListener(e -> changeLayer(-1));
    }

    void updateButtonStates(boolean isEnabled) {
        leftButton.setEnabled(isEnabled);
        rightButton.setEnabled(isEnabled);
        upButton.setEnabled(isEnabled);
        downButton.setEnabled(isEnabled);
        rotateCWButton.setEnabled(isEnabled);
        rotateCCWButton.setEnabled(isEnabled);
        layerUpButton.setEnabled(isEnabled);
        layerDownButton.setEnabled(isEnabled);
    }

    private void moveSelected(int dx, int dy) {
        Drawable selected = posterApp.canvasPanel.selected;
        if (selected != null) {
            selected.translate(dx, dy);
        }
    }

    private void rotateSelected(double radians) {
        Drawable selected = posterApp.canvasPanel.selected;
        if (selected != null) {
            Point2D center = selected.getCenter();
            selected.rotate(radians, center.getX(), center.getY());
        }
    }

    private void changeLayer(int direction) {
        CanvasPanel canvas = posterApp.canvasPanel;
        Drawable selected = canvas.selected;
        if (selected != null) {
            int index = canvas.drawableObjects.indexOf(selected);
            int newIndex = index + direction;

            if (newIndex >= 0 && newIndex < canvas.drawableObjects.size()) {
                canvas.drawableObjects.remove(index);
                canvas.drawableObjects.add(newIndex, selected);
            }
        }
    }
}