package wily.legacy.util;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import wily.legacy.client.screen.TabButton;

import java.util.*;

public class PagedList<T> extends AbstractList<T> {
    public final Stocker.Sizeable page;
    private final int maxListSize;
    public T[] objects = (T[]) new Object[0];

    public PagedList(Stocker.Sizeable page, int maxListSize){
        this.maxListSize = maxListSize;
        this.page = page;
    }
    public int allSize(){
        return objects.length;
    }

    @Override
    public int size() {
        return Math.min(maxListSize,allSize() - page.get() * maxListSize);
    }

    @Override
    public void add(int index, T element) {
        objects = ArrayUtils.add(objects,index,element);
    }

    @Override
    public boolean add(T t) {
        if (!isEmpty() && (allSize() % maxListSize) == 0 && page.max <= (allSize() / maxListSize - 1))
            page.max++;
        add(allSize(),t);
        return true;
    }
    @Override
    public T get(int index) {
        return objects[page.get() * maxListSize + index];
    }


    @Override
    public T remove(int index) {
        T r = objects[index];
        objects = ArrayUtils.remove(objects,index);
        if (objects.length % maxListSize == maxListSize - 1) page.max--;
        return r;
    }

    @Override
    public int indexOf(Object o) {
        for (int i = 0; i < objects.length; i++)
            if (objects[i] == o) return i % maxListSize;
        return -1;
    }
    public static <T> int occurrenceOf(List<T> list, T object, int index){
        int o = 0;
        for (int i = 0; i < list.size(); i++) {
            if (object == list.get(i)){
                if (i == index) return o;
                else o++;
            }
        }
        return -1;
    }
    public static <T> int indexOf(List<T> list, T object, int occurrence){
        int o = 0;
        for (int i = 0; i < list.size(); i++) {
            if (object == list.get(i)){
                if (o == occurrence) return i;
                else o++;
            }
        }
        return -1;
    }


}
