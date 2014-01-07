package com.hwbs.intertask.server;

import com.hwbs.intertask.shared.NameRecord;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import static com.hwbs.intertask.server.TrieSorting.LayerType.Alpha;
import static com.hwbs.intertask.server.TrieSorting.LayerType.AlphaNumeric;

/**
 * User:      kaa
 * Timestamp: 12/31/13 11:03 AM
 */
public class TrieSorting<E extends NameRecord> {

    public enum LayerType {
        Alpha,
        AlphaNumeric
    };


    private static final int SMALL_ARRAY_SIZE = 26;
    private static final int LARGE_ARRAY_SIZE = SMALL_ARRAY_SIZE + 10;

    LinkedList<PrefixNode<E>> leafNodesList = new LinkedList<>();


    public interface TrieNode<E extends NameRecord> {
        //PrefixNode[] getRefsArray();
        // public void append(ListNode ln, int level, LayerAdapter adapter);
        void append(E ln, int level, NameRecord.FieldGetter<E> getter);
        void clear();

        ListNode<E> getHead();
        ListNode<E> getTail();

        int size();

        public List<E> getSorted( NameRecord.FieldGetter fg );
    }

    public static class PrefixNode<E extends NameRecord> implements TrieNode<E> {
        TrieNode<E>[] refs;
        ArrayIndexer indexer;

        @SuppressWarnings("unchecked")
        PrefixNode( ArrayIndexer arr) {
            this.refs = new TrieNode[ arr.size() ];

//            this.refs = lastLayer ?
//                    new TailNode[   arr.size() ] :
//                    new PrefixNode[ arr.size() ];
            this.indexer = arr;
        }

        public TrieNode<E>[] getRefsArray() {
            return refs;
        }

        @Override
        public final void append( E e, int level, NameRecord.FieldGetter<E> getter) {
            char ch   = getter.get(e)[level];
            int index = indexer.index(ch);
            refs[index].append( e, level + 1, getter);
        }


        @Override
        public void clear() {
            for(TrieNode node : getRefsArray()) {
                node.clear();
            }
        }

        @Override
        public ListNode<E> getHead() {
            return null;
        }

        @Override
        public ListNode<E> getTail() {
            return null;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public List<E> getSorted(NameRecord.FieldGetter fg) {
            return new LinkedList<>();
        }
    }

    public static int TOTAL = 0;
    public static int STAT_CMP = 0;

    public class TailNode implements TrieNode<E> {

        //LinkedList<E> subList = new LinkedList<>();
        //Set<E> subSet = new TreeSet<>( new NameRecord.FirstNameComparator(4) );

        ListNode<E> head;

        // Tail is needed to construct result list
        ListNode<E> tail;

        int size;

        Comparator<E> cmp;

        //TailNode(Comparator cmp) {
        //    this.cmp = cmp;
        //}



        //@Override
        public final void appendOld1( E e, int ignore, NameRecord.FieldGetter<E> getter) {
            //size++;
            //subList.add(e);
            //TOTAL++;
            //subSet.add(e);
        }

        @Override
        public final void append( E e, int ignore, NameRecord.FieldGetter<E> getter) {
            size++;
            //TOTAL++;

            ListNode<E> inserted = getNextListNode(e);

            inserted.next = head;
            head = inserted;

            if (null == tail) {
                tail = inserted;
            }

           // System.err.println("head: " + head.get());
//            if (size >= 10)
//                System.exit(1310);

        }

            //@Override
        public final void appendOld2( E e, int ignore, NameRecord.FieldGetter<E> getter) {
            size++;

            ListNode<E> inserted = getNextListNode(e);



            inserted.next = head;
            head = inserted;

            if (null == tail) {
                tail = inserted;
            }

            if (true)
                return;


//            if (true)
//                return;


            if (head == null) {
                head = inserted;
                tail = head;
                return;
            }

//            if (true)
//                return;


            //
            // At least one element in list
            //
            Comparator<E> cmp  = getter.getComparator();
            ListNode<E> node   = head;
            // STAT_CMP++;


            if (cmp.compare(node.get(), e) < 0) {

                if (tail == head)
                    tail = inserted;

                head = inserted;
                inserted.next = node;
                return;
            }



            ListNode<E> parent = head;
            node = head.next;


            for( ; null != node; parent = node, node = node.next) {
                // STAT_CMP++;
                if (cmp.compare(node.get(), e) < 0) {
                    break;
                }
            }

//            if (true)
//                return;


            parent.next   = inserted;
            inserted.next = node;
            if (node == null)
                tail = inserted;
        }

