/**
 *
 */
package org.theseed.proteins.core;

import java.io.Closeable;
import java.io.OutputStream;
import java.io.PrintWriter;

import org.apache.commons.text.TextStringBuilder;
import org.theseed.sequence.Sequence;

/**
 * This is the base class for writing proteins.  Each supported format is a subclass.  The input
 * to each write request is an ID, sequence, and comment.  The output is to a client-provided text
 * stream.
 *
 * @author Bruce Parrello
 *
 */
public abstract class SequenceWriter implements Closeable, AutoCloseable {

    // FIELDS
    /** open output stream */
    PrintWriter outStream;

    /** output formats */
    public static enum Type {
        /** FASTA file */
        FASTA,
        /** comma-delimited, fixed width, 0/1 last */
        ORANGE,
        /** tab-delimited, fixed width, 1.0/0.0 first */
        DL4J
    };

    /**
     * Open a sequence writer for a specified stream.
     *
     * @param outStream	target output stream
     */
    public SequenceWriter(OutputStream outStream) {
        this.outStream = new PrintWriter(outStream);
    }

    /**
     * Create a sequence writer of the specified type.
     *
     * @param type		type of output desired
     * @oaram outStream	target output stream
     * @param max		maximum sequence length
     */
    public static SequenceWriter create(Type type, OutputStream outStream, int max) {
        SequenceWriter retVal = null;
        switch (type) {
        case FASTA :
            retVal = new FastaSequenceWriter(outStream);
            break;
        case ORANGE :
            retVal = new CommaSequenceWriter(outStream, max);
            break;
        case DL4J :
            retVal = new TabSequenceWriter(outStream, max);
            break;
        }
        return retVal;
    }

    /**
     * Write out a sequence record.
     *
     * @param id		sequence identifier
     * @param comment	sequence comment
     * @param seq		sequence letters
     */
    public abstract void write(String id, String comment, String seq);

    /** Write out a sequence.
     *
     * @param sequence	sequence object representing the sequence to write
     */
    public void write(Sequence sequence) {
        write(sequence.getLabel(), sequence.getComment(), sequence.getSequence());
    }

    /**
     * Output a line of text.
     *
     * @param line	line to write
     */
    protected void println(String line) {
        this.outStream.println(line);
    }

    /**
     * Format a sequence for one of the delimited writers.  This involves fitting
     * it to the maximum length and inserting the delimiters themselves.
     *
     * @param buffer	output string buffer
     * @param sequence	sequence to format
     * @param len		fixed sequence length
     * @param delim		delimiter string
     *
     * @return the formatted sequence
     */
    protected void formatSeq(TextStringBuilder buffer, String sequence, int len, String delim) {
        int n = sequence.length();
        char chr = (n > 0 ? sequence.charAt(0) : '-');
        buffer.append(chr);
        for (int i = 1; i < len; i++) {
            if (i < n)
                chr = sequence.charAt(i);
            else if (i == n)
                chr = '*';
            else
                chr = '-';
            buffer.append(delim);
            buffer.append(chr);
        }
    }

    /**
     * Format a header for one of the delimited writers.  Only the sequence part of
     * the header is formatted, so the class has to be added either before or after.
     *
     * @oaran buffer	string output buffer
     * @param len		fixed sequence length
     * @param delim		delimiter string
     */
    protected void formatHeader(TextStringBuilder buffer, int len, String delim) {
        buffer.append("p1");
        String prefix = delim + "p";
        for (int i = 2; i <= len; i++) {
            buffer.append(prefix);
            buffer.append(i);
        }
    }

    /**
     * Flush and close the output stream.
     */
    @Override
    public void close() {
        this.outStream.close();
    }

}
