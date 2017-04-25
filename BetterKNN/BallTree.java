package BetterKNN;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * @since 4/24/2017
 */
public abstract class BallTree {
   public static final int PAD = 80;

   public static void main(String[] args) {
      List<Point> data = new ArrayList<>();
      generateData(data::add, 10);
      BallTree tree = makeBallTree(data);

      JFrame f = new JFrame();
      f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      f.add(new JPanel() {
         @Override
         public void paint(Graphics g) {
            super.setBackground(Color.DARK_GRAY);
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                  RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();

            paint(g2, tree);

            data.clear();
         }

         void paint(Graphics2D g, BallTree b) {
            g.setPaint(Color.WHITE);
            if (b instanceof BallTreeLeaf) {
               BallTreeLeaf leaf = (BallTreeLeaf) b;
               g.fill(new Ellipse2D.Double(leaf.point.getX() - 4, leaf.point.getY() - 4, 4 * 2, 4 * 2));
            } else {
               BallTreeNode node = (BallTreeNode) b;
               g.draw(new Ellipse2D.Double(node.center.x - node.radius, node.center.y - node.radius, node.radius * 2, node.radius * 2));
               paint(g, node.left);
               paint(g, node.right);
            }
         }
      });
      f.setSize(800, 800);
      f.setLocation(50, 50);

      f.setVisible(true);
   }

   private static void generateData(Consumer<Point> result, int amt) {
      Random random = new Random();
      for (int i = 0; i < amt; i++) {
         result.accept(new Point((800 - PAD) * random.nextDouble() + PAD, (800 - PAD) * random.nextDouble() + PAD));
      }
   }

   public static BallTree makeBallTree(List<Point> points) {
      if (points.isEmpty()) throw new IllegalArgumentException("Point list cannot be empty.");

      boolean allSame = true;
      for (Point p : points) {
         // allSame &= p.equals(points.get(0));
         if (!p.equals(points.get(0))) {
            allSame = false;
            break; // the computer
         }
      }

      if (allSame) {
         return new BallTreeLeaf(points.get(0), points.size());
      }

      Point x = points.get(0);
      Point a = getFarthestPoint(points, x);
      Point b = getFarthestPoint(points, a);

      List<Point> closestToA = new ArrayList<>();
      List<Point> closestToB = new ArrayList<>();

      for (Point p : points) {
         if (p.distance(a) < p.distance(b)) {
            closestToA.add(p);
         } else {
            closestToB.add(p);
         }
      }

      BallTree aTree = makeBallTree(closestToA);
      BallTree bTree = makeBallTree(closestToB);

      Point midPoint = a.midPoint(b);
      double radius = midPoint.distance(getFarthestPoint(points, midPoint));

      return new BallTreeNode(aTree, bTree, midPoint, radius);
   }

   private static Point getFarthestPoint(List<Point> list, Point point) {
      Point farthest = list.get(0);
      for (Point p : list) {
         if (p.distance(point) > farthest.distance(point)) {
            farthest = p;
         }
      }
      return farthest;
   }

   private static class BallTreeLeaf extends BallTree {
      private final Point point;
      private final int n;

      public BallTreeLeaf(Point point, int n) {
         this.point = point;
         this.n = n;
      }
   }

   private static class BallTreeNode extends BallTree {
      private final BallTree left;
      private final BallTree right;
      private final Point center;
      private final double radius;

      public BallTreeNode(BallTree left, BallTree right, Point center, double radius) {
         this.left = left;
         this.right = right;
         this.center = center;
         this.radius = radius;
      }


   }

   private static class Point {
      private final double x;
      private final double y;

      public Point(double x, double y) {
         this.x = x;
         this.y = y;
      }

      public double getX() {
         return x;
      }

      public double getY() {
         return y;
      }

      public double distance(Point other) {
         return Math.hypot(x - other.x, y - other.y);
      }

      public Point midPoint(Point other) {
         return new Point((x + other.x) / 2, (y + other.y) / 2);
      }

      @Override
      public boolean equals(Object o) {
         if (this == o) return true;
         if (o == null || getClass() != o.getClass()) return false;

         Point point = (Point) o;

         if (Double.compare(point.x, x) != 0) return false;
         return Double.compare(point.y, y) == 0;
      }

      @Override
      public int hashCode() {
         int result;
         long temp;
         temp = Double.doubleToLongBits(x);
         result = (int) (temp ^ (temp >>> 32));
         temp = Double.doubleToLongBits(y);
         result = 31 * result + (int) (temp ^ (temp >>> 32));
         return result;
      }
   }

}

