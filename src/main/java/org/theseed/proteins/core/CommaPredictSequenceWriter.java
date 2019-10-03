/**
 *
 */
package org.theseed.proteins.core;

import java.io.OutputStream;

import org.apache.commons.text.TextStringBuilder;

/**
 * @author parrello
 *
 */
public class CommaPredictSequenceWriter extends SequenceWriter {

    // FIELDS
    /** number of output columns */
    private int len;
    /** buffer for assembling output */
    TextStringBuilder buffer;

    public CommaPredictSequenceWriter(OutputStream outStream, int max) {
        super(outStream);
        this.len = max;
        this.buffer = new TextStringBuilder(max * 4 + 10);
        // Format and write the header.
        this.formatHeader(this.buffer, max, ",");
        this.buffer.append(",peg_id");
        this.println(this.buffer.toString());
    }

    @Override
    public void write(String id, String comment, String seq) {
        this.buffer.setLength(0);
        this.formatSeq(this.buffer, seq, this.len, ",");
        this.buffer.append(",");
        this.buffer.append(id);
        this.println(this.buffer.toString());
    }

}
