import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * @since 4/24/2017
 */
public abstract class BallTree {
   public static final int PAD = 80;

   protected abstract double getCircleClosestDistance(Point point);

   protected abstract double getCircleFarthestDistance(Point point);

   protected abstract void expand(Consumer<BallTree> children, Consumer<Point> result, int k);

   public static interface NearestNeighborLookup {
      public void getKNearestNeighbors(int k, Point point, List<Point> neighbors);
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

      protected double getCircleClosestDistance(Point other) {
         return other.distance(this.point);
      }

      protected double getCircleFarthestDistance(Point other) {
         return other.distance(this.point);
      }

      protected void expand(Consumer<BallTree> children, Consumer<Point> result, int k){
         for (int i = 0; i < Math.min(k,n); i++) {
            result.accept(this.point);
         }
      }
   }

   private static class BallTreeNode extends BallTree implements NearestNeighborLookup {
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

      // return closest point on circle to pt other
      protected double getCircleClosestDistance(Point other) {
         return Math.max(0f, other.distance(this.center) - this.radius);
      }

      // return farthest point on circle to pt other
      protected double getCircleFarthestDistance(Point other) {
         return other.distance(this.center) + this.radius;
      }

      protected void expand(Consumer<BallTree> children, Consumer<Point> result, int k){
         children.accept(left);
         children.accept(right);
      }

      public void getKNearestNeighbors(int k, Point point, List<Point> neighbors) {
         Queue<BallTree> queue = new PriorityQueue<>(16, Comparator.comparingDouble(a -> a.getCircleClosestDistance(point)));
         queue.add(this); // add the parent ball to the queue
         while(neighbors.size() < k && queue.size() > 0){
            BallTree head = queue.poll();
            head.expand(queue::add, neighbors::add, k - neighbors.size()); // add on all children of head
         }
      }
   }

   public static NearestNeighborLookup makeBallTreeNearestNeighborLookup(List<Point> points) {
      return (NearestNeighborLookup) makeBallTree(points);
   }

   private static BallTree makeBallTree(List<Point> points) {
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
      public String toString() { return String.format("<%f, %f>", x, y); }

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

   public static void main(String[] args) {
      List<Point> data = new ArrayList<>();
      generateData(data::add, 1000);

      NearestNeighborLookup tree = makeBallTreeNearestNeighborLookup(data);

      List<Point> result = new ArrayList<>();

      tree.getKNearestNeighbors(5, new Point(0.69, 0.96), result);

      result.forEach(System.out::println);

      /* JFrame f = new JFrame();
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

      f.setVisible(true); */
   }

   private static void generateData(Consumer<Point> result, int amt) {
      Random random = new Random();
      for (int i = 0; i < amt; i++) {
         result.accept(new Point(random.nextDouble(), random.nextDouble()));
         //result.accept(new Point((800 - PAD) * random.nextDouble() + PAD, (800 - PAD) * random.nextDouble() + PAD));
      }
   }

}

