package deepy.clone;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Utility class for deep copying objects and collections, including support for cyclic references.
 * This class also includes a pooling mechanism for optimizing performance by reusing
 * commonly used collections like ArrayList, HashSet, and HashMap.
 */
public class CopyUtils {

    private static final Map<Class<?>, Object> DEFAULT_VALUES = new HashMap<>();

    static {
        DEFAULT_VALUES.put(boolean.class, false);
        DEFAULT_VALUES.put(char.class, '\u0000');
        DEFAULT_VALUES.put(byte.class, (byte) 0);
        DEFAULT_VALUES.put(short.class, (short) 0);
        DEFAULT_VALUES.put(int.class, 0);
        DEFAULT_VALUES.put(long.class, 0L);
        DEFAULT_VALUES.put(float.class, 0.0f);
        DEFAULT_VALUES.put(double.class, 0.0);
    }

    private static final ObjectPool<ArrayList<Object>> arrayListPool = new ObjectPool<>(ArrayList::new);
    private static final ObjectPool<HashSet<Object>> hashSetPool = new ObjectPool<>(HashSet::new);
    private static final ObjectPool<HashMap<Object, Object>> hashMapPool = new ObjectPool<>(HashMap::new);

    /**
     * Performs a deep copy of the given object, handling collections, maps, and arrays,
     * while preserving references to avoid cyclic dependencies.
     *
     * @param <T> The type of the object being copied.
     * @param obj The object to deep copy.
     * @return A deep copy of the input object, or null if the input is null.
     * @throws RuntimeException if an error occurs during the deep copy process.
     */
    public static <T> T deepCopy(final T obj) {
        if (obj == null) {
            return null;
        }

        final Map<Object, Object> visited = new IdentityHashMap<>();
        try {
            return (T) deepCopyInternal(obj, visited);
        } catch (Exception e) {
            throw new RuntimeException("Error during deep copy", e);
        }
    }

    /**
     * Internal method to perform the actual deep copy logic, handling arrays, collections, and maps.
     *
     * @param obj     The object to be copied.
     * @param visited A map tracking visited objects to handle cyclic references.
     * @return A deep copy of the input object.
     */
    private static Object deepCopyInternal(final Object obj, final Map<Object, Object> visited) {
        if (obj == null || isImmutable(obj)) {
            return obj;
        }

        if (visited.containsKey(obj)) {
            return visited.get(obj);
        }

        final Class<?> clazz = obj.getClass();

        if (clazz.isArray()) {
            return copyArray(obj, visited, clazz);
        }

        if (Collection.class.isAssignableFrom(clazz)) {
            return copyCollection(obj, visited);
        }

        if (Map.class.isAssignableFrom(clazz)) {
            return copyMap(obj, visited);
        }

        final Object newObj = createNewInstance(clazz);
        visited.put(obj, newObj);

        copyFields(obj, newObj, visited, clazz);

        return newObj;
    }

    /**
     * Deep copies an array, preserving its length and component type.
     *
     * @param obj     The array to copy.
     * @param visited A map tracking visited objects to handle cyclic references.
     * @param clazz   The class of the array.
     * @return A deep copy of the array.
     */
    private static Object copyArray(final Object obj, final Map<Object, Object> visited, final Class<?> clazz) {
        int length = Array.getLength(obj);
        Object newArray = Array.newInstance(clazz.getComponentType(), length);
        visited.put(obj, newArray);  // Track before recursion
        for (int i = 0; i < length; i++) {
            try {
                Array.set(newArray, i, deepCopyInternal(Array.get(obj, i), visited));
            } catch (Exception e) {
                throw new RuntimeException("Error copying array element at index " + i, e);
            }
        }
        return newArray;
    }

    /**
     * Determines if an object is immutable (e.g., String, primitive wrappers, etc.).
     *
     * @param obj The object to check.
     * @return true if the object is immutable, false otherwise.
     */
    private static boolean isImmutable(final Object obj) {
        final Class<?> clazz = obj.getClass();
        return clazz.isPrimitive() ||
                clazz.isEnum() ||
                clazz.equals(String.class) ||
                Number.class.isAssignableFrom(clazz) ||
                Boolean.class.isAssignableFrom(clazz) ||
                Character.class.isAssignableFrom(clazz);
    }

