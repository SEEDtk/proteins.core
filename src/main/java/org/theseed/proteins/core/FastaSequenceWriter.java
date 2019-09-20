/**
 *
 */
package org.theseed.proteins.core;

import java.io.OutputStream;

/**
 * This class emits sequences in FASTA format.
 *
 * @author Bruce Parrello
 *
 */
public class FastaSequenceWriter extends SequenceWriter {

    public FastaSequenceWriter(OutputStream outStream) {
        super(outStream);
    }

    @Override
    public void write(String id, String comment, String seq) {
        this.println(">" + id + " " + comment);
        this.println(seq);
    }

}
