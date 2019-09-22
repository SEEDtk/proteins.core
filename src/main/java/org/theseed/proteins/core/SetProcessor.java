package org.theseed.proteins.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.theseed.io.TabbedLineReader;
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

    /** current map of genome IDs to sequence lists */
    private Map<String, PegList> genomeMap;
    /** input stream */
    protected InputStream inStream;

    // CONSTANTS

    /** path-and-name suffix to convert a genome ID to the complete path to the peg FASTA file */
    private static final String PEG_FILE_SUFFIX = File.separator + "Features" + File.separator + "peg" + File.separator + "fasta";

    /** path-and-name suffix to convert a genome ID to the complete path to the assigned-functions file */
    private static final String FUNCTION_FILE_SUFFIX = File.separator + "assigned_functions";

    /** path-and-name suffix to convert a genome ID to the complete path to the deleted-pegs file */
    private static final String DELETED_PEGS_FILE_SUFFIX = File.separator + "Features" + File.separator + "peg" + File.separator + "deleted.features";

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
    /** number of input sequences to process in each batch */
    @Option(name = "--batch", aliases = { "--batchSize",
            "-b" }, metaVar = "1000", usage = "number of input sequences per batch")
    private int batchSize;
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
        this.batchSize = 100;
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
        int colMax = (this.maxCols == 0 ? seqBatch.longest() : this.maxCols);
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

    /**
     * @return the batch size used to estimate the pre-allocation size of each sequence batch
     */
    protected int getBatchSize() {
        return this.batchSize;
    }

    /**
     * @return an object for iterating through all the genomes
     */
    protected Iterable<String> getGenomes() {
        if (this.debug) System.err.println("Reading genomes from " + this.orgDir + ".");
        OrganismDirectories retVal = new OrganismDirectories(this.orgDir);
        if (this.debug) System.err.println(retVal.size() + " genomes found.");
        return retVal;
    }

    /**
     * @return a map of peg IDs to functions for a genome
     *
     * @param the genome ID
     *
     * @throws IOException
     */
    public Map<String, String> getGenomeFunctions(String genomeId) throws IOException {
        Map<String, String> retVal = new HashMap<String, String>(this.batchSize);
        // This set will hold the deleted features.
        Set<String> deletedPegs = new HashSet<String>(this.batchSize);
        File deleteFile = new File(this.orgDir, genomeId + DELETED_PEGS_FILE_SUFFIX);
        if (deleteFile.exists()) {
            if (this.debug) System.err.println("Reading deleted pegs for " + genomeId + ".");
            try (TabbedLineReader deleteReader = new TabbedLineReader(deleteFile, 1)) {
                for (TabbedLineReader.Line line : deleteReader) {
                    String peg = line.get(0);
                    deletedPegs.add(peg);
                }
            }
        }
        // Now, pull in all the un-deleted pegs, and map each peg to its function.  Because we are
        // storing the pegs in a map, only the last function will be kept, which is desired behavior.
        File functionFile = new File(this.orgDir, genomeId + FUNCTION_FILE_SUFFIX);
        try (TabbedLineReader functionReader = new TabbedLineReader(functionFile, 2)) {
            if (this.debug) System.err.println("Reading assigned functions for " + genomeId + ".");
            for (TabbedLineReader.Line line : functionReader) {
                String peg = line.get(0);
                if (peg.contains("peg") && ! deletedPegs.contains(peg)) {
                    retVal.put(peg, line.get(1));
                }
            }
        }
        return retVal;
    }

}
