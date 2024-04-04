package com.jia.sort;

import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Merge sort a List. 
 * Optional threaded sort.
 */
public class MergeSort {

    @SuppressWarnings("unchecked")
    public <T> void mergeSort(List<T> l, Comparator<? super T> c, boolean threaded) {
        Object[] a = l.toArray();

        arraySort((T[])a, c, threaded);

        ListIterator<T> i = l.listIterator();
        for (Object e : a) {
            i.next();
            i.set((T) e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> void arraySort(T[] a, Comparator<? super T> c, boolean threaded) {
        Object[] aux = a.clone();
        if (threaded) {
            ThreadedMergeSort sort = new ThreadedMergeSort((T[])aux, a, 0, a.length, 0, c);
            sort.run();
        } else {
            mergeSort((T[])aux, (T[])a, 0, a.length, 0, c);
        }
    }

    public class ThreadedMergeSort implements Runnable {

        public static final int MAX_THREADS = Runtime.getRuntime().availableProcessors();
        static final ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);

        final Object[] src, dest;
        final int low, high, off;
        final Comparator<Object> c;

        private final int minParitionSize;

        @SuppressWarnings("unchecked")
        public <T> ThreadedMergeSort(T[] src, T[] dest, int low, int high, int off, Comparator<? super T> c) {
            this.minParitionSize = src.length / MAX_THREADS;
            this.src = src;
            this.dest = dest;
            this.low = low;
            this.high = high;
            this.off = off;
            this.c = (Comparator<Object>) c;
            System.out.println("In ThreadedMergeSort: MAX_THREADS=" + MAX_THREADS + "; minParitionSize="
                    + minParitionSize + "; low=" + low + "; high=" + high + "; off=" + off);
        }

        public void run() {
            try {
                threadedMergeSort(src, dest, low, high, off, c);
            } catch (Exception e) {
                e.printStackTrace();
                shutdownAndAwaitTermination();
            }
        }

        private <T> void threadedMergeSort(T[] src, T[] dest, int low, int high, int off, Comparator<? super T> c) {

            int length = high - low;

            System.out.println(
                    "In threadedMergeSort: length=" + length + "; low=" + low + "; high=" + high + "; off=" + off);

            // Insertion sort on smallest arrays
            if (length < 7) {
                for (int i = low; i < high; i++)
                    for (int j = i; j > low && c.compare(dest[j - 1], dest[j]) > 0; j--)
                        swap(dest, j, j - 1);
                return;
            }

            // Recursively sort halves of dest into src
            int destLow = low;
            int destHigh = high;
            low += off;
            high += off;
            int mid = (low + high) >>> 1;
            if (length > minParitionSize) {
                ThreadedMergeSort sort = new ThreadedMergeSort(dest, src, low, mid, -off, c);
                Future<?> future = executor.submit(sort);
                threadedMergeSort(dest, src, mid, high, -off, c);

                try {
                    future.get(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    e.printStackTrace(); // intentionally continue despite error
                }
            } else {
                mergeSort(dest, src, low, mid, -off, c);
                mergeSort(dest, src, mid, high, -off, c);
            }

            // If list is already sorted, just copy from src to dest. This is an
            // optimization that results in faster sorts for nearly ordered lists.
            if (c.compare(src[mid - 1], src[mid]) <= 0) {
                System.arraycopy(src, low, dest, destLow, length);
                return;
            }

            // Merge sorted halves (now in src) into dest
            for (int i = destLow, p = low, q = mid; i < destHigh; i++) {
                if (q >= high || p < mid && c.compare(src[p], src[q]) <= 0)
                    dest[i] = src[p++];
                else
                    dest[i] = src[q++];
            }
        }

        public static void shutdownAndAwaitTermination() {
            executor.shutdown(); // Disable new tasks from being submitted
            try {
                // Wait a while for existing tasks to terminate
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow(); // Cancel currently executing tasks
                    // Wait a while for tasks to respond to being cancelled
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS))
                        System.err.println("Pool did not terminate");
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
                // (Re-)Cancel if current thread also interrupted
                executor.shutdownNow();
                // Preserve interrupt status
                Thread.currentThread().interrupt();
            }
        }
    }

    public static <T> void mergeSort(T[] src, T[] dest, int low, int high, int off, Comparator<? super T> c) {
        int length = high - low;

        System.out.println("In mergeSort: length=" + length + "; low=" + low + "; high=" + high + "; off=" + off);

        /*
         *  Insertion sort on smallest arrays
         *  Cursor from left to right, compare current with left, swap if current is smaller. Keep swapping until 
         *  current is bigger.
         */
        if (length < 7) {
            for (int i = low; i < high; i++)
                for (int j = i; j > low && c.compare(dest[j - 1], dest[j]) > 0; j--) {
                    swap(dest, j, j - 1);
                }
            return;
        }

        // Recursively sort halves of dest into src
        int destLow = low;
        int destHigh = high;
        low += off;
        high += off;
        int mid = (low + high) >>> 1;
        mergeSort(dest, src, low, mid, -off, c);
        mergeSort(dest, src, mid, high, -off, c);

        /*
         *  If list is already sorted, just copy from src to dest. This is an
         *  optimization that results in faster sorts for nearly ordered lists.
         */
        if (c.compare(src[mid - 1], src[mid]) <= 0) {
            System.arraycopy(src, low, dest, destLow, length);
            return;
        }

        /*
         *  Merge sorted halves (now in src) into dest
         *  Compare the two halves, one from each half at a time, left to right. Take the smaller value. Increment on
         *  the side just taken.
         */
        
        for (int i = destLow, p = low, q = mid; i < destHigh; i++) {
            if (q >= high || p < mid && c.compare(src[p], src[q]) <= 0)
                dest[i] = src[p++];
            else
                dest[i] = src[q++];
        }
    }

    private static void swap(Object[] x, int a, int b) {
        Object t = x[a];
        x[a] = x[b];
        x[b] = t;
    }

}
