package com.hwbs.intertask.bench;

import com.hwbs.intertask.server.GreetingServiceImpl;
import com.hwbs.intertask.server.HybridSorting;
import com.hwbs.intertask.server.TrieSorting;
import com.hwbs.intertask.server.generator.TaskResult;
import com.hwbs.intertask.server.generator.mt.NamesGenExecutor;
import com.hwbs.intertask.shared.NameRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * User:      kaa
 * Timestamp: 12/28/13 6:45 AM
 *
 * Results for: Core™2 Duo CPU P8600 @ 2.40GHz
 * 100 iterations, average result taken
 *
 *  (k) - radix sort prefix size
 *
 * hybrid sort time, taken (5) (avg from 100 iters): 1504 ms
 * hybrid sort time, taken (4) (avg from 100 iters): 1190 ms
 * hybrid sort time, taken (3) (avg from 100 iters): 1116 ms
 * hybrid sort time, taken (2) (avg from 100 iters): 11917 ms
 * hybrid sort time, taken (1) (avg from 100 iters): 0 ms
 *
 * hybrid mixed sort time, taken (5) (avg from 100 iters): 1523 ms
 * hybrid mixed sort time, taken (4) (avg from 100 iters): 1250 ms
 * hybrid mixed sort time, taken (3) (avg from 100 iters): 1002 ms
 * hybrid mixed sort time, taken (2) (avg from 100 iters):  901 ms   <--- seems to be best one
 * hybrid mixed sort time, taken (1) (avg from 100 iters): 1144 ms
 * 
 * system sorttime taken (avg from 100 iters): 1179 ms
 *
 * Notes:
 *
 * - it seems that 'nanoTime' may get counter values from different cores, and they may be in defferent state,
 *   so negative diff values arise. Solution: switch to 'currentTimeMillis'
 *
 */
public class SortingBench {

    private static int BENCH_SIZE  = 1;
    private static int BENCH_TIMES = 100;


    public void testSortingSpeed() {


        List<NameRecord[]> li = new ArrayList<>(BENCH_SIZE);

        for (int i = 0; i < BENCH_SIZE; ++i) {
            NameRecord[] cache = GreetingServiceImpl.fillCacheSingle();
            NamesGenExecutor gen = new NamesGenExecutor(2, cache);

            TaskResult tr = gen.generate();
            tr.waitAll();

            li.add(cache);
        }

        System.out.println("size: " + li.size());

        long start = System.nanoTime();
        for(NameRecord[] cache : li) {
            Arrays.sort(cache, new NameRecord.FirstNameComparator<>() );
        }
        System.out.println("time taken (avg): " +
                (System.nanoTime() - start) / 1000000.0 / BENCH_SIZE +
                " ms");

        start = System.nanoTime();
        for(NameRecord[] cache : li) {
            Arrays.sort(cache, new NameRecord.FirstNameComparator<>() );
        }
        System.out.println("time taken (avg, sorted array): " +
                (System.nanoTime() - start) / 1000000.0 / BENCH_SIZE + " ms");
    }


