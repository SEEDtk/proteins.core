/**
 *
 */
package org.theseed.proteins.core;

import java.io.OutputStream;

import org.apache.commons.text.TextStringBuilder;

/**
 * This class emits sequences in comma-delimited format with the classification at the end.
 * The output is fixed-width.
 *
 * @author Bruce Parrello
 *
 */
public class CommaSequenceWriter extends SequenceWriter {

    // FIELDS
    /** number of output columns */
    private int len;
    /** buffer for assembling output */
    TextStringBuilder buffer;

    public CommaSequenceWriter(OutputStream outStream, int max) {
        super(outStream);
        this.len = max;
        this.buffer = new TextStringBuilder(max * 4 + 10);
        // Format and write the header.
        this.formatHeader(this.buffer, max, ",");
        this.buffer.append(",class");
        this.println(this.buffer.toString());
    }

    @Override
    public void write(String id, String comment, String seq) {
        this.buffer.setLength(0);
        this.formatSeq(this.buffer, seq, this.len, ",");
        this.buffer.append(",");
        this.buffer.append(comment);
        this.println(this.buffer.toString());
    }

}
