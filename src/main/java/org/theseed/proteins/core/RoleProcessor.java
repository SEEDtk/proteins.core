/**
 *
 */
package org.theseed.proteins.core;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.kohsuke.args4j.Option;
import org.theseed.genome.Feature;
import org.theseed.io.TabbedLineReader;
import org.theseed.proteins.Role;
import org.theseed.proteins.RoleMap;
import org.theseed.utils.ICommand;

/**
 * Create files of protein sequences from a FIGdisk and a list of roles.  One file will be created for each incoming role.  The role's
 * features will be included in the output with a "1" notation, and zero or more similar features will be included in the output with
 * a "0" notation.  The number of 0-features included is determined by a command-line option-- the default is <code>1</code>.
 *
 * The list of roles comes in via the standard input, in a tab-delimited file with no headers.  The first column is a role ID (this will
 * be the main part of the output file name for that role) and the second is the role name.
 *
 * The command-line options are as follows:
 *
 * --outDir	output directory name; the default is <code>.</code>, indicating the current directory
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
public class RoleProcessor extends SetProcessor implements ICommand {

    // COMMAND-LINE OPTIONS

    @Option(name="--outDir", aliases={"--modelDir"}, metaVar="modelDir", usage="output directory for training files")
    private File outDir;


    @Override
    public boolean parseCommand(String[] args) {
        // Set the defaults.
        this.outDir = new File(".");
        // Parse the command line.
        boolean retVal = parseArguments(args);
        return retVal;
    }

    @Override
    public void run() {
        try {
            // Create the role map and create a sequence batch for each role.
            if (this.debug) System.err.println("Reading roles to process.");
            RoleMap targetRoles = new RoleMap();
            Map<Role, SequenceBatch> batchMap = new HashMap<Role, SequenceBatch>();
            try (TabbedLineReader roleStream = new TabbedLineReader(this.inStream, 2)) {
                for (TabbedLineReader.Line line : roleStream) {
                    Role newRole = new Role(line.get(0), line.get(1));
                    targetRoles.put(newRole);
                    batchMap.put(newRole, new SequenceBatch(2000));
                }
            }
            // Loop through all the organism directories.
            Iterable<String> genomeList = this.getGenomes();
            for (String genomeId : genomeList) {
                Map<String, String> pegRoles = this.getGenomeFunctions(genomeId);
                // Loop through all the features.
                for (Map.Entry<String, String> pegInfo : pegRoles.entrySet()) {
                    // Break the assignment into roles.
                    String[] roles = Feature.rolesOfFunction(pegInfo.getValue());
                    // Get the roles we want.
                    for (String roleDesc : roles) {
                        Role role = targetRoles.getByName(roleDesc);
                        if (role != null) {
                            // Here it is a role we want.  Add the peg to the sequence batch.
                            SequenceBatch seqBatch = batchMap.get(role);
                            this.addSequence(seqBatch, pegInfo.getKey());
                        }
                    }
                }
            }
            // Loop through the sequence batches, writing them out.
            for (Map.Entry<Role, SequenceBatch> roleInfo : batchMap.entrySet()) {
                // Create the output file.
                File outFile = new File(this.outDir, roleInfo.getKey().getId() + this.getOutputSuffix());
                if (this.debug) System.err.println("Creating " + outFile + ".");
                OutputStream outStream = new FileOutputStream(outFile);
                this.processPegs(roleInfo.getValue(), outStream);
                outStream.close();
            }
            if (this.debug) System.err.println("All done.");
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
    }

}
