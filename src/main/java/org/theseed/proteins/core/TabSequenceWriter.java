/**
 *
 */
package org.theseed.proteins.core;

import java.io.OutputStream;

import org.apache.commons.text.TextStringBuilder;

/**
 * This class emits sequences in tab-delimited format with the classification in the first column.  "yes" is
 * used for <code>1</code>, "no" for <code>0</code>.  The output is fixed-width.
 *
 * @author Bruce Parrello
 *
 */
public class TabSequenceWriter extends SequenceWriter {

    // FIELDS
    /** number of output columns */
    private int len;
    /** buffer for assembling output */
    TextStringBuilder buffer;

    public TabSequenceWriter(OutputStream outStream, int max) {
        super(outStream);
        this.len = max;
        this.buffer = new TextStringBuilder(max * 4 + 10);
        // Format and write the header.
        this.buffer.append("found\t");
        this.formatHeader(this.buffer, max, "\t");
        this.println(this.buffer.toString());
    }

    @Override
    public void write(String id, String comment, String seq) {
        this.buffer.setLength(0);
        if (comment.contentEquals("0")) {
            this.buffer.append("0.0\t");
        } else {
            this.buffer.append("1.0\t");
        }
        this.formatSeq(this.buffer, seq, this.len, "\t");
        this.println(this.buffer.toString());
    }

}