    public void testSortingHybridWrk() {

        //long accHybrid = 0;
        long accSystem = 0;

        int[] k              = new int[] {5, 4, 3, 2, 1};
        int[] accHybrid      = new int[k.length];
        int[] accHybridMixed = new int[k.length];

        for (int i = 0; i < BENCH_TIMES; ++i) {
            NameRecord[] cache = GreetingServiceImpl.fillCacheSingle();
            NamesGenExecutor gen = new NamesGenExecutor(1, cache);

            TaskResult tr = gen.generate();
            tr.waitAll();

            NameRecord[] c = cache.clone();
            //long start = System.nanoTime();
            long start = System.currentTimeMillis();
            Arrays.sort(c, new NameRecord.FirstNameComparator<>() );
            //accSystem += System.nanoTime() - start;
            accSystem += System.currentTimeMillis() - start;

            for(int j = 0; j < k.length; ++j) {
                if (k[j] < 2 )
                    continue;

                c = cache.clone();
                Runtime.getRuntime().gc();
                //start = System.nanoTime();
                start = System.currentTimeMillis();
                HybridSorting.sortByFirstName(c, k[j] );
                accHybrid[j] += System.currentTimeMillis() - start;

                //accHybrid[j] += System.nanoTime() - start;

                System.out.println(" --- Hybrid " + k[j] + " ----- ");
                GreetingServiceImpl.dumpHead(c, 32);
            }

            for(int j = 0; j < k.length; ++j) {
                c = cache.clone();
                Runtime.getRuntime().gc();
                //start = System.nanoTime();
                start = System.currentTimeMillis();
                HybridSorting.sortByFirstNameMixed(c, k[j]);
                //accHybridMixed[j] += System.nanoTime() - start;
                accHybridMixed[j] += System.currentTimeMillis() - start;

                System.out.println(" --- mixed " + k[j] + " ----- ");
                GreetingServiceImpl.dumpHead(c, 32);
            }


        }

        for(int i = 0; i < k.length; ++i) {
            System.out.println("hybrid sort time, taken ("+ k[i] +") (avg from " + BENCH_TIMES + " iters): " +
                    accHybrid[i] / BENCH_TIMES  +
                    " ms");
        }

        for(int i = 0; i < k.length; ++i) {
            System.out.println("hybrid mixed sort time, taken ("+ k[i] +") (avg from " + BENCH_TIMES + " iters): " +
                    accHybridMixed[i] / BENCH_TIMES +
                    " ms");
        }


        System.out.println("system sort time, taken (avg from " + BENCH_TIMES + " iters): " +
                accSystem / BENCH_TIMES  +
                " ms");

        //GreetingServiceImpl.dumpHead( li.get(0), 16);

    }

    public void testSortingTrie() {
        NameRecord[] cache = GreetingServiceImpl.fillCacheSingle();
        NamesGenExecutor gen = new NamesGenExecutor(1, cache);

        TaskResult tr = gen.generate();
        tr.waitAll();

        NameRecord[] c = cache.clone();

        System.out.println("Trie sorting: structure creating");

        long start = System.currentTimeMillis();
        final int prefixLen = 3;
        TrieSorting<NameRecord> tsort = new TrieSorting<>( prefixLen, c.length );

        System.out.println("total buckets size (0): " + tsort.totalResultSize() );


        NameRecord.FirstNameGetter<NameRecord> fg = new NameRecord.FirstNameGetter<>(prefixLen);
        for(NameRecord nr : c) {
             tsort.add(nr, fg);
        }

        //List li = tsort.sort(fg);
        List<NameRecord> li = tsort.getUnsortedResult();
        Collections.sort(li, new NameRecord.FirstNameComparator<>() );
        li.toArray(c);

        //NameRecord[] arr = tsort.getUnsortedArrayResult(c);
        //Arrays.sort(arr, new NameRecord.FirstNameComparator() );

        System.out.println("sort time, taken " +
                 + (System.currentTimeMillis() - start ) + " ms");
        System.out.println("leaf avg size " + tsort.avgLeafSize() );
        System.out.println("total buckets size (1): " + tsort.totalResultSize() );
        System.out.println("number of comp.: " + TrieSorting.STAT_CMP );
        System.out.println("result size " + li.size() );
        //System.out.println("result size " + arr.length );



//        TrieSorting.ListNode<NameRecord> hd = tsort.getResult();
//        TrieSorting.ListNode<NameRecord> node = hd;
//
//        int i = 0;
//        while(null != node && i < 100) {
//            System.err.println(node.get().firstName());
//            node = node.next();
//        }
    }