        @Override
        public int size() {
            return getSize();
        }

        public int getSize() {
            return size;
            //return subList.size();
            //return subSet.size();
        }

        @Override
        public void clear() {
            head = null;
        }

        @Override
        public ListNode<E> getHead() {
            return head;
        }

        @Override
        public ListNode<E> getTail() {
            return tail;
        }

        //@Override
        public List<E> getSortedOld(NameRecord.FieldGetter fg) {
            //Collections.sort(subList, fg.getComparator() );
            //return subList;
            return null;
        }

        @Override
        public List<E> getSorted(NameRecord.FieldGetter fg) {
            return null;
            //return subSet.iterator();
        }
    }

    public static class ListNode<E> {
        E e;
        ListNode<E> next;

        private ListNode(E e) {
            this.e = e;
        }

        public final E get() {
            return e;
        }

        public final ListNode<E> next() {
            return next;
        }

        public final ListNode<E> set(E e) {
            this.e = e;
            return this;
        }
    }



    private interface ArrayIndexer {
        int index(char ch);
        int size();
    }

    private static class SmallArrayIndexer implements ArrayIndexer {

        @Override
        public final int index(char ch) {
            return ch - 'A';
        }

        @Override
        public int size() {
            return SMALL_ARRAY_SIZE;
        }
    }

    private static class BigArrayIndexer implements ArrayIndexer {

        @Override
        public final int index(char ch) {
            return (ch < 'a') ?
                    ch - '0':
                    ch - 'a' + 10;

        }

        @Override
        public int size() {
            return LARGE_ARRAY_SIZE;
        }

    }

    private static class ArrayIndexerFactory {
        private static ArrayIndexer small; //= new SmallArrayIndexer();
        private static ArrayIndexer   big; //= new BigArrayIndexer();

        public static ArrayIndexer small() {
            if (null == small)
                small = new SmallArrayIndexer();

            return small;
        }

        public static ArrayIndexer big() {
            if (null == big)
                big = new BigArrayIndexer();

            return big;
        }



        public static ArrayIndexer get(int size) {
            switch (size) {
                case SMALL_ARRAY_SIZE:
                    return small();
                case LARGE_ARRAY_SIZE:
                    return big();
            }

            return small();
        }

        public static ArrayIndexer get(LayerType t) {
            switch (t) {
                case Alpha:
                    return small();
                case AlphaNumeric:
                    return big();
            }

            return small();
        }

    }


    PrefixNode<E> root;

    ListNode<E>[] preCreatedListNodes;
    int listNodesOffset;

    public TrieSorting(int prefixLen, int size) {
        assert  prefixLen > 0;

        LayerType[] layers = new LayerType[prefixLen+1];

        layers[0] = Alpha;

        for(int i = 1; i < layers.length; ++i) {
            layers[i] = AlphaNumeric;
        }

        preCreatedListNodes = precreateListNodes(size);
        root = createStructure(layers);
    }

    //
    // Note: not synchronized
    //
    private ListNode<E> getNextListNode(E e) {
        return preCreatedListNodes[listNodesOffset++].set(e);
    }

    @SuppressWarnings("unchecked")
    private ListNode<E>[] precreateListNodes(int size) {
        listNodesOffset = 0;

        ListNode<E>[] listArr = new ListNode[size];
        for(int i = 0; i < listArr.length; ++i) {
            listArr[i] = new ListNode<>(null);
        }

        return listArr;
    }


    private PrefixNode<E> createStructure(LayerType...layerTypes) {
        if (layerTypes.length < 1)
            throw new IllegalStateException("Can't have layers zero size");

        // We are at root, make a small array
        PrefixNode<E> node = new PrefixNode<>( ArrayIndexerFactory.get(layerTypes[0]));

        return createStructure(1, node, layerTypes );
    }

