package org.theseed.proteins.core;

import java.util.Arrays;

import org.theseed.utils.ICommand;

/**
 * Main Application Class
 *
 * This class produces training and prediction sets for role identification neural nets.  It has two modes, one
 * for processing a list of pegs, and one for processing a list of roles.
 *
 * The commands are
 *
 * pegs		input contains a list of roles, produces training set
 * roles	input contains a role map, with role IDs and role names, produces training set
 * fasta	input contains a FASTA, produces prediction set
 * proteins	input contains a list of roles, produces prediction set
 * collate	input contains a list of roles, produces one FASTA for each role
 *
 */
public class App
{
    public static void main( String[] args )
    {
        // Get the control parameter.
        String command = args[0];
        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
        ICommand processor;
        switch (command) {
        case "pegs" :
            processor = new PegProcessor();
            break;
        case "roles" :
            processor = new RoleProcessor();
            break;
        case "fasta" :
            processor = new FastaProcessor();
            break;
        case "proteins" :
            processor = new ProteinProcessor();
            break;
        case "collate" :
        	processor = new CollateProcessor();
        	break;
        default :
            throw new RuntimeException("Invalid command " + command + ": must be \"pegs\", \"roles\", \"fasta\", or \"proteins\".");
        }
        boolean ok = processor.parseCommand(newArgs);
        if (ok) {
            processor.run();
        }
    }
}
