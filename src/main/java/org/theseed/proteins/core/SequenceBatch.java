/**
 *
 */
package org.theseed.proteins.core;

import org.theseed.io.Shuffler;
import org.theseed.sequence.Sequence;

/**
 * This class accumulates a set of sequences for output, which can then be shuffled and written later.
 *
 * @author Bruce Parrello
 *
 */
public class SequenceBatch extends Shuffler<Sequence> {

    /**
     * serialization indicator
     */
    private static final long serialVersionUID = 5210893943653206805L;

    /**
     * Create an empty sequence batch.
     *
     * @param cap	initial allocation size
     */
    public SequenceBatch(int cap) {
        super(cap);
    }

    /**
     * @return the length of the longest sequence.
     */
    public int longest() {
        int retVal = 0;
        for (Sequence seq : this)
            retVal = Math.max(seq.length(), retVal);
        return retVal;
    }

}
