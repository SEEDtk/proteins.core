/**
 *
 */
package org.theseed.proteins.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.theseed.io.TabbedLineReader;
import org.theseed.sequence.FastaOutputStream;
import org.theseed.sequence.Sequence;
import org.theseed.utils.ICommand;

/**
 * This processor generates FASTA files from CoreSEED feature IDs.
 *
 * The list of pegs comes in via the standard input, in a tab-delimited file with headers.  The output is to the standard output.
 *
 * The command-line options are as follows:
 *
 * --col	 input column index (1-based) or name for feature IDs; the default is <code>1</code>
 * --input	 name of the input file; the default is the standard input
 * --comment input column index (1-based) or name for comments; the default is <code>2</code>
 *
 * The positional parameter is the location of the FIGdisk Organisms directory.
 *
 * @author Bruce Parrello
 *
 */
public class FeatureProcessor implements ICommand {

    // FIELDS
    /** peg input stream */
    private TabbedLineReader input;
    /** input stream */
    protected InputStream inStream;
    /** column index of peg IDs */
    private int colIdx;
    /** column index of comments */
    private int commentIdx;
    /** coreSEED manager */
    CoreUtilities coreSeed;

    // COMMAND-LINE OPTIONS

    /** help option */
    @Option(name = "-h", aliases = { "--help" }, help = true)
    private boolean help;
    /** TRUE if we want progress messages */
    @Option(name = "-v", aliases = { "--verbose", "--debug" }, usage = "display progress on STDERR")
    protected boolean debug;
    /** input column index */
    @Option(name = "--col", aliases = { "-c" }, metaVar="peg_id", usage="input column name or index (1-based)")
    private String column;
    /** comment column index */
    @Option(name = "--comment", aliases = { "-m" }, metaVar="notes", usage="comment column name or index (1-based)")
    private String comment;
    /** input file (if not using STDIN) */
    @Option(name = "--input", aliases = { "-i" }, usage = "input file (if not STDIN)")
    private File inFile;
    /** organisms directory */
    @Argument(index = 0, metaVar = "FIGdisk/FIG/Data/Organisms", usage = "organism directory containing SEED genomes", required = true)
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
        this.inFile = null;
        this.column = "1";
        this.comment = "2";
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

    public void run() {
        try {
            // Open the input stream and find the peg column and comment column.
            this.input = new TabbedLineReader(inStream);
            this.colIdx = this.input.findField(this.column);
            this.commentIdx = this.input.findField(this.comment);
            // Create a FASTA writer.
            try (FastaOutputStream outStream = new FastaOutputStream(System.out)) {
                // Read in the input file.
                if (debug) System.err.println("Reading peg IDs and comments.");
                for (TabbedLineReader.Line inLine : this.input) {
                    String pegId = inLine.get(colIdx);
                    String comment = inLine.get(commentIdx);
                    // Get the genome for this feature.
                    String genomeId = CoreUtilities.genomeOf(pegId);
                    PegList genomePegs = coreSeed.getGenomePegs(genomeId);
                    // Get the sequence for this peg.
                    Sequence pegSeq = genomePegs.get(pegId);
                    pegSeq.setComment(comment);
                    // Write it out.
                    outStream.write(pegSeq);
                }
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

}
