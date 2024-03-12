package searchengine.services;

import searchengine.dto.Response;

import java.util.Collection;
import java.util.List;

public interface CRUDService<T> {
    public List<T> getAll();
    public Object get(int id);
    public Response delete(int id);
    public void create(T obj);
    public void update(T obj);
}
