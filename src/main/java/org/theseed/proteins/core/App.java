package org.theseed.proteins.core;


/**
 * Main Application Class
 *
 * If multiple processors are required, we put them in here as alternative commands.
 *
 */
public class App
{
    public static void main( String[] args )
    {
        PegProcessor runObject = new PegProcessor();
        boolean ok = runObject.parseCommand(args);
        if (ok) {
            runObject.run();
        }
    }
}