    public void testTrieSortingMultiple() {

        int accSortTime = 0;
        final int prefixLen = 2;
        TrieSorting<NameRecord> tsort = new TrieSorting<>( prefixLen, 1000000 );

        System.err.println("Stating sorting iterations..");
        for(int i = 0; i < BENCH_TIMES; ++i ) {

            NameRecord[] cache = GreetingServiceImpl.fillCacheSingle();
            NamesGenExecutor gen = new NamesGenExecutor(1, cache);

            TaskResult tr = gen.generate();
            tr.waitAll();

            NameRecord[] c = cache.clone();

            System.err.println("--- new sorting iteration ----");
            GreetingServiceImpl.dumpHead(c, 16);
            GreetingServiceImpl.dumpTail(c, 16);


            long start = System.currentTimeMillis();
            tsort.sortBy1stName(c);
            accSortTime += System.currentTimeMillis() - start;

            GreetingServiceImpl.dumpHead(c, 16);
            GreetingServiceImpl.dumpTail(c, 16);

            System.err.println();
        }

        System.err.println(" trie sorting avg time: " + (accSortTime / (double) BENCH_SIZE ) + " ms" );
    }

    public void testRadixMSD() {

        long accHybridTiming = 0;
        for(int i = 0; i < BENCH_TIMES; ++i ) {

            NameRecord[] cache = GreetingServiceImpl.fillCacheSingle();
            NamesGenExecutor gen = new NamesGenExecutor(1, cache);

            TaskResult tr = gen.generate();
            tr.waitAll();

            NameRecord[] a = cache.clone();
            NameRecord[] b = cache.clone();

            long start = System.currentTimeMillis();
            HybridSorting.sortRadixMSD(a, new NameRecord.FirstNameGetter<>() );
            accHybridTiming += System.currentTimeMillis() - start;

            Arrays.sort(b, new NameRecord.FirstNameGetter<>().getComparator() );

            if (!Arrays.equals(a,b)) {
                GreetingServiceImpl.dumpHead(a, 16);
                GreetingServiceImpl.dumpTail(b, 16);
                System.err.print("Incorrect array sorting!");
                System.exit(1310);
            }


            //GreetingServiceImpl.dumpHead(c, 16);
            //GreetingServiceImpl.dumpTail(c, 16);
        }

        System.err.println(" hybrid RadixMSD sorting avg time: " +
                (accHybridTiming / (double) BENCH_TIMES ) +
                " ms" );

    }

    public void testRadixMSDSA() {

        long accHybridTiming   = 0;
        long classicSortTiming = 0;

        for(int i = 0; i < BENCH_TIMES; ++i ) {

            NameRecord[] cache = GreetingServiceImpl.fillCacheSingle();
            NamesGenExecutor gen = new NamesGenExecutor(1, cache);

            TaskResult tr = gen.generate();
            tr.waitAll();

            NameRecord[] a = cache.clone();
            NameRecord[] b = cache.clone();

            long start = System.currentTimeMillis();
            HybridSorting.sortRadixMSDSA(a, new NameRecord.FirstNameGetter<>() );
            accHybridTiming += System.currentTimeMillis() - start;


            start = System.currentTimeMillis();
            Arrays.sort(b, new NameRecord.FirstNameGetter<>().getComparator() );
            classicSortTiming += System.currentTimeMillis() - start;

            //b[1024] = b[512];

            if (!Arrays.equals(a,b)) {
                //GreetingServiceImpl.dumpHead(a, 16);
                //GreetingServiceImpl.dumpTail(b, 16);
                System.err.print("Incorrect array sorting!");
                System.exit(1310);
            }


            //GreetingServiceImpl.dumpHead(c, 16);
            //GreetingServiceImpl.dumpTail(c, 16);
        }

        System.err.println(" hybrid RadixMSD-SA sorting avg time: " +
                accHybridTiming / (double) BENCH_TIMES  +
                " ms" );

        System.err.println(" classic sorting avg time: " +
                classicSortTiming / (double) BENCH_TIMES  +
                " ms" );

        System.err.println(" RadixMSD-SA / Classic ration: " +
                100.f * accHybridTiming / classicSortTiming  +
                " %" );
    }


    public static void main(String[] arg) {
        SortingBench sb = new SortingBench();

        //sb.testSortingHybridWrk();
        //sb.testSortingTrie();
        //sb.testTrieSortingMultiple();

        sb.testRadixMSD();
        sb.testRadixMSDSA();
    }

}
