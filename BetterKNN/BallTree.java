import java.util.*;
import java.util.function.Consumer;

/**
 * A generic nearest-neighbor search algorithm using ball trees.
 *
 * @author GHS Programming Club
 * @since 4/24/2017
 */
public abstract class BallTree<T> implements NearestNeighborLookup<T> {

   public static interface DistanceFunction<T> {
      public double distance(T a, T b);
   }

   public static interface MidpointFunction<T> {
      public T midpoint(T a, T b);
   }

   /**
    * Find the farthest object of type T to another object of the same type from a list of objects.
    *
    * @param d     A distance function class to measure distance with.
    * @param list  The list of objects from which to find the farthest point.
    * @param point The object to compare against.
    * @param <T>   The type of object being compared.
    * @return The object of type T in list that is farthest from object point.
    */
   private static <T> T getFarthestPoint(DistanceFunction<T> d, List<T> list, T point) {
      T farthest = list.get(0);
      for (T p : list) { // super simple value updating
         if (d.distance(p, point) > d.distance(farthest, point)) {
            farthest = p;
         }
      }
      return farthest;
   }

   /**
    * Create the ball tree data structure. To be run once, then you can search for nearest neighbors using the structure.
    * Does so recursively.
    *
    * @param d      The distance function to compare objects with.
    * @param m      The function that finds the midpoint between two objects.
    * @param points The data with which to work with.
    * @param <T>    The type of object being handled.
    * @return A BallTree with the proper structure for future lookups.
    */
   private static <T> BallTree<T> makeBallTree(DistanceFunction<T> d, MidpointFunction<T> m, List<T> points) {
      if (points.isEmpty()) throw new IllegalArgumentException("List cannot be empty.");

      // Base case \\
      // return a tree leaf if all points passed in are the same
      boolean allSame = true;
      for (T p : points) {
         if (!p.equals(points.get(0))) {
            allSame = false;
            break;
         }
      }

      if (allSame) {
         return new BallTreeLeaf<>(d, points.get(0), points.size());
      }

      // Recursive case \\
      // Find the two points that are farthest from each other
      T x = points.get(0);
      T a = getFarthestPoint(d, points, x);
      T b = getFarthestPoint(d, points, a);

      // Split the data in half based on their distance to the two points
      List<T> closestToA = new ArrayList<>();
      List<T> closestToB = new ArrayList<>();

      for (T p : points) {
         if (d.distance(p, a) < d.distance(p, b)) {
            closestToA.add(p);
         } else {
            closestToB.add(p);
         }
      }

      // Recurse and make a BallTree with the newly split sets of data
      BallTree<T> aTree = makeBallTree(d, m, closestToA);
      BallTree<T> bTree = makeBallTree(d, m, closestToB);

      // "Draw" a ball around the data so that you can do lookup later
      T midPoint = m.midpoint(a, b);
      double radius = d.distance(midPoint, getFarthestPoint(d, points, midPoint));

      return new BallTreeNode<>(d, aTree, bTree, midPoint, radius);
   }

   /**
    * Fill a Consumer with data.
    *
    * @param result The Consumer that will accept the data.
    * @param amt    The amount of data to put in the Consumer.
    */
   private static void generateData(Consumer<Point> result, int amt) {
      Random random = new Random();
      for (int i = 0; i < amt; i++) {
         result.accept(new Point(random.nextDouble(), random.nextDouble()));
      }
   }

   protected abstract double getCircleClosestDistance(T point);

   protected abstract double getCircleFarthestDistance(T point);

   protected abstract void expand(Consumer<BallTree<T>> children, Consumer<T> result, int k);

   /**
    * The data structure for storing single nodes, or multiple nodes that are the exact same.
    *
    * @param <T> The type of object.
    */
   private static class BallTreeLeaf<T> extends BallTree<T> {
      private final DistanceFunction<T> d;
      private final T point;
      private final int n;

      public BallTreeLeaf(DistanceFunction<T> d, T point, int n) {
         this.d = d;
         this.point = point;
         this.n = n;
      }

      @Override
      protected double getCircleClosestDistance(T other) {
         return d.distance(other, point);
      }

      @Override
      protected double getCircleFarthestDistance(T other) {
         return d.distance(other, point);
      }

      @Override
      protected void expand(Consumer<BallTree<T>> children, Consumer<T> result, int k) {
         getKNearestNeighbors(k, null, result);
      }

      @Override
      public void getKNearestNeighbors(int k, T point, Consumer<T> result) {
         for (int i = 0; i < Math.min(k, n); i++) { // Math.min(k, n) to ensure that we don't send more points than we have.
            result.accept(this.point);
         }
      }
   }

   /**
    * The data structure for storing multiple "leaves". Basically the circles when using 2D points.
    *
    * @param <T> The type of Object.
    */
   private static class BallTreeNode<T> extends BallTree<T> {
      private final DistanceFunction<T> d;
      private final BallTree<T> left;
      private final BallTree<T> right;
      private final T center;
      private final double radius;

      public BallTreeNode(DistanceFunction<T> d, BallTree<T> left, BallTree<T> right, T center, double radius) {
         this.d = d;
         this.left = left;
         this.right = right;
         this.center = center;
         this.radius = radius;
      }

      // return closest point on circle to pt other
      @Override
      protected double getCircleClosestDistance(T other) {
         return Math.max(0f, d.distance(other, center) - this.radius);
      }

      // return farthest point on circle to pt other
      @Override
      protected double getCircleFarthestDistance(T other) {
         return d.distance(other, center) + this.radius;
      }

      @Override
      protected void expand(Consumer<BallTree<T>> children, Consumer<T> result, int k) {
         children.accept(left);
         children.accept(right);
      }

      private static class MutableInteger {
         int x;
      }

      @Override
      public void getKNearestNeighbors(int k, T point, Consumer<T> result) {
         MutableInteger count = new MutableInteger();
         Consumer<T> consumer = x -> { // Keep track of how many neighbors we've passed in
            count.x++;
            result.accept(x);
         };
         Queue<BallTree<T>> queue = new PriorityQueue<>(16, Comparator.comparingDouble(a -> a.getCircleClosestDistance(point)));
         queue.add(this); // Add the parent ball to the queue
         while (count.x < k && queue.size() > 0) {
            BallTree<T> head = queue.poll();
            head.expand(queue::add, consumer, k - count.x); // Add on all children of head
         }
      }
   }

   public static class Point {
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
      public String toString() {
         return String.format("<%f, %f>", x, y);
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

   public static void main(String[] args) {
      List<Point> data = new ArrayList<>();
      generateData(data::add, 50);

      NearestNeighborLookup<Point> tree = makeBallTree(Point::distance, Point::midPoint, data);
      List<Point> result = new ArrayList<>();
      Point goal = new Point(0.5, 0.5);
      tree.getKNearestNeighbors(5, goal, result::add);
      result.forEach(System.out::println);

      // Double check algorithm with brute force
      System.out.println("\nreal");
      data.sort(Comparator.comparing(goal::distance));
      for (int i = 0; i < 5; i++) {
         System.out.println(data.get(i));
      }
   }
}

