package server;

public class Cache<V extends Comparable<V>, T extends IndexedData<V>> implements DataService<T> {

    public boolean store() {
        return false;
    }

    public T retrieve() {
        return null;
    }
}
