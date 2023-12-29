package wily.legacy.util;


import java.util.function.Supplier;

public class Stocker<T> implements Supplier<T> {
    private T object;
    public Stocker(T obj){
        object = obj;
    }
    public T get() {
        return object;
    }
    public void set(T obj){
        object = obj;
    }
    public static<T> Stocker<T> of(T obj){return new Stocker<>(obj);}

    public static class Sizeable extends Stocker<Integer>{
        public int max = 0;
        public int min = 0;

        public Sizeable(Integer i) {
            super(i);
        }
        public void set(Integer obj) {
            super.set(Math.max(min,Math.min(obj,max)));
        }
        public int add(int value){
            int oldValue = get();
            set(get() + value);
            return get() - oldValue;
        }
        public int shrink(int value){
            return add(-value);
        }
    }
}
