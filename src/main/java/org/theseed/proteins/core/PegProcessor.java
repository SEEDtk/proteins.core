/**
 *
 */
package org.theseed.proteins.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.theseed.io.TabbedLineReader;
import org.theseed.sequence.Sequence;
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
public class PegProcessor implements ICommand {

    // FIELDS
    /** input stream */
    private TabbedLineReader input;
    /** column index of peg IDs */
    private int colIdx;
    /** output stream */
    private SequenceWriter output;
    /** current map of genome IDs to sequence lists */
    private Map<String, PegList> genomeMap;
    /** queue of sequences to output */
    private SequenceBatch seqBatch;

    // CONSTANTS
    /** path-and-name suffix to convert a genome ID to the complete path to the peg FASTA file */
    private static final String PEG_FILE_SUFFIX = File.separator + "Features" + File.separator + "peg" + File.separator + "fasta";

    // COMMAND LINE

    /** help option */
    @Option(name="-h", aliases={"--help"}, help=true)
    private boolean help;

    /** TRUE if we want progress messages */
    @Option(name="-v", aliases={"--verbose", "--debug"}, usage="display progress on STDERR")
    private boolean debug;

    /** input column index */
    @Option(name="--col", aliases={"-c"}, metaVar="peg_id", usage="input column name or index (1-based)")
    private String column;

    /** number of extra features per input feature */
    @Option(name="--other", aliases={"--extra", "--neg", "-x"}, metaVar="3", usage="number of extra features per input feature")
    private int extraFeatures;

    /** number of columns in columnar mode (excluding yes/no indicator */
    @Option(name="--fix", aliases={"--max", "-f"}, metaVar="100", usage="maximum data columns in columnar modes (0 to compute)")
    private int maxCols;

    /** output format */
    @Option(name="--format", aliases={"-o"}, usage="format for output sequences")
    private SequenceWriter.Type format;

    /** number of input sequences to process in each batch */
    @Option(name="--batch", aliases={"--batchSize", "-b"}, metaVar="1000", usage="number of input sequences per batch")
    private int batchSize;

    /** input file (if not using STDIN) */
    @Option(name="--input", aliases={"-i"}, usage="input file (if not STDIN)")
    private File inFile;

    /** organisms directory */
    @Argument(index=0, metaVar="FIGdisk/FIG/Data/Organisms", usage="organism directory containing SEED genomes", required=true)
    private File orgDir;

    /**
     * Parse command-line options to specify the parameters of this object.
     *
     * @param args	an array of the command-line parameters and options
     *
     * @return TRUE if successful, FALSE if the parameters are invalid
     */
    public boolean parseCommand(String[] args) {
        boolean retVal = false;
        // Set the defaults.
        this.help = false;
        this.debug = false;
        this.batchSize = 100;
        this.column = "1";
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
                // Open the standard input and find the input column.
                InputStream inStream = System.in;
                if (this.inFile != null)
                    inStream = new FileInputStream(this.inFile);
                this.input = new TabbedLineReader(inStream);
                this.colIdx = this.input.findField(this.column);
                // Denote we have not created the output stream yet.
                this.output = null;
                // Create the sequence storage buffer.
                this.seqBatch = new SequenceBatch(this.batchSize * (1 + this.extraFeatures));
                // Create the genome map.
                this.genomeMap = new HashMap<String, PegList>(this.batchSize);
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

    public void run() {
        try {
            // Read in all the input sequences.
            if (this.debug) System.err.println("Reading pegs from input.");
            for (TabbedLineReader.Line line : this.input) {
                String pegId = line.get(this.colIdx);
                this.addSequence(pegId);
            }
            // Produce the output from the pegs.
            processPegs();
            // Close and flush the output.
            this.output.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    /**
     * Shuffle the input sequences and write them out along with the specified number
     * of neighbors.
     *
     * @throws IOException
     */
    protected void processPegs() throws IOException {
        int sequenceCount = this.seqBatch.size();
        if (this.debug) System.err.println("Shuffling " + sequenceCount + " input sequences.");
        this.seqBatch.shuffle(sequenceCount);
        // Create a buffer for counter-example sequences.
        ArrayList<Sequence> buffer = new ArrayList<Sequence>(this.extraFeatures);
        // Open the output stream.
        if (this.maxCols == 0) this.maxCols = this.seqBatch.longest();
        if (this.debug) System.err.println("Producing output with column bias of " + this.maxCols + ".");
        this.output = SequenceWriter.create(this.format, System.out, this.maxCols);
        for (Sequence seq : this.seqBatch) {
            // First, write this sequence.
            this.output.write(seq);
            // Get the pegs for this sequence's genome.
            String seqId = seq.getLabel();
            PegList genomePegs = this.getGenome(seqId);
            // Now find the extras.  These are written as counter-examples.
            buffer.clear();
            genomePegs.findClose(seq, this.extraFeatures, buffer);
            for (Sequence seq2 : buffer) {
                seq2.setComment("0");
                this.output.write(seq2);
                // Insure the peg isn't re-used.
                genomePegs.suppress(seq2);
            }
        }
    }

    /**
     * Find an input sequence and add it to the output queue.
     *
     * @param pegId		ID of the input sequence
     *
     * @throws IOException
     */
    public void addSequence(String pegId) throws IOException {
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
                this.storeSequence(targetPeg, genomePegs, "1");
            }
        }
    }

    /**
     * Add a sequence to the current batch with the specified comment.
     *
     * @param targetPeg		sequence object for a specified target peg
     * @param genomePegs	peg list for the source genome
     * @param comment		comment to be stored with the sequence
     */
    private void storeSequence(Sequence targetPeg, PegList genomePegs, String comment) {
        targetPeg.setComment(comment);
        this.seqBatch.add(targetPeg);
        // Blacklist the sequence so we don't reuse it.
        genomePegs.suppress(targetPeg);
    }

    /**
     * @return the list of pegs for the genome identified in a peg ID, or NULL if no such genome exists
     *
     * @param pegId		ID of the relevant peg
     *
     * @throws IOException
     */
    public PegList getGenome(String pegId) throws IOException {
        // To get the genome ID, we pull out everything between "fig|" and ".peg".
        String genomeId = StringUtils.substringBetween(pegId, "fig|", ".peg");
        // Try to find the genome in the cache.
        PegList retVal = this.genomeMap.get(genomeId);
        if (retVal == null) {
            // Here we have to read the genome in.
            File pegFile = new File(this.orgDir, genomeId + PEG_FILE_SUFFIX);
            if (! pegFile.isFile()) {
                // The genome is not found, so we leave our return value NULL.
                if (this.debug) System.err.println("Genome not found for input peg " + pegId + ".");
            } else {
                // Here the genome does exist.
                if (this.debug) System.err.println("Reading genome " + genomeId + ".");
                retVal = new PegList(pegFile);
                // Cache the genome in case it comes up again.
                this.genomeMap.put(genomeId, retVal);
            }
        }
        return retVal;
    }
}
