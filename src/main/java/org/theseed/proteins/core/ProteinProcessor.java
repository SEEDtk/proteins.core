/**
 *
 */
package org.theseed.proteins.core;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.theseed.genome.Feature;
import org.theseed.io.TabbedLineReader;
import org.theseed.proteins.Role;
import org.theseed.proteins.RoleMap;
import org.theseed.sequence.Sequence;
import org.theseed.utils.ICommand;

/**
 * This class creates one or more input files for the neural nets designed to detect function.  It takes as input
 * a list of roles and then selects pegs with those roles and processes them into input files for the neural nets.
 * The output format will be the tab-delimited DL4J style.  A control file should specify the name and input width
 * of each neural net.  The output files will be in the specified output directory and will have the name of the
 * neural net with a suffix of <code>.tbl</code>.
 *
 * The positional parameters are the name of the output directory and the name of the control file.
 *
 * The command-line options are as follows.
 *
 * --input		name of the input file; this should be tab-delimited with no headers, and contain role names;
 * 				the default is the standard input
 * --col		index (1-based) of the input file column containing roles names; the default is <code>0</code>,
 * 				indicating the last column
 * --reverse	normally, the pegs for the roles in the input file will be selected; if this option is specified,
 * 				all pegs but the ones with the input roles will be selected
 * --core		name of the coreSEED Organisms directory; the default is <code>FIGdisk/FIG/Data/Organisms</code>
 * 				in the current directory
 * --single		if specified, only singleton roles are considered to match
 *
 *
 * @author Bruce Parrello
 *
 */
public class ProteinProcessor extends PredictProcessor implements ICommand {

    // FIELDS
    /** map of roles of interest */
    private RoleMap roleMap;
    /** coreSEED manager */
    private CoreUtilities coreSeed;

    // COMMAND-LINE OPTIONS

    /** input file (if not using STDIN) */
    @Option(name = "--input", aliases = { "-i" }, usage = "input file (if not STDIN)")
    private File inFile;

    /** column of input file containing role name */
    @Option(name = "--col", aliases = { "-c" }, metaVar = "3", usage = "role name input column (1-based)")
    private String roleCol;

    /** coreSEED organism directory */
    @Option(name = "--core", metaVar = "/vol/core-seed/FIGdisk/FIG/Data/Organisms", usage = "CoreSEED organism directory")
    private File orgDir;

    /** include/exclude flag */
    @Option(name = "--reverse", aliases = { "-r" }, usage = "if specified, input roles will be excluded instead of selected")
    private boolean reverse;

    /** singleton flag */
    @Option(name = "--single", aliases = { "-s", "-u" }, usage = "if specified, only singly-functional proteins will be output")
    private boolean single;


    @Override
    public boolean parseCommand(String[] args) {
        boolean retVal = false;
        // Set the defaults.
        this.help = false;
        this.debug = false;
        this.inFile = null;
        this.orgDir = new File("FIGdisk/FIG/Data/Organisms");
        this.reverse = false;
        this.single = false;
        this.roleCol = "0";
        // Parse the command line.
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
            if (this.help) {
                parser.printUsage(System.err);
            } else {
                // Set up the control file and the output directory.
                initializeOutput();
                // Create the role map.
                this.roleMap = new RoleMap();
                // Set up the input file.
                if (this.debug) System.err.println("Reading input roles.");
                TabbedLineReader inStream;
                if (this.inFile == null)
                    inStream = new TabbedLineReader(System.in);
                else
                    inStream = new TabbedLineReader(this.inFile);
                try {
                    // Read the role names and add them to the map.
                    int colIdx = inStream.findField(this.roleCol);
                    for (TabbedLineReader.Line line : inStream) {
                        String role = line.get(colIdx);
                        roleMap.register(role);
                    }
                    if (this.debug) System.err.println(this.roleMap.fullSize() + " roles found in input.");
                } finally {
                    inStream.close();
                }
                // Verify the organism directory.
                if (! this.orgDir.isDirectory())
                    throw new IOException(this.orgDir + " is not a valid directory.");
                // Connect to the coreSEED.
                this.coreSeed = new CoreUtilities(this.debug, this.orgDir);
                // If we got this far, we are ready to run.
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

    @Override
    public void run() {
        try {
            // We will put the sequences to output in here.
            List<Sequence> inSequences = new ArrayList<Sequence>(6000);
            // Get all the genomes.
            Iterable<String> genomes = this.coreSeed.getGenomes();
            // Loop through them, extracting sequences.
            for (String genomeId : genomes) {
                // Get the functions for all the pegs.
                Map<String, String> pegRoles = this.coreSeed.getGenomeFunctions(genomeId);
                // Get the sequences for the genome.
                PegList pegSequences = this.coreSeed.getGenomePegs(genomeId);
                // Only proceed if the genome has proteins.
                if (pegSequences != null) {
                    // Loop through all the features.
                    for (Map.Entry<String, String> pegInfo : pegRoles.entrySet()) {
                        // Break the assignment into roles.
                        String[] roles = Feature.rolesOfFunction(pegInfo.getValue());
                        // Perform the singleton filtering here.
                        if (roles.length == 1 || ! this.single) {
                            // Does this sequence have one of our target roles?
                            boolean found = false;
                            for (String roleDesc : roles) {
                                Role role = this.roleMap.getByName(roleDesc);
                                if (role != null) found = true;
                            }
                            // Process it accordingly.
                            if (found != this.reverse) {
                                // Get the protein sequence for this peg.
                                Sequence pegSequence = pegSequences.get(pegInfo.getKey());
                                if (pegSequence != null)
                                    inSequences.add(pegSequence);
                            }
                        }
                    }
                }
            }
            // Write the sequences to the output.
            this.writeSequences(inSequences);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }


    }

}
