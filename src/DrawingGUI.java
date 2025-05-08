import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;

public class DrawingGUI {
    private JFrame frame;
    private JPanel drawingPanel;
    private JButton lineButton, circleButton, rectButton, clearButton;
    private String currentTool = "LINE";
    private List<Shape> shapes = new ArrayList<>();
    private List<Shape> permanentShapes = new ArrayList<>();
    private Point startPoint;
    private Shape previewShape;
    private ClientNetworkHandler networkHandler;

    public DrawingGUI(int port) {
        networkHandler = new ClientNetworkHandler(port);
        createGUI();
    }

    private void createGUI() {
        frame = new JFrame("Network Drawing Board");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        // Toolbar
        JPanel toolbar = new JPanel();
        lineButton = new JButton("Line");
        circleButton = new JButton("Circle");
        rectButton = new JButton("Rectangle");
        clearButton = new JButton("Clear Board");

        lineButton.addActionListener(e -> currentTool = "LINE");
        circleButton.addActionListener(e -> currentTool = "CIRCLE");
        rectButton.addActionListener(e -> currentTool = "RECT");
        clearButton.addActionListener(e -> clearBoard());

        toolbar.add(lineButton);
        toolbar.add(circleButton);
        toolbar.add(rectButton);
        toolbar.add(clearButton);
        frame.add(toolbar, BorderLayout.NORTH);

        // Drawing Panel
        drawingPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Draw permanent shapes
                g2d.setColor(Color.BLACK);
                for (Shape shape : permanentShapes) {
                    g2d.draw(shape);
                }

                // Draw preview shape
                if (previewShape != null) {
                    g2d.setColor(new Color(0, 0, 255, 128)); // Semi-transparent blue
                    g2d.draw(previewShape);
                }
            }
        };

        drawingPanel.setBackground(Color.WHITE);
        drawingPanel.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                startPoint = e.getPoint();
            }

            public void mouseReleased(MouseEvent e) {
                if (previewShape != null) {
                    permanentShapes.add(previewShape);
                    sendShapeToServer(previewShape);
                    previewShape = null;
                    drawingPanel.repaint();
                }
            }
        });

        drawingPanel.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                updatePreviewShape(e.getPoint());
                drawingPanel.repaint();
            }
        });

        frame.add(drawingPanel, BorderLayout.CENTER);
        frame.setVisible(true);

        // Start listening for network updates
        new Thread(() -> {
            while (true) {
                String command = networkHandler.receiveCommand();
                if (command != null) {
                    processNetworkCommand(command);
                    drawingPanel.repaint();
                }
            }
        }).start();
    }

    private void updatePreviewShape(Point currentPoint) {
        switch (currentTool) {
            case "LINE":
                previewShape = new Line2D.Double(startPoint, currentPoint);
                break;
            case "CIRCLE":
                int radius = (int) startPoint.distance(currentPoint);
                previewShape = new Ellipse2D.Double(
                    startPoint.x - radius, startPoint.y - radius, 
                    radius * 2, radius * 2);
                break;
            case "RECT":
                previewShape = new Rectangle2D.Double(
                    startPoint.x, startPoint.y,
                    currentPoint.x - startPoint.x, 
                    currentPoint.y - startPoint.y);
                break;
        }
    }

    private void sendShapeToServer(Shape shape) {
        String command = "";
        if (shape instanceof Line2D) {
            Line2D line = (Line2D) shape;
            command = String.format("LINE:%d,%d,%d,%d", 
                (int) line.getX1(), (int) line.getY1(),
                (int) line.getX2(), (int) line.getY2());
        } else if (shape instanceof Ellipse2D) {
            Ellipse2D circle = (Ellipse2D) shape;
            int radius = (int) (circle.getWidth() / 2);
            command = String.format("CIRCLE:%d,%d,%d",
                (int) (circle.getX() + radius), 
                (int) (circle.getY() + radius),
                radius);
        } else if (shape instanceof Rectangle2D) {
            Rectangle2D rect = (Rectangle2D) shape;
            command = String.format("RECT:%d,%d,%d,%d",
                (int) rect.getX(), (int) rect.getY(),
                (int) rect.getWidth(), (int) rect.getHeight());
        }
        networkHandler.sendCommand(command);
    }

    private void clearBoard() {
        permanentShapes.clear();
        networkHandler.sendCommand("CLEAR");
        drawingPanel.repaint();
    }

    private void processNetworkCommand(String command) {
        if (command.equals("CLEAR")) {
            permanentShapes.clear();
            return;
        }

        String[] parts = command.split(":");
        String[] coords = parts[1].split(",");
        Shape shape = null;

        switch (parts[0]) {
            case "LINE":
                shape = new Line2D.Double(
                    Integer.parseInt(coords[0]), Integer.parseInt(coords[1]),
                    Integer.parseInt(coords[2]), Integer.parseInt(coords[3]));
                break;
            case "CIRCLE":
                shape = new Ellipse2D.Double(
                    Integer.parseInt(coords[0]) - Integer.parseInt(coords[2]),
                    Integer.parseInt(coords[1]) - Integer.parseInt(coords[2]),
                    Integer.parseInt(coords[2]) * 2,
                    Integer.parseInt(coords[2]) * 2);
                break;
            case "RECT":
                shape = new Rectangle2D.Double(
                    Integer.parseInt(coords[0]), Integer.parseInt(coords[1]),
                    Integer.parseInt(coords[2]), Integer.parseInt(coords[3]));
                break;
        }

        if (shape != null) {
            permanentShapes.add(shape);
        }
    }

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 0;
        SwingUtilities.invokeLater(() -> new DrawingGUI(port));
    }
}