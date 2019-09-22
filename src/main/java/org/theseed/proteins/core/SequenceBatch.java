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

    /**
     * Add a sequence to this batch with the specified comment and blacklist it
     * in the specified peg list.
     *
     * @param targetPeg		sequence object for a specified target peg
     * @param genomePegs	peg list for the source genome
     * @param comment		comment to be stored with the sequence
     */
    public void storeSequence(Sequence targetPeg, PegList genomePegs, String comment) {
        targetPeg.setComment(comment);
        this.add(targetPeg);
        // Blacklist the sequence so we don't reuse it.
        genomePegs.suppress(targetPeg);
    }

}
