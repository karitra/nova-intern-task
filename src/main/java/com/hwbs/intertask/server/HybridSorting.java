package com.hwbs.intertask.server;

import com.hwbs.intertask.shared.NameRecord;

import java.util.Arrays;
import java.util.Comparator;

/**
 * User:      kaa
 * Timestamp: 12/28/13 7:34 AM
 *
 * Counter-insertion sorting
 *
 * Now is used in application: partial sort with radix prefix = 2
 *
 * Sort algorithm selection is based on benchmarking (@link com.hwbs.intertask.bench.SortingBench)
 * and some common sense.
 *
 */
public class HybridSorting<T extends NameRecord> {

    private static final int BEST_RADIX_SORT_PREFIX     = 2;
    private static final int RADIX_SORT_MAX_DIGITS = 10;

    private static final char[] charset = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();

    //
    // Order working array sequence: \00..9A..Za..z
    //
    private static int ORDER_ARRAY_SIZE = 2 * charset.length - 10  + 1 /* zero char */;

    private int[]         wrkArray       ;//  = new int[ORDER_ARRAY_SIZE];
    private NameRecord[] shadowArr;//r = new NameRecord[a.length];

    //final short[] paritions     = new short[1<<16];
    private int k;


    //private Comparator<NameRecord> comparator;
    private NameRecord.FieldGetter fieldGetter;


    HybridSorting( T[] a, int k, NameRecord.FieldGetter fg) {

        assert k > 0;
        assert k < RADIX_SORT_MAX_DIGITS;

        //this.comparator  = cmp;
        this.fieldGetter = fg;

        this.k = k;

        wrkArray  = new int[ORDER_ARRAY_SIZE];
        shadowArr = new NameRecord[a.length];
    }

    public static <T extends NameRecord> void sortByFirstName( T[] a, int k) {
        sort(a, k, new NameRecord.FirstNameGetter<T>() );
    }

    public static <T extends NameRecord> void sortBySecondName( T[] a, int k) {
        sort(a, k, new NameRecord.SecondNameGetter<T>() );
    }


    public static <T extends NameRecord> void sortByFirstNameMixed( T[] a, int k) {
        sortMixed(a, k, new NameRecord.FirstNameGetter<T>());
    }

    public static <T extends NameRecord> void sortBySecondNameMixed( T[] a, int k) {
        sortMixed(a, k, new NameRecord.SecondNameGetter<T>());
    }

    public static <T extends NameRecord> void sortByFirstNameMixed(T[] a) {
        sortMixed(a, BEST_RADIX_SORT_PREFIX,  new NameRecord.FirstNameGetter<T>());
    }

    public static <T extends NameRecord> void sortBySecondNameMixed(T[] a) {
        sortMixed(a, BEST_RADIX_SORT_PREFIX,  new NameRecord.SecondNameGetter<T>());
    }


    public static <T extends NameRecord> void sortMixed(T[] a, int k, NameRecord.FieldGetter<T> fg) {
        HybridSorting<T> hs = new HybridSorting<>(a, k, fg);

        hs.sortRadixLSD(a);
        Arrays.sort(a, fg.getComparator());
    }


    public static <T extends NameRecord> void sort(T[] a, int k,NameRecord.FieldGetter<T> fg) {

        HybridSorting<T> hs = new HybridSorting<>(a, k, fg);

        hs.sortRadixLSD(a);
        hs.insertionSort(a, fg);
    }


    //private void insertionSort(NameRecord[] a) {
    private static final <T extends NameRecord> void insertionSort( T[] a, NameRecord.FieldGetter<T> fg) {
        insertionSort(a, 0, a.length, fg);
    }

    private static final <T extends NameRecord> void insertionSort( T[] a, int offset, int len,  NameRecord.FieldGetter<T> fg) {
        int i;

        Comparator<T> cmp = fg.getComparator();

        for(int j = offset + 1; j < offset + len; ++j ) {

            for( i = j;
                 i > offset &&
                         cmp.compare( a[i], a[i - 1]) < 0;
                 --i ) {
                swap(a, i, i - 1);
            }
        }
    }


    public static void countPartitions(NameRecord[] a, short[] part) {
        for(int i = 0; i < a.length; ++i) {
            part[ (0xFF & Character.getNumericValue( a[i].firstName()[0]) ) << 8 |
                   0xFF & Character.getNumericValue( a[i].firstName()[1]  ) ]++;
        }
    }

