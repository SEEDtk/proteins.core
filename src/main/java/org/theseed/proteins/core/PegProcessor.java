/**
 *
 */
package org.theseed.proteins.core;

import java.io.IOException;

import org.kohsuke.args4j.Option;
import org.theseed.io.TabbedLineReader;
import org.theseed.utils.ICommand;

/**
 * Create a file of proteins sequences from a FIGdisk and a list of PEG IDs.  The basic plan is to read the non-deleted features from
 * the <code>fasta<code> file.  The specified features will be included in the output with a "1" notation, and zero or more similar
 * features will be included in the output with a "0" notation.  The number of 0-features included is determined by a command-line
 * option-- the default is <code>1</code>.
 *
 * The list of pegs comes in via the standard input, in a tab-delimited file with headers.  The output is to the standard output.
 *
 * The command-line options are as follows:
 *
 * --col	input column index (1-based) or name; the default is <code>1</code>
 * --other	number of extra features to include for each specified feature
 * --fix	fixed output size to use in columnar modes; the default is to use the length of the largest specified sequence
 * --format	output format-- FASTA (yes/no flag in comment), ORANGE (comma-delimited columns with 0/1 flag last),
 * 			or DL4J (tab-delimited columns with yes/no column first)
 * --input	name of the input file; the default is the standard input
 *
 * The positional parameter is the location of the FIGdisk Organisms directory.
 *
 * @author Bruce Parrello
 *
 */
public class PegProcessor extends SetProcessor implements ICommand {

    // FIELDS
    /** peg input stream */
    private TabbedLineReader input;
    /** column index of peg IDs */
    private int colIdx;


    // COMMAND LINE

    /** input column index */
    @Option(name="--col", aliases={"-c"}, metaVar="peg_id", usage="input column name or index (1-based)")
    private String column;

    /**
     * Parse command-line options to specify the parameters of this object.
     *
     * @param args	an array of the command-line parameters and options
     *
     * @return TRUE if successful, FALSE if the parameters are invalid
     */
    public boolean parseCommand(String[] args) {
        // Set the default input column.
        this.column = "1";
        // Parse the command line.
        boolean retVal = parseArguments(args);
        return retVal;
    }

    public void run() {
        try {
            // Open the input stream and find the peg column.
            this.input = new TabbedLineReader(inStream);
            this.colIdx = this.input.findField(this.column);
            // Create the sequence batch for output.
            SequenceBatch seqBatch = new SequenceBatch(super.getBatchSize());
            // Read in all the input sequences.
            if (this.debug) System.err.println("Reading pegs from input.");
            for (TabbedLineReader.Line line : this.input) {
                String pegId = line.get(this.colIdx);
                this.addSequence(seqBatch, pegId);
            }
            // Produce the output from the pegs.
            processPegs(seqBatch, System.out);
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
}
