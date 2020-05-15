/**
 *
 */
package org.theseed.proteins.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kohsuke.args4j.Argument;
import org.theseed.counters.QualityCountMap;
import org.theseed.genome.Feature;
import org.theseed.genome.Genome;
import org.theseed.genome.GenomeDirectory;
import org.theseed.proteins.Role;
import org.theseed.proteins.RoleMap;
import org.theseed.sequence.FastaOutputStream;
import org.theseed.sequence.Sequence;
import org.theseed.utils.BaseProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This command will take as input a role map containing role IDs and names (with an unused middle column).
 * It will then apply the role map to each genome in a genome directory, producing a FASTA file of the
 * proteins containing roles in the map.
 *
 * The positional parameters are the name of the role map file, the name of the genome directory, and the
 * name of the output FASTA file.  The standard output will be a report on the roles found.
 *
 * The command-line options are as follows.
 *
 * -h	display usage
 * -v	show more detailed progress messages
 *
 * @author Bruce Parrello
 *
 */
public class ExtractProcessor extends BaseProcessor {

    // FIELDS
    /** logging facility */
    protected static Logger log = LoggerFactory.getLogger(ExtractProcessor.class);
    /** role map */
    private RoleMap usefulRoles;
    /** role counters:  good = found, bad = missing */
    private QualityCountMap<String> roleCounts;
    /** input genome directory */
    private GenomeDirectory genomes;
    /** output FASTA stream */
    private FastaOutputStream fastaStream;

    // COMMAND-LINE OPTIONS

    /** input role file */
    @Argument(index = 0, metaVar = "roles.tbl", usage = "role input file", required = true)
    private File inFile;

    /** genome directory */
    @Argument(index = 1, metaVar = "genomeDir", usage = "input genome directory", required = true)
    private File genomeDir;

    /** output file */
    @Argument(index = 2, metaVar = "output.faa", usage = "output protein FASTA file", required = true)
    private File outFile;

    @Override
    protected void setDefaults() {
        this.roleCounts = new QualityCountMap<String>();
    }

    @Override
    protected boolean validateParms() throws IOException {
        // Validate and load the roles.
        if (! this.inFile.canRead())
            throw new FileNotFoundException("Role input file " + this.inFile + " not found or unreadable.");
        // Load the role map.
        log.info("Reading roles from {}.", this.inFile);
        this.usefulRoles = RoleMap.load(this.inFile);
        // Validate and load the genome directory.
        if (! this.genomeDir.isDirectory())
            throw new FileNotFoundException("Genome directory " + this.genomeDir + " not found or invalid.");
        log.info("Genomes will be loaded from {}.", this.genomeDir);
        this.genomes = new GenomeDirectory(this.genomeDir);
        // Finally, open the output file.
        this.fastaStream = new FastaOutputStream(this.outFile);
        return true;
    }

    @Override
    public void runCommand() throws Exception {
        try {
            // Get a set of all the roles.
            Set<String> roleSet = this.usefulRoles.keySet();
            // Use this as a sequence buffer.
            Sequence seq = new Sequence();
            // Loop through the genomes.
            for (Genome genome : genomes) {
                // Get a copy of the set of all the roles.
                Set<String> missingRoles = new HashSet<String>(roleSet);
                // Loop through the proteins.
                for (Feature peg : genome.getPegs()) {
                    List<Role> roles = peg.getUsefulRoles(this.usefulRoles);
                    // If this one has a useful role, save it.
                    if (roles.size() > 0) {
                        for (Role role : roles) {
                            String roleId = role.getId();
                            this.roleCounts.setGood(roleId);
                            missingRoles.remove(roleId);
                        }
                        seq.setLabel(peg.getId());
                        seq.setComment(peg.getFunction());
                        seq.setSequence(peg.getProteinTranslation());
                        this.fastaStream.write(seq);
                    }
                }
                // Count the missing roles.
                log.info("{} missing roles in {}.", missingRoles.size(), genome);
                for (String roleId : missingRoles)
                    this.roleCounts.setBad(roleId);
            }
            log.info("{} genomes processed.", genomes.size());
            // Write the role counts.
            System.out.println("role\tname\tfound\tmissing");
            for (String roleId : this.roleCounts.keys()) {
                String roleName = this.usefulRoles.getName(roleId);
                System.out.format("%s\t%s\t%d\t%d%n", roleId, roleName, this.roleCounts.good(roleId),
                        this.roleCounts.bad(roleId));
            }
        } finally {
            this.fastaStream.close();
        }
    }

}
