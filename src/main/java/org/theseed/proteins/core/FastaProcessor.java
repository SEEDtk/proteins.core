/**
 *
 */
package org.theseed.proteins.core;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.theseed.counters.Shuffler;
import org.theseed.sequence.FastaInputStream;
import org.theseed.sequence.Sequence;
import org.theseed.utils.ICommand;

/**
 * This class creates one or more input files for the neural nets designed to detect function.  It takes as input
 * a FASTA file of protein sequences and then processes them into input files for the neural nets.
 * The output format will be the tab-delimited DL4J style.  A control file should specify the name and input width
 * of each neural net.  The output files will be in the specified output directory and will have the name of the
 * neural net with a suffix of <code>.tbl</code>.
 *
 * The positional parameters are the name of the output directory and the name of the control file.
 *
 * The command-line options are as follows.
 *
 * --input	name of the input file; the default is the standard input
 *
 * @author Bruce Parrello
 *
 */
public class FastaProcessor extends PredictProcessor implements ICommand {

    // FIELDS
    /** input stream */
    private FastaInputStream inStream;


    // COMMAND-LINE OPTIONS

    /** input file (if not using STDIN) */
    @Option(name = "--input", aliases = { "-i" }, usage = "input file (if not STDIN)")
    private File inFile;

    @Override
    public boolean parseCommand(String[] args) {
        boolean retVal = false;
        // Set the defaults.
        this.help = false;
        this.debug = false;
        this.inFile = null;
        // Parse the command line.
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
            if (this.help) {
                parser.printUsage(System.err);
            } else {
                // Set up the control file and the output directory.
                initializeOutput();
                // Set up the input file.
                if (this.inFile == null) {
                    this.inStream = new FastaInputStream(System.in);
                } else {
                    this.inStream = new FastaInputStream(this.inFile);
                }
                // If we got this far, we are ready to run.
                retVal = true;
            }
        } catch (CmdLineException e) {
            System.err.println(e.toString());
            // For parameter errors, we display the command usage.
            parser.printUsage(System.err);
        } catch (IOException e) {
            System.err.println(e.toString());
        }
        return retVal;
    }

    @Override
    public void run() {
        try {
            // Loop through the input, collecting the sequences.
            if (this.debug) System.err.println("Reading input sequences.");
            List<Sequence> inSequences = new Shuffler<Sequence>(1000).addSequence(this.inStream);
            if (this.debug) System.err.println(inSequences.size() + " input sequences found.");
            // Output the sequences.
            writeSequences(inSequences);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

}
