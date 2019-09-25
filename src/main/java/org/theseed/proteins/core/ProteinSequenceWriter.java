/**
 *
 */
package org.theseed.proteins.core;

import java.io.OutputStream;

import org.apache.commons.text.TextStringBuilder;

/**
 * This produces prediction input for the various protein processors.  The output file is in tab-delimited
 * format, but instead of a yes/no classification, the first column is the sequence ID, which can be used
 * as metadata to the neural net.
 *
 * @author Bruce Parrello
 *
 */
public class ProteinSequenceWriter extends SequenceWriter {

    // FIELDS
    /** number of output columns */
    private int len;
    /** buffer for assembling output */
    TextStringBuilder buffer;

    public ProteinSequenceWriter(OutputStream outStream, int max) {
        super(outStream);
        this.len = max;
        this.buffer = new TextStringBuilder(max * 4 + 10);
        // Format and write the header.
        this.buffer.append("seq_id\t");
        this.formatHeader(this.buffer, max, "\t");
        this.println(this.buffer.toString());
    }

    @Override
    public void write(String id, String comment, String seq) {
        this.buffer.setLength(0);
        this.buffer.append(id);
        this.buffer.append("\t");
        this.formatSeq(this.buffer, seq, this.len, "\t");
        this.println(this.buffer.toString());

    }

}
