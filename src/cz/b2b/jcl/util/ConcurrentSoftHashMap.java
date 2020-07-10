/*
https://www.javaspecialists.eu/archive/Issue015.html
 */
package cz.b2b.jcl.util;

//: ConcurrentSoftHashMap.java
import java.util.*;
import java.lang.ref.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.AbstractMap.SimpleImmutableEntry;

public class ConcurrentSoftHashMap<K, V> extends AbstractMap {

    /**
     The internal HashMap that will hold the SoftReference.
     */
    private final Map<Object, SoftReference> hash = new ConcurrentHashMap<>();
    /**
     The number of "hard" references to hold internally.
     */
    private final int HARD_SIZE;
    /**
     The FIFO list of hard references, order of last access.
     */
    private final ConcurrentLinkedSetQueue hardCache = new ConcurrentLinkedSetQueue();
    /**
     Reference queue for cleared SoftReference objects.
     */
    private final ReferenceQueue queue = new ReferenceQueue();

    public ConcurrentSoftHashMap(int hardSize) {
        HARD_SIZE = hardSize;
    }

    @Override
    public Object get(Object key) {
        Object result = null;
        // We get the SoftReference represented by that key
        SoftReference soft_ref = hash.get(key);
        if (soft_ref != null) {
            // From the SoftReference we get the value, which can be
            // null if it was not in the map, or it was removed in
            // the processQueue() method defined below
            result = soft_ref.get();
            if (result == null) {
                // If the value has been garbage collected, remove the
                // entry from the HashMap.
                hash.remove(key);
            } else {
                // We now add this object to the beginning of the hard
                // reference queue.  
                hardCache.enqueue(result);
                if (hardCache.size() > HARD_SIZE) {
                    // Remove the last entry if list longer than HARD_SIZE
                    hardCache.dequeue();
                }
            }
        }
        return result;
    }

    /**
     Here we put the key, value pair into the HashMap using
     a SoftValue object.
     @param key
     @param value
     @return
     */
    @Override
    public Object put(Object key, Object value) {
        processQueue(); // throw out garbage collected values first
        return hash.put(key, new SoftValue(value, key, queue));
    }

    @Override
    public Object remove(Object key) {
        processQueue(); // throw out garbage collected values first
        return hash.remove(key);
    }

    @Override
    public void clear() {
        hardCache.clear();
        processQueue(); // throw out garbage collected values
        hash.clear();
    }

    @Override
    public int size() {
        processQueue(); // throw out garbage collected values first
        return hash.size();
    }

    @Override
    public boolean containsKey(Object key) {
        processQueue(); // throw out garbage collected values first
        return hash.containsKey(key);
    }

    @Override
    public Set entrySet() {
        Set<Map.Entry> entry = new HashSet<>();
        Map.Entry simpleImmutableEntry = null;
        Object result = null;
        processQueue(); // throw out garbage collected values first
        for (Map.Entry<Object, SoftReference> item : hash.entrySet()) {
            if (item == null) {
                continue;
            }
            Object key = item.getKey();
            SoftReference soft_ref = item.getValue();
            if (soft_ref != null) {
                result = soft_ref.get();
                if (result == null) {
                    hash.remove(key);
                } else {
                    hardCache.enqueue(result);
                    if (hardCache.size() > HARD_SIZE) {
                        hardCache.dequeue();
                    }
                    simpleImmutableEntry = new SimpleImmutableEntry(key, result);
                    entry.add(simpleImmutableEntry);

                }
            }

        }

        return entry;
    }

    private class ConcurrentLinkedSetQueue<E> extends ConcurrentLinkedQueue<E> {

        public void enqueue(E o) {
            if (!contains(o)) {
                add(o);
            }
        }

        public E dequeue() {
            return poll();
        }

    }

    /**
     We define our own subclass of SoftReference which contains
     not only the value but also the key to make it easier to find
     the entry in the HashMap after it's been garbage collected.
     */
    private static class SoftValue extends SoftReference {

        private final Object key; // always make data member final

        /**
         Did you know that an outer class can access private data
         members and methods of an inner class? I didn't know that!
         I thought it was only the inner class who could access the
         outer class's private information. An outer class can also
         access private members of an inner class inside its inner
         class.
         */
        private SoftValue(Object k, Object key, ReferenceQueue q) {
            super(k, q);
            this.key = key;
        }
    }

    /**
     Here we go through the ReferenceQueue and remove garbage
     collected SoftValue objects from the HashMap by looking them
     up using the SoftValue.key data member.
     */
    private void processQueue() {
        SoftValue sv;
        while ((sv = (SoftValue) queue.poll()) != null) {
            hash.remove(sv.key); // we can access private data!
        }
    }

}
