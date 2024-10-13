package deepy.clone;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * A generic object pool for managing reusable object instances.
 *
 * @param <T> The type of objects in the pool.
 */
public class ObjectPool<T> {

    /**
     * A thread-safe queue to hold pooled objects.
     */
    private final ConcurrentLinkedQueue<T> pool;

    /**
     * A supplier for creating new instances of the object type.
     */
    private final Supplier<T> supplier;

    /**
     * Constructs a new ObjectPool with a specified supplier for object creation.
     *
     * @param supplier A Supplier<T> that provides new instances of type T.
     */
    public ObjectPool(Supplier<T> supplier) {
        this.pool = new ConcurrentLinkedQueue<>();
        this.supplier = supplier;
    }

    /**
     * Acquires an object from the pool. If no objects are available,
     * a new one is created using the supplier.
     *
     * @return An instance of type T from the pool.
     */
    public T acquire() {
        T obj = pool.poll();
        return (obj != null) ? obj : supplier.get();
    }

    /**
     * Returns an object to the pool for reuse.
     *
     * @param obj The object to be returned to the pool.
     */
    public void release(T obj) {
        pool.offer(obj);
    }
}