    public static final <T extends NameRecord> void sortRadixMSD(T[] a, NameRecord.FieldGetter<T> fg) {
        int[] oracle = new int[a.length];
        sortRadixMSD(a, 0, a.length, 0, oracle, fg);
    }

    public static final <T extends NameRecord> void sortRadixMSDSA(T[] a, NameRecord.FieldGetter<T> fg) {
        int[] oracle = new int[a.length];
        sortRadixMSDSA(a, 0, a.length, 0, oracle, fg);
    }


    public static final <T extends NameRecord> void sortRadix1StNameMSDSA( T[] a ) {
        int[] oracle = new int[a.length];
        sortRadixMSDSA( a, 0, a.length, 0, oracle, new NameRecord.FirstNameGetter<T>()  );
    }

    public static final <T extends NameRecord> void sortRadix2ndNameMSDSA( T[] a ) {
        int[] oracle = new int[a.length];
        sortRadixMSDSA( a, 0, a.length, 0, oracle, new NameRecord.SecondNameGetter<T>() );
    }


    private static <T extends NameRecord> void sortRadixMSD(T[] a, int offset, int len, int depth, int[] oracle, NameRecord.FieldGetter<T> fg) {

        //
        // 0. If small enough, use insertion
        //
        if (len < 16) {
            insertionSort(a, offset, len, fg);
            return;
        }

        //
        // 1. Count buckets size
        //
        int[] bucketsizes   = new int[ORDER_ARRAY_SIZE];
        int[] bucketindexes = new int[ORDER_ARRAY_SIZE];


        for(int i = offset; i < offset + len; ++i) {
            oracle[i] = mapIndex(fg.get(a[i])[depth]);
        }

        for(int i = offset; i < offset + len; ++i) {
            bucketsizes[ oracle[i] ]++;
        }

        //
        // Summing out offset
        //
        for(int i = 1; i < bucketsizes.length; ++i) {
            bucketindexes[i] = bucketindexes[i - 1] + bucketsizes[ i - 1 ];
        }

        NameRecord[] sortedArr = new NameRecord[len];
        for(int i = offset; i < offset + len; ++i) {
            // assert a[i] != null;
            //sortedArr[ bucketindexes[mapIndex( fg.get(a[i] )[depth] )]++ ] = a[i];
            //sortedArr[ bucketindexes[mapIndex( oracle[i] )]++ ] = a[i];
            sortedArr[ bucketindexes[ oracle[i] ]++ ] = a[i];
        }

        //
        // [DEBUG]
        //
//        for(int i = 0; i < sortedArr.length; ++i) {
//            if (null == sortedArr[i]) {
//                System.err.println("sorted is null at index " + i + ", offset = " + offset + ", len = " + len  );
//               System.exit(1310);
//            }
//        }

        //
        // Update source
        //
        System.arraycopy( sortedArr, 0, a, offset, len);

        //
        // Dive into recursion
        //
        int blockOffset = offset; //bucketsizes[0];
        for(int i = 0; i < bucketsizes.length; ++i) {
            if (bucketsizes[i] == 0)
                continue;

            sortRadixMSD(a, blockOffset, bucketsizes[i], depth + 1, oracle, fg );
            blockOffset += bucketsizes[i];
        }
    }

