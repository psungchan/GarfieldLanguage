import java.util.function.Consumer;

/**
 * Ensure that a class can find its k nearest-neighbors.
 *
 * @author GHS Programming Club
 * @since 5/8/2017
 */
public interface NearestNeighborLookup<T> {
   public void getKNearestNeighbors(int k, T point, Consumer<T> result);
}
