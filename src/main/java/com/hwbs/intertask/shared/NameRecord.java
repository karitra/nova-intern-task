package com.hwbs.intertask.shared;

import java.util.Comparator;

/**
 * User:      kaa
 * Timestamp: 12/21/13 7:51 PM
 */
public class NameRecord {


    public enum NameNum {
        FIRST_NAME,
        SECOND_NAME
    };

    //
    // Note: should be in some config in real app
    //
    public static final int MAX_CHARS_IN_NAME = 10;
    public static final int MIN_CHARS_IN_NAME =  5;
    public static final int NAME_FIELDS       =  2;

    long id;

    private char[] first;
    private char[] second;

    private int firstEnd;
    private int secondEnd;

    public NameRecord(long id ,String fs, String ss) {
        this(fs, ss);
        this.id = id;
    }

    public NameRecord(String fs, String ss) {
        this();

        setFirst(fs);
        setSecond(ss);
    }

    public NameRecord() {
        id     = 0;

        first  = new char[ MAX_CHARS_IN_NAME ];
        second = new char[ MAX_CHARS_IN_NAME ];
    }

    public Long getId() {
        return id;
    }

    public final char[] firstName() {
        return first;
    }

    public final char[] secondName() {
        return second;
    }


    public int getFirstEnd() {
        return firstEnd;
    }

    public void setFirstEnd(int firstEnd) {
        this.firstEnd = firstEnd;
    }

    public int getSecondEnd() {
        return secondEnd;
    }

    public void setSecondEnd(int secondEnd) {
        this.secondEnd = secondEnd;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(MAX_CHARS_IN_NAME * 2 + 1);

        return sb.append(first, 0, firstEnd)
            .append(' ')
            .append(second, 0, secondEnd).toString();
    }

    public void setFirst(String name) {
        firstEnd = setFromString(first, name);
    }


    public void setSecond(String name) {
        secondEnd = setFromString(second, name);
    }

    public void setNames(String first, String second) {
        setFirst(first);
        setSecond(second);
    }

    private int setFromString(char[] a, String name) {
        char[] ca = name.toCharArray();

        int i;
        for(i = 0; i < ca.length && i < a.length; ++i) {
            a[i] = ca[i];
        }

        return i;
    }


    public String getFirstName() {
        return new String( firstName(), 0, getFirstEnd());
    }

    public String getSecondName() {
        return new String( secondName(), 0, getSecondEnd());
    }


    public NameRecord clone() {
        NameRecord copy = new NameRecord();

        copy.setFirstEnd(this.getFirstEnd());
        copy.setSecondEnd(this.getSecondEnd());

        System.arraycopy(this.first,  0, copy.first,  0, MAX_CHARS_IN_NAME );
        System.arraycopy(this.second, 0, copy.second, 0, MAX_CHARS_IN_NAME );

        // System.err.println(" this: " + this.toString() );
        // System.err.println(" copy: " + this.toString() );

        return copy;
    }


    /**
     * For counting sorting. Field adapter
     *
     */
    public interface FieldGetter<T extends NameRecord> {
         Comparator<T> getComparator();
         char[] get(NameRecord a);
    }

    public static class FirstNameGetter<T extends NameRecord> implements FieldGetter<T> {

        Comparator<T> cmp;

        public FirstNameGetter() {
            this(0);
        }


        public FirstNameGetter(int offset) {
            cmp = new FirstNameComparator<T>(offset);
        }

        @Override
        public final Comparator<T> getComparator() {
            return cmp;
        }

        @Override
        public final char[] get(NameRecord a) {
            return a.firstName();
        }
    }

    public static class SecondNameGetter<T extends NameRecord> implements FieldGetter<T> {

        Comparator<T> cmp;

        public SecondNameGetter() {
            this(0);
        }

        public SecondNameGetter(int offset) {
            cmp = new SecondNameComparator<T>(offset);
        }


        @Override
        public final Comparator<T> getComparator() {
            return cmp;
        }


        public final char[] get(NameRecord a) {
            return a.secondName();
        }
    }


    private static class BasicComparator {

        protected char[] a,b;
        protected int alen, blen, i, min;
        protected boolean altb, agtb;
        protected int offset;

        BasicComparator(int offset) {
            this.offset = offset;
        }

        protected int cmpAsc() {

            if (alen < blen) {
                min  = alen;
                altb = true;
            } else {
                min  = blen;
                altb = false;
            }

            for(i = offset; i < min; ++i) {
                if (a[i] < b[i]) {
                    return -1;
                } else if (a[i] > b[i]) {
                    return 1;
                }
            }

            if (alen == blen)
                return 0;
            else if (altb)
                return -1;

            return 1;
        }

        protected int cmpDesc() {

            if (alen < blen) {
                min  = alen;
                agtb = false;
            } else {
                min  = blen;
                agtb = true;
            }

            for(i = offset; i < min; ++i) {
                if (a[i] > b[i]) {
                    return -1;
                } else if (a[i] < b[i]) {
                    return 1;
                }
            }

            if (alen == blen)
                return 0;
            else if (agtb)
                return -1;

            return 1;
        }

        protected int cmp() {
            return cmpAsc();
        }
    }

    //
    // Note: following two comparator should be not used as static fields, as they are stateful
    // and therefor not thread safe
    //
    public static class FirstNameComparator<T extends NameRecord> extends BasicComparator implements  Comparator<T> {

        public FirstNameComparator() {
            super(0);
        }

        public FirstNameComparator(int offset) {
            super(offset);
        }


        @Override
        public int compare(T o1, T o2) {
            a    = o1.firstName();
            alen = o1.getFirstEnd();

            b    = o2.firstName();
            blen = o2.getFirstEnd();

            return cmp();
        }
    }

    public static class SecondNameComparator<T extends NameRecord> extends BasicComparator implements Comparator<T> {

        public SecondNameComparator() {
            super(0);
        }

        public SecondNameComparator(int offset) {
            super(offset);
        }


        @Override
        public int compare(NameRecord o1, NameRecord o2) {
            a    = o1.secondName();
            alen = o1.getSecondEnd();

            b    = o2.secondName();
            blen = o2.getSecondEnd();

            return cmp();
        }
    }

    /*
    public static class SecondNameComparatorDesc extends SecondNameComparator {
        @Override
        protected int cmp() {
            return cmpDesc();
        }
    }

    public static class FirstNameComparatorDesc extends FirstNameComparator {
        @Override
        protected int cmp() {
            return cmpDesc();
        }
    } */




}
