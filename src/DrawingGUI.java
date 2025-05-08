import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.io.IOException;

public class DrawingGUI {
    private JFrame frame;
    private JPanel drawingPanel;
    private JButton lineButton, circleButton, rectButton, clearButton;
    private String currentTool = "LINE";
    private final List<Shape> permanentShapes = new ArrayList<>();
    private Point startPoint;
    private Shape previewShape;
    private ClientNetworkHandler networkHandler;

    public DrawingGUI(int port) {
        try {
            networkHandler = new ClientNetworkHandler(port);
            createGUI();
        } catch (SocketException e) {
            showErrorAndExit("Network Error", "Could not create network connection: " + e.getMessage());
        } catch (UnknownHostException e) {
            showErrorAndExit("Connection Error", "Could not find server: " + e.getMessage());
        }
    }

    private void showErrorAndExit(String title, String message) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
        System.exit(1);
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

                g2d.setColor(Color.BLACK);
                for (Shape shape : permanentShapes) {
                    g2d.draw(shape);
                }

                if (previewShape != null) {
                    g2d.setColor(new Color(0, 0, 255, 128));
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

        // Network listener thread
        new Thread(() -> {
            while (true) {
                try {
                    String command = networkHandler.receiveCommand();
                    if (command != null) {
                        processNetworkCommand(command);
                        SwingUtilities.invokeLater(drawingPanel::repaint);
                    }
                } catch (IOException e) {
                    System.err.println("Error receiving network data: " + e.getMessage());
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
                int width = currentPoint.x - startPoint.x;
                int height = currentPoint.y - startPoint.y;
                if ((getKeyModifiers() & InputEvent.SHIFT_DOWN_MASK) != 0) {
                    int size = Math.max(Math.abs(width), Math.abs(height));
                    width = width < 0 ? -size : size;
                    height = height < 0 ? -size : size;
                }
                previewShape = new Rectangle2D.Double(
                    startPoint.x, startPoint.y, width, height);
                break;
        }
    }

    private int getKeyModifiers() {
        return Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx();
    }

    private void sendShapeToServer(Shape shape) {
        try {
            String command = shapeToCommand(shape);
            if (!command.isEmpty()) {
                networkHandler.sendCommand(command);
            }
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> 
                JOptionPane.showMessageDialog(frame, 
                    "Failed to send shape: " + e.getMessage(),
                    "Network Error", 
                    JOptionPane.ERROR_MESSAGE));
        }
    }

    private String shapeToCommand(Shape shape) {
        if (shape instanceof Line2D) {
            Line2D line = (Line2D) shape;
            return String.format("LINE:%d,%d,%d,%d", 
                (int) line.getX1(), (int) line.getY1(),
                (int) line.getX2(), (int) line.getY2());
        } else if (shape instanceof Ellipse2D) {
            Ellipse2D circle = (Ellipse2D) shape;
            int radius = (int) (circle.getWidth() / 2);
            return String.format("CIRCLE:%d,%d,%d",
                (int) (circle.getX() + radius), 
                (int) (circle.getY() + radius),
                radius);
        } else if (shape instanceof Rectangle2D) {
            Rectangle2D rect = (Rectangle2D) shape;
            return String.format("RECT:%d,%d,%d,%d",
                (int) rect.getX(), (int) rect.getY(),
                (int) rect.getWidth(), (int) rect.getHeight());
        }
        return "";
    }

    private void clearBoard() {
        permanentShapes.clear();
        try {
            networkHandler.sendCommand("CLEAR");
        } catch (IOException e) {
            SwingUtilities.invokeLater(() -> 
                JOptionPane.showMessageDialog(frame, 
                    "Failed to clear board: " + e.getMessage(),
                    "Network Error", 
                    JOptionPane.ERROR_MESSAGE));
        }
        drawingPanel.repaint();
    }

    private void processNetworkCommand(String command) {
        if (command.equals("CLEAR")) {
            permanentShapes.clear();
            return;
        }

        String[] parts = command.split(":");
        if (parts.length < 2) return;

        String[] coords = parts[1].split(",");
        Shape shape = null;

        try {
            switch (parts[0]) {
                case "LINE":
                    if (coords.length == 4) {
                        shape = new Line2D.Double(
                            Integer.parseInt(coords[0]), Integer.parseInt(coords[1]),
                            Integer.parseInt(coords[2]), Integer.parseInt(coords[3]));
                    }
                    break;
                case "CIRCLE":
                    if (coords.length == 3) {
                        int radius = Integer.parseInt(coords[2]);
                        shape = new Ellipse2D.Double(
                            Integer.parseInt(coords[0]) - radius,
                            Integer.parseInt(coords[1]) - radius,
                            radius * 2,
                            radius * 2);
                    }
                    break;
                case "RECT":
                    if (coords.length == 4) {
                        shape = new Rectangle2D.Double(
                            Integer.parseInt(coords[0]), Integer.parseInt(coords[1]),
                            Integer.parseInt(coords[2]), Integer.parseInt(coords[3]));
                    }
                    break;
            }
        } catch (NumberFormatException e) {
            System.err.println("Invalid coordinates in command: " + command);
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