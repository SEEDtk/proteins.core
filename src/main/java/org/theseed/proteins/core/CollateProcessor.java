/**
 *
 */
package org.theseed.proteins.core;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.theseed.genome.Feature;
import org.theseed.genome.core.CoreUtilities;
import org.theseed.genome.core.PegList;
import org.theseed.io.TabbedLineReader;
import org.theseed.proteins.Role;
import org.theseed.proteins.RoleMap;
import org.theseed.sequence.FastaOutputStream;
import org.theseed.sequence.Sequence;
import org.theseed.utils.ICommand;

/**
 * This processor reads a list of roles and creates a FASTA file for each role.  The input file
 * should be a two-column tab-delimited file with headers and a role ID and role description
 * on each line.  An output file will be produced for each role having the same name as the
 * role ID with a suffix of <code>.fa</code>.  Each file will contain all the proteins sequences
 * for that role.
 *
 * The positional parameter is the name of the output directory.  The command-line options are as
 * follows.
 *
 * --input		name of the input file; this should be tab-delimited with no headers, and contain role names;
 * 				the default is the standard input
 * --core		name of the coreSEED Organisms directory; the default is <code>FIGdisk/FIG/Data/Organisms</code>
 * 				in the current directory
 *
 * @author Bruce Parrello
 *
 */
public class CollateProcessor implements ICommand {

    // FIELDS
    /** input stream containing role IDs and names */
    private TabbedLineReader inStream;
    /** core seed organism manager */
    private CoreUtilities coreSeed;

    // COMMAND-LINE OPTIONS
    /** help option */
    @Option(name = "-h", aliases = { "--help" }, help = true)
    private boolean help;
    /** TRUE if we want progress messages */
    @Option(name = "-v", aliases = { "--verbose", "--debug" }, usage = "display progress on STDERR")
    private boolean debug;
    /** input file (if not using STDIN) */
    @Option(name = "--input", aliases = { "-i" }, usage = "input file (if not STDIN)")
    private File inFile;
    /** coreSEED organism directory */
    @Option(name = "--core", metaVar = "/vol/core-seed/FIGdisk/FIG/Data/Organisms", usage = "CoreSEED organism directory")
    private File orgDir;
    /** output directory */
    @Argument(index = 0, metaVar = "outDir", usage = "output directory", required = true)
    private File outDir;

    @Override
    public boolean parseCommand(String[] args) {
        boolean retVal = false;
        // Set the defaults.
        this.help = false;
        this.debug = false;
        this.inFile = null;
        this.orgDir = new File("FIGdisk/FIG/Data/Organisms");
        // Parse the command line.
        CmdLineParser parser = new CmdLineParser(this);
        try {
            parser.parseArgument(args);
            if (this.help) {
                parser.printUsage(System.err);
            } else {
                // Verify the output directory.
                if (! this.outDir.isDirectory()) {
                    // The directory does not exist.  Try to create it.
                    if (this.debug) System.err.println("Creating directory " + this.outDir + ".");
                    if (! this.outDir.mkdirs())
                        throw new IOException("Could not create " + this.outDir + ".");
                }
                // Open the input stream.
                if (this.inFile != null) {
                    if (this.debug) System.err.println("Reading roles from " + this.inFile + ".");
                    this.inStream = new TabbedLineReader(this.inFile);
                } else {
                    if (this.debug) System.err.println("Reading roles from standard input.");
                    this.inStream = new TabbedLineReader(System.in);
                }
                // Connect to the coreSEED.
                this.coreSeed = new CoreUtilities(this.debug, this.orgDir);
                // We made it this far, we can run.
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
            // Create a role map from the input file.
            if (this.debug) System.err.println("Reading roles.");
            RoleMap rolesMap = new RoleMap();
            for (TabbedLineReader.Line line : this.inStream) {
                Role inRole = new Role(line.get(0), line.get(1));
                rolesMap.put(inRole);
            }
            Set<String> roles = rolesMap.keySet();
            if (this.debug) System.err.println(roles.size() + " roles found.");
            // Create a hash of roles to output files.
            Map<String, FastaOutputStream> roleStreams = new HashMap<String, FastaOutputStream>();
            for (String role : roles) {
                File roleFile = new File(this.outDir, role + ".fa");
                FastaOutputStream roleStream = new FastaOutputStream(roleFile);
                roleStreams.put(role, roleStream);
                if (this.debug) System.err.println(roleFile + " created for role \""
                        + rolesMap.getName(role) + "\".");
            }
            // Loop through the genomes, creating output.
            for (String genomeId : this.coreSeed.getGenomes()) {
                Map<String, String> pegFunctions = this.coreSeed.getGenomeFunctions(genomeId);
                PegList pegSequences = this.coreSeed.getGenomePegs(genomeId);
                for (Map.Entry<String, String> pegInfo : pegFunctions.entrySet()) {
                    // Break the assignment into roles.
                    String[] fRoles = Feature.rolesOfFunction(pegInfo.getValue());
                    // Does this sequence have one of our target roles?
                    for (String roleDesc : fRoles) {
                        Role role = rolesMap.getByName(roleDesc);
                        if (role != null) {
                            // We have a target role; write the sequence to its file.
                            FastaOutputStream roleStream = roleStreams.get(role.getId());
                            Sequence pegSeq = pegSequences.get(pegInfo.getKey());
                            roleStream.write(pegSeq);
                        }
                    }
                }
            }
            // Close the output files.
            if (this.debug) System.err.println("Closing output files.");
            for (FastaOutputStream roleStream : roleStreams.values()) {
                roleStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        } finally {
            this.inStream.close();
        }
    }

}
