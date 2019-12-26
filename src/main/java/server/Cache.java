package server;

public class Cache<V extends Comparable<V>, T extends IndexedData<V>> implements DataService<T> {

    public boolean store(T obj) {
        return false;
    }

    public T retrieve() {
        return null;
    }

    public boolean remove(T obj) {
        return false;
    }
}