    private PrefixNode<E> createStructure(int level, PrefixNode<E> node, LayerType...layerTypes ) {

        if (level == layerTypes.length)
            return null;

        // last layer. add to layers list
        if ( // layerTypes.length > 0 &&
                level == layerTypes.length - 1) {
            for(int i = 0; i < node.getRefsArray().length; ++i) {
                node.getRefsArray()[i] = new TailNode();

            }

            leafNodesList.add(node);
            return node;
        }


        //System.err.println("level = " + levedef z = "q" - "a"l);
        //System.err.println("layers.size =  " + layerTypes.length );

        for(int i = 0; i < node.getRefsArray().length; ++i) {

            PrefixNode<E> child  = null;
            switch (layerTypes[level]) {
                case Alpha:
                    //System.err.println( "alpha: " + level );
                    child = new PrefixNode<>( ArrayIndexerFactory.small());
                    break;
                case AlphaNumeric:
                    //System.err.println( "alphanumeric: " + level );
                    child = new PrefixNode<>( ArrayIndexerFactory.big());
                    break;
                default:
                    //System.err.println( "level: " + level );
                    throw new IllegalStateException("unknown type: " + level);
            }

            if (child != null) {
                node.getRefsArray()[i] = child;
            }

            createStructure( level + 1, child, layerTypes);
        }

        return node;
    }

    public void add(E e, NameRecord.FieldGetter<E> fg) {
        root.append(e, 0, fg);
    }

    public void clear() {
        for(TrieNode node : root.getRefsArray()) {
            node.clear();
        }

        //leafNodesList.clear();
        listNodesOffset = 0;
    }

    public List<E> sort(NameRecord.FieldGetter<E> fg) {
        if (leafNodesList.size() == 0)
            return null;

        LinkedList<E> li = new LinkedList<>();

//        for(TrieNode tn : leafNodesList) {
//            li.addAll( tn.getSorted(fg) );
//        }
//
        for(PrefixNode<E> tn : leafNodesList) {
            for(TrieNode<E> t : tn.getRefsArray() ) {
                li.addAll(t.getSorted(fg));
            }
        }


        return li;
    }


    public List<E> getUnsortedResult() {

        List<E> li = new LinkedList<>();

        if (leafNodesList.size() == 0)
            return li;

//        int lim = 10;
//        int i = 0;

        //int[] distr = new int[1024];

        for(PrefixNode<E> tn : leafNodesList) {
            for(TrieNode<E> t : tn.getRefsArray()) {

                //distr[t.size()] ++;

                for(ListNode<E> node = t.getHead(); node != null; node = node.next) {
                    li.add( node.get() );
                }
            }
        }

//        System.err.println( "Big ones: " + bigOnes );

//        for(int i = 0; i < distr.length; ++i) {
//            System.err.printf( "[%4d]: %d\n", i, distr[i] );
//        }

        return li;
    }



    public double avgLeafSize() {
        double summ = 0.;
        int cnt = 0;
        for(PrefixNode tn : leafNodesList) {
            for(int i = 0; i < tn.getRefsArray().length; ++i) {
                summ += tn.getRefsArray()[i].size();
                cnt++;
            }
        }

        return summ / cnt;
    }

    public long totalResultSize() {
        long size = 0;
        for(PrefixNode tn : leafNodesList) {
            for(TrieNode t : tn.getRefsArray()) {
                size += t.size();
            }
        }

        return size;
    }


    public void sort( E[] arr,  NameRecord.FieldGetter<E> fg) {
        clear();

        //System.err.println(" arr size: " + arr.length);
        for(E e : arr) {
            add(e, fg);
        }

        //System.err.println("Total: " + TOTAL);
        //System.exit(1310);

        List<E> li = getUnsortedResult();
        Collections.sort( li, fg.getComparator() );
        li.toArray(arr);
        //Arrays.sort(arr, fg.getComparator() );
    }

    public void sortBy1stName( E[] a) {
        sort(a, new NameRecord.FirstNameGetter<E>() );
    }

    public void sortBy2ndName( E[] a) {
        sort(a, new NameRecord.FirstNameGetter<E>() );
    }
}
