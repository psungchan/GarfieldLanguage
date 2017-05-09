import java.util.function.Consumer;

/**
 * @since 5/8/2017
 */
public interface NearestNeighborLookup<T> {
   public void getKNearestNeighbors(int k, T point, Consumer<T> result);
}
