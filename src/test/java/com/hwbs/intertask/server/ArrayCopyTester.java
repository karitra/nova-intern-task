package com.hwbs.intertask.server;

import com.hwbs.intertask.shared.NameRecord;
import junit.framework.TestCase;

/**
 * User:      kaa
 * Timestamp: 12/23/13 9:09 PM
 */
public class ArrayCopyTester extends TestCase {
    public void testArrCopy() {
        NameRecord[] a = new NameRecord[] {
                new NameRecord(),
                new NameRecord(),
                new NameRecord()
        };

        a[0].firstName()[0] = 'a';
        a[0].firstName()[1] = 'b';
        a[0].firstName()[2] = 'c';
        a[0].setFirstEnd(3);

        a[0].secondName()[0] = 'a';
        a[0].secondName()[1] = 'b';
        a[0].secondName()[2] = 'c';
        a[0].secondName()[3] = 'd';
        a[0].setSecondEnd(4);

        a[1].firstName()[0] = 'x';
        a[1].firstName()[1] = 'y';
        a[1].firstName()[2] = 'z';
        a[1].setFirstEnd(3);

        a[2].firstName()[0] = 'i';
        a[2].firstName()[1] = 'l';
        a[2].firstName()[2] = 'i';
        a[2].firstName()[3] = 's';
        a[2].firstName()[4] = 'i';
        a[2].firstName()[5] = 'u';
        a[2].firstName()[6] = 'm';
        a[2].setFirstEnd(7);


        NameRecord[] b = ArrayDeepCopier.clone(a);

        assertEquals( b[0].firstName()[0],  a[0].firstName()[0]);
        assertEquals( b[0].secondName()[2], a[0].secondName()[2]);

        assertEquals( b[1].firstName()[1],  a[1].firstName()[1]);
        assertEquals( b[1].secondName()[0], a[1].secondName()[0]);

        assertEquals( b[2].firstName()[4],  a[2].firstName()[4]);
        assertEquals( b[2].firstName()[7],  a[2].firstName()[7]);
        assertEquals( b[2].secondName()[0], a[2].secondName()[0]);
    }
}