    /**
     * Deep copies a collection, handling both List and Set types using pooling where applicable.
     *
     * @param obj     The collection to copy.
     * @param visited A map tracking visited objects to handle cyclic references.
     * @return A deep copy of the collection.
     */
    private static Object copyCollection(final Object obj, final Map<Object, Object> visited) {
        final Collection<?> original = (Collection<?>) obj;
        Collection<Object> copy;

        try {
            if (original.getClass().isAssignableFrom(ArrayList.class)) {
                copy = arrayListPool.acquire();
            } else if (original.getClass().isAssignableFrom(HashSet.class)) {
                copy = hashSetPool.acquire();
            } else {
                copy = createNewCollectionInstance(original.getClass());
            }

            visited.put(obj, copy);
            for (Object element : original) {
                try {
                    copy.add(deepCopyInternal(element, visited));
                } catch (Exception e) {
                    throw new RuntimeException("Error copying collection element", e);
                }
            }
            return copy;
        } catch (Exception e) {
            throw new RuntimeException("Error during collection copy", e);
        }
    }

    /**
     * Deep copies a map, handling both key and value objects using pooling where applicable.
     *
     * @param obj     The map to copy.
     * @param visited A map tracking visited objects to handle cyclic references.
     * @return A deep copy of the map.
     */
    private static Object copyMap(final Object obj, final Map<Object, Object> visited) {
        final  Map<?, ?> original = (Map<?, ?>) obj;
        final Map<Object, Object> copy;

        try {
            if (original.getClass().isAssignableFrom(HashMap.class)) {
                copy = hashMapPool.acquire();
            } else {
                copy = createNewMapInstance(original.getClass());
            }

            visited.put(obj, copy);
            for (Map.Entry<?, ?> entry : original.entrySet()) {
                try {
                    Object keyCopy = deepCopyInternal(entry.getKey(), visited);
                    Object valueCopy = deepCopyInternal(entry.getValue(), visited);
                    copy.put(keyCopy, valueCopy);
                } catch (Exception e) {
                    throw new RuntimeException("Error copying map entry", e);
                }
            }
            return copy;
        } catch (Exception e) {
            throw new RuntimeException("Error during map copy", e);
        }
    }

    /**
     * Creates a new instance of the given class using its no-arg constructor.
     *
     * @param clazz The class to instantiate.
     * @return A new instance of the class.
     * @throws RuntimeException If an error occurs while creating the new instance.
     */
    private static Object createNewInstance(final Class<?> clazz) {
        try {
            Constructor<?>[] constructors = clazz.getDeclaredConstructors();
            for (Constructor<?> constructor : constructors) {
                final Class<?>[] parameterTypes = constructor.getParameterTypes();
                final Object[] params = new Object[parameterTypes.length];

                for (int i = 0; i < parameterTypes.length; i++) {
                    if (parameterTypes[i].isPrimitive()) {
                        params[i] = DEFAULT_VALUES.get(parameterTypes[i]);
                    } else {
                        params[i] = null;
                    }
                }
                return constructor.newInstance(params);
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to create a new instance of class: " + clazz.getName(), e);
        }
        return null;
    }

    /**
     * Creates a new instance of a collection class.
     *
     * @param clazz The collection class to instantiate.
     * @return A new instance of the collection.
     * @throws InstantiationException    If the class cannot be instantiated.
     * @throws IllegalAccessException    If the class cannot be accessed.
     * @throws NoSuchMethodException     If the no-arg constructor is not found.
     * @throws InvocationTargetException If the constructor throws an exception.
     */
    private static Collection<Object> createNewCollectionInstance(final Class<?> clazz)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        return (Collection<Object>) clazz.getDeclaredConstructor().newInstance();
    }

    /**
     * Creates a new instance of a map class.
     *
     * @param clazz The map class to instantiate.
     * @return A new instance of the map.
     * @throws InstantiationException    If the class cannot be instantiated.
     * @throws IllegalAccessException    If the class cannot be accessed.
     * @throws NoSuchMethodException     If the no-arg constructor is not found.
     * @throws InvocationTargetException If the constructor throws an exception.
     */
    private static Map<Object, Object> createNewMapInstance(final Class<?> clazz)
            throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        return (Map<Object, Object>) clazz.getDeclaredConstructor().newInstance();
    }

    /**
     * Copies fields from the original object to the new object.
     *
     * @param original The original object.
     * @param copy     The new object to which fields will be copied.
     * @param visited  A map tracking visited objects to handle cyclic references.
     * @param clazz    The class of the original object.
     */
    private static void copyFields(final Object original, final Object copy, final Map<Object, Object> visited, final Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            field.setAccessible(true);
            try {
                Object value = field.get(original);
                Object copiedValue = deepCopyInternal(value, visited);
                field.set(copy, copiedValue);
            } catch (Exception e) {
                throw new RuntimeException("Error copying field: " + field.getName(), e);
            }
        }
    }
}
