package com.hwbs.intertask.server.generator.mt;

import com.hwbs.intertask.shared.NameRecord;

import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
* User:      kaa
* Timestamp: 12/22/13 2:15 PM
*/
public class RandGenTask implements Runnable {

    //
    // Logging context
    //
    Logger logger = Logger.getLogger("nviewer");

    private static final int LATIN_ALPHA_NUM = 26;
    private static final int OFFESTS_DELTA   = NameRecord.MAX_CHARS_IN_NAME - NameRecord.MIN_CHARS_IN_NAME;

    private static final char[] charset = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final char[] alpha   = new char[LATIN_ALPHA_NUM];
    static {
        for(int i = 0; i < LATIN_ALPHA_NUM; i++) {
            alpha[i] = Character.toUpperCase(charset[i+10]);
        }
    }

    private int j,k;
    // Indexes in generated string for each character in name (first name and second name)
    private int[][] indexes;
    // offset for latter in name string starting from 5 to 10
    private int[]   offsets;
    //private Random random;


    final NameRecord[] cache;

    public RandGenTask(NameRecord[] cache, int j, int k) {

        //random = new Random();

        this.cache = cache;

        this.j = j;
        this.k = k;

        this.indexes = new int[NameRecord.NAME_FIELDS][NameRecord.MAX_CHARS_IN_NAME];
        this.offsets = new int[NameRecord.NAME_FIELDS];

        fillIndexes();
        fillOffsets();
    }

    @Override
    public void run() {
        //logger.log( Level.INFO, "thread with range (" + j +"," + k + ") starting");

         for(int i = j; i < k; ++i) {
             char[] first  = cache[i].firstName();
             char[] second = cache[i].secondName();

             cache[i].setFirstEnd( gen(cache[i].firstName(),  NameRecord.NameNum.FIRST_NAME.ordinal() ));
             cache[i].setSecondEnd(gen(cache[i].secondName(), NameRecord.NameNum.SECOND_NAME.ordinal() ));
         }

        //logger.log( Level.INFO, "thread with range (" + j +"," + k + ") done");
    }

    private static final int RESEED_STEP = LATIN_ALPHA_NUM / 12;

    private int gen(char[] s, int t) {
        // generate name length
        int lim = (t==0) ?  NameRecord.MIN_CHARS_IN_NAME + offsets[t]++ % (OFFESTS_DELTA + 1) :
                            NameRecord.MAX_CHARS_IN_NAME - offsets[t]++ % (OFFESTS_DELTA + 1);


        // set cappa
        s[0] = alpha[indexes[t][0]++ % alpha.length];
        if (indexes[t][0] % RESEED_STEP == 0) {
            //indexes[t][0] = random.nextInt(alpha.length);
            indexes[t][0] = ThreadLocalRandom.current().nextInt(alpha.length);
        }

        // set reminder
        for(int i = 1; i < lim; ++i) {
            s[i] = charset[indexes[t][i]++ % charset.length];
            if (indexes[t][i] % RESEED_STEP == 0) {
                indexes[t][i] = ThreadLocalRandom.current().nextInt(charset.length);
            }
        }

        return lim;
    }

    private void fillIndexes() {
        //Random r = new Random();
        for(int n = 0; n < indexes.length; ++n) {
            for (int i = 1; i < indexes[n].length; ++i) {
                //indexes[n][i] = r.nextInt(charset.length);
                indexes[n][i] = ThreadLocalRandom.current().nextInt(charset.length);
            }

            // correct first letter as it should contain only latter
            //indexes[n][0] = r.nextInt(LATIN_ALPHA_NUM);
            indexes[n][0] = ThreadLocalRandom.current().nextInt(LATIN_ALPHA_NUM);
        }
    }

    private void fillOffsets() {
        //Random r = new Random();
        for (int i = 0; i < offsets.length; ++i) {
            //offsets[i] = r.nextInt(OFFESTS_DELTA);
            offsets[i] = ThreadLocalRandom.current().nextInt(OFFESTS_DELTA);
        }
    }

}
