package server;

public interface DataService<T> {

    boolean store(T obj);

    T retrieve();

    boolean remove(T obj);
}
