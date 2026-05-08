package pro.revive.interfaces;

import java.util.List;

public interface IService<T> {

    void addEntity(T t);

    void deleteEntity(T t);

    void updateEntity(int id, T t);

    List<T> getData();
}
