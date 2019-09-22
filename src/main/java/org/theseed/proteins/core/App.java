package org.theseed.proteins.core;

import java.util.Arrays;

import org.theseed.utils.ICommand;

/**
 * Main Application Class
 *
 * This class produces training sets for role identification neural nets.  It has two modes, one
 * for processing a list of pegs, and one for processing a list of roles.
 *
 * The commands are
 *
 * pegs		input contains a list of roles
 * roles	input contains a role map, with role IDs and role names
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
        default :
            throw new RuntimeException("Invalid command " + command + ": must be \"pegs\" or \"roles\".");
        }
        boolean ok = processor.parseCommand(newArgs);
        if (ok) {
            processor.run();
        }
    }
}
