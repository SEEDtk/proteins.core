package org.theseed.proteins.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.theseed.sequence.Sequence;

/**
 * This is the base class for creating training sets from peg lists.  The subclass determines which pegs are to be written and
 * where they should go.  We do the rest.
 *
 * The list of pegs comes in via the standard input, in a tab-delimited file with headers.  The output is to the standard output.
 *
 * The command-line options are as follows:
 *
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
public class SetProcessor {

    // FIELDS

    /** input stream */
    protected InputStream inStream;
    /** coreSEED manager */
    CoreUtilities coreSeed;


    // COMMAND-LINE OPTIONS

    /** help option */
    @Option(name = "-h", aliases = { "--help" }, help = true)
    private boolean help;
    /** TRUE if we want progress messages */
    @Option(name = "-v", aliases = { "--verbose", "--debug" }, usage = "display progress on STDERR")
    protected boolean debug;
    /** number of extra features per input feature */
    @Option(name = "--other", aliases = { "--extra", "--neg",
            "-x" }, metaVar = "3", usage = "number of extra features per input feature")
    private int extraFeatures;
    /** number of columns in columnar mode (excluding yes/no indicator */
    @Option(name = "--fix", aliases = { "--max",
            "-f" }, metaVar = "100", usage = "maximum data columns in columnar modes (0 to compute)")
    private int maxCols;
    /** output format */
    @Option(name = "--format", aliases = { "-o" }, usage = "format for output sequences")
    private SequenceWriter.Type format;
    /** input file (if not using STDIN) */
    @Option(name = "--input", aliases = { "-i" }, usage = "input file (if not STDIN)")
    private File inFile;
    /** organisms directory */
    @Argument(index = 0, metaVar = "FIGdisk/FIG/Data/Organisms", usage = "organism directory containing SEED genomes", required = true)
    private File orgDir;

    /** Construct the base class for the two processors. */
    public SetProcessor() {
        super();
    }

    /**
     * Parse the command-line options and arguments.
     *
     * @param args	parameters from the command line
     *
     * @return TRUE if successful, FALSE if the command should not be run
     */
    protected boolean parseArguments(String[] args) {
        boolean retVal = false;
        // Set the defaults.
        this.help = false;
        this.debug = false;
        this.extraFeatures = 1;
        this.format = SequenceWriter.Type.FASTA;
        this.maxCols = 0;
        this.inFile = null;
        // Parse the command line.
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
            if (this.help) {
                parser.printUsage(System.err);
            } else {
                // Insure the organism directory exists.
                if (! this.orgDir.isDirectory())
                    throw new FileNotFoundException("Organism directory " + this.orgDir + " not found or invalid.");
                // Save the input stream.
                this.inStream = System.in;
                if (this.inFile != null)
                    this.inStream = new FileInputStream(this.inFile);
                // Create the coreSEED manager.
                this.coreSeed = new CoreUtilities(this.debug, this.orgDir);
                // We made it this far, we can run the application.
                retVal = true;
            }
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            // For parameter errors, we display the command usage.
            parser.printUsage(System.err);
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
        return retVal;
    }

    /**
     * Shuffle the input sequences and write them out along with the specified number
     * of neighbors.
     *
     * @param outStream		stream to which output should be written
     *
     * @throws IOException
     */
    protected void processPegs(SequenceBatch seqBatch, OutputStream outStream) throws IOException {
        int sequenceCount = seqBatch.size();
        if (this.debug) System.err.println("Shuffling " + sequenceCount + " input sequences.");
        seqBatch.shuffle(sequenceCount);
        // Create a buffer for counter-example sequences.
        ArrayList<Sequence> buffer = new ArrayList<Sequence>(this.extraFeatures);
        // Open the output stream.
        int colMax = (this.maxCols == 0 ? seqBatch.longest() + 1 : this.maxCols);
        if (this.debug) System.err.println("Producing output with column bias of " + colMax + ".");
        SequenceWriter output = SequenceWriter.create(this.format, outStream, colMax);
        for (Sequence seq : seqBatch) {
            // First, write this sequence.
            output.write(seq);
            // Get the pegs for this sequence's genome.
            String seqId = seq.getLabel();
            PegList genomePegs = this.getGenome(seqId);
            // Now find the extras.  These are written as counter-examples.
            buffer.clear();
            genomePegs.findClose(seq, this.extraFeatures, buffer);
            for (Sequence seq2 : buffer) {
                seq2.setComment("0");
                output.write(seq2);
                // Insure the peg isn't re-used.
                genomePegs.suppress(seq2);
            }
        }
        // Close and flush the output.
        output.close();
    }

    /**
     * Find an input sequence and add it to the output queue.
     *
     * @param seqBatch	output queue for sequences
     * @param pegId		ID of the input sequence
     *
     * @throws IOException
     */
    protected void addSequence(SequenceBatch seqBatch, String pegId) throws IOException {
        // Get the pegs for the relevant genome.
        PegList genomePegs = this.getGenome(pegId);
        // Only proceed if the peg belongs to a valid genome.
        if (genomePegs != null) {
            // Get the sequence for the specified peg.
            Sequence targetPeg = genomePegs.get(pegId);
            // Make sure we found it.
            if (targetPeg == null) {
                if (this.debug) System.err.println(pegId + " not found in genome.");
            } else {
                // Add the sequence to the sequence batch.
                seqBatch.storeSequence(targetPeg, genomePegs, "1");
            }
        }
    }


    /**
     * @return the list of pegs for the genome identified in a peg ID, or NULL if no such genome exists
     *
     * @param pegId		ID of the relevant peg
     *
     * @throws IOException
     */
    protected PegList getGenome(String pegId) throws IOException {
        // To get the genome ID, we pull out everything between "fig|" and ".peg".
        String genomeId = StringUtils.substringBetween(pegId, "fig|", ".peg");
        // Try to find the genome in the cache.
        PegList retVal = this.coreSeed.getGenomePegs(genomeId);
        return retVal;
    }


    /**
     * @return the appropriate suffix for output files
     */
    public String getOutputSuffix() {
        String retVal = null;
        switch (this.format) {
        case FASTA :
            retVal = ".fa";
            break;
        case DL4J :
            retVal = ".tbl";
            break;
        case ORANGE :
            retVal = ".csv";
            break;
        }
        return retVal;
    }

    /**
     * @return an object for iterating through all the genomes
     */
    protected Iterable<String> getGenomes() {
        return this.coreSeed.getGenomes();
    }

    /**
     * Get a map from peg IDs to functions for a genome.
     *
     * @param genomeId	ID of the genome of interest
     *
     * @return a map from each peg ID to its assigned function
     *
     * @throws IOException
     */
    protected Map<String, String> getGenomeFunctions(String genomeId) throws IOException {
        return this.coreSeed.getGenomeFunctions(genomeId);
    }

}
