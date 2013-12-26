package com.hwbs.intertask.server;

import com.hwbs.intertask.shared.NameRecord;

import java.util.ArrayList;
import java.util.List;

/**
* User:      kaa
* Timestamp: 12/24/13 5:08 PM
*/
public class Slice {

    private int j,k;
    private NameRecord[] a;

    Slice(NameRecord[] a, int j, int k) {
        this.j = j;
        this.k = k;

        this.a = a;
    }

    public int length() {
        return k - j;
    }

    public NameRecord getItem(int index) {
        return a[j + index];
    }

    public List<NameRecord> getItems() {
        List<NameRecord> li = new ArrayList<>(k-j);

        for (int i = j; i < k; ++i ) {
            li.add(a[i]);
        }

        return li;
    }
}