    //
    // Super alphabet implementation
    //
    // Alphabet size:
    //
    // 26 * 2 (upper case + lower case) + 10 (ten digits) = 62, so one element = 2^6
    //
    //
    private static <T extends NameRecord> void sortRadixMSDSA(T[] a, int offset, int len, int depth, int[] oracle, NameRecord.FieldGetter<T> fg) {

        if (len < (1<<16) ) { // 2^16 seems to be optimal
            sortRadixMSD(a, offset, len, depth, oracle, fg);
            return;
        }

        //
        // 1. Count buckets size
        //
        int[] bucketsizes   = new int[1<<12];
        int[] bucketindexes = new int[1<<12];


        char[] cha;
        for(int i = offset; i < offset + len; ++i) {
            cha = fg.get(a[i]);
            oracle[i] = mapIndex( cha[ depth    ]  ) << 6 |
                    (0x3F & mapIndex( cha[ depth + 1])  );
        }

        for(int i = offset; i < offset + len; ++i) {
            bucketsizes[ oracle[i] ]++;
        }

        //
        // [DEBUG]
        //
//        for(int i = 0; i < bucketindexes.length / 100; ++i) {
//            System.out.printf("[%5d]: %d \n", i,  bucketsizes[i]);
//        }


        //
        // Summing out offset
        //
        for(int i = 1; i < bucketsizes.length; ++i) {
            bucketindexes[i] = bucketindexes[i - 1] + bucketsizes[ i - 1 ];
        }


//        System.exit(1310);

        NameRecord[] sortedArr = new NameRecord[len];
        for(int i = offset; i < offset + len; ++i) {
            // assert a[i] != null;
            //sortedArr[ bucketindexes[mapIndex( fg.get(a[i] )[depth] )]++ ] = a[i];
            //sortedArr[ bucketindexes[mapIndex( oracle[i] )]++ ] = a[i];
            sortedArr[ bucketindexes[ oracle[i] ]++ ] = a[i];
        }

        //
        // [DEBUG]
        //
//        for(int i = 0; i < sortedArr.length; ++i) {
//            if (null == sortedArr[i]) {
//                System.err.println("sorted is null at index " + i + ", offset = " + offset + ", len = " + len  );
//               System.exit(1310);
//            }
//        }

        //
        // Update source
        //
        System.arraycopy( sortedArr, 0, a, offset, len);

        //
        // Dive into recursion
        //
        int blockOffset = offset; //bucketsizes[0];
        for(int i = 0; i < bucketsizes.length; ++i) {
            if (bucketsizes[i] == 0)
                continue;

            sortRadixMSDSA(a, blockOffset, bucketsizes[i], depth + 2, oracle, fg );
            blockOffset += bucketsizes[i];
        }
    }



    private <T extends NameRecord> void sortRadixLSD(T[] a) {

        if (this.k == 0)
            return;

        for (int k = this.k - 1; k >= 0; --k) {

            //
            // First pass
            //
            for (int i = 0; i < a.length; ++i) {
                wrkArray[ mapIndex(fieldGetter.get(a[i])[k]) ]++;
            }

            //dumpArray(wrkArray);

            //
            // Add up array values
            //
            for (int i = 1; i < wrkArray.length; ++i) {
                wrkArray[i] += wrkArray[i - 1];
            }

            //
            // Normalize index
            //
            //for (int i = 0; i < wrkArray.length; ++i) {
            //    wrkArray[i]--;
            //}

            //dumpArray(wrkArray);

            //
            // Final pass
            //
            int index;
            for(int i = a.length - 1; i >= 0; --i) {
                index = mapIndex( fieldGetter.get(a[i])[k] );
                place(a, i, shadowArr, --wrkArray[index]);
            }

            //dumpArray(wrkArray);

            //
            // Clear for next iteration
            //
            Arrays.fill(wrkArray, 0);

            //
            // Update source
            //
            System.arraycopy( shadowArr, 0, a, 0, a.length);
        }
    }

    private static final void place(NameRecord[] src, int srcIndex, NameRecord[] dst, int dstIndex) {
        dst[dstIndex] = src[srcIndex];
    }

    private static final void zeroArray(int[] a) {
        Arrays.fill(a, 0);
    }

    private static final <T> void swap(T[] a, int from, int to) {
        T tmp   = a[from];
        a[from] = a[to];
        a[to]   = tmp;
    }

//
//    private static final void swap(NameRecord[] a, int from, int to) {
//        NameRecord tmp = a[from];
//        a[from] = a[to];
//        a[to]   = tmp;
//    }


    private static final int mapIndex(char ch) {
        if (ch < 'A') { // digit or nil, return as is
            if (ch == '\0')
                return 0;
            else
                return ch - '0';
        } else if (ch < 'a') { // cappa
            return 10  - 'A' + ch;
        } else {
            return 26 + 10 - 'a' + ch;
        }
    }

    private static void dumpArray(int[] a) {
        for(int i = 0; i < a.length; ++i) {
            System.out.printf( "[%2d] %10d \n", i,  a[i] );
        }
    }


    private static void dumpArray(short[] a) {
        int ranges = 0;

        for(int i = 0; i < a.length; ++i) {
            if (a[i] > 0) {
                System.out.printf( "[%2d] %10d \n", i,  a[i] );
                ranges++;
            }
        }

        System.out.printf( "ranges %d\n", ranges );

    }


}
