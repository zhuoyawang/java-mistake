#### ThreadLocal

threadLocal是一个线程内部的存储类，可以在指定线程内部存储数据，数据存储以后，只有指定线程才可以得到存储数据。

threadLocal的静态内部类ThreadLocalMap为每一个thread都维护了一个table，threadLocal确定了一个数组下标，这个下标就是value存储对应位置

线程在重复使用的时候，若上一个线程的value没有及时的清除，新线程未赋值之前取到的都是上一个线程的数据，所以在使用完之后一定要进行remove掉

##### threadLocal作为线程的内部存储类，主要的就是他的get和set方法

get方法

```
    public T get() {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            ThreadLocalMap.Entry e = map.getEntry(this);
            if (e != null) {
                @SuppressWarnings("unchecked")
                T result = (T)e.value;
                return result;
            }
        }
        return setInitialValue();
    }
    
```
```
    private T setInitialValue() {
        T value = initialValue();
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
        return value;
    }
```

如上所示get方法中，获取当前线程值，通过调用getMap(t)方法，获取该线程对应的ThreadLocalMap。在map中找到该线程对应的下标，获取value值返回。
如果线程t没有对应的threadLocalMap，就set一个进去


###### set方法


```
    public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null)
            map.set(this, value);
        else
            createMap(t, value);
    }
```
```
private void set(ThreadLocal<?> key, Object value) {
 
             // We don't use a fast path as with get() because it is at
             // least as common to use set() to create new entries as
             // it is to replace existing ones, in which case, a fast
             // path would fail more often than not.
 
             Entry[] tab = table;
             int len = tab.length;
             int i = key.threadLocalHashCode & (len-1);
 
             for (Entry e = tab[i];
                  e != null;
                  e = tab[i = nextIndex(i, len)]) {
                 ThreadLocal<?> k = e.get();
 
                 if (k == key) {
                     e.value = value;
                     return;
                 }
 
                 if (k == null) {
                     replaceStaleEntry(key, value, i);
                     return;
                 }
             }
 
             tab[i] = new Entry(key, value);
             int sz = ++size;
             if (!cleanSomeSlots(i, sz) && sz >= threshold)
                 rehash();
         }
```

1.根据线程信息以及固定计算方式，算出对应的table下标的值。
2.将value里的值放入对应的下班中