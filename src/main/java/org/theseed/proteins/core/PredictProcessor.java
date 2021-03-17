package org.theseed.proteins.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.Option;
import org.theseed.io.TabbedLineReader;
import org.theseed.sequence.Sequence;

public class PredictProcessor {

    /** control file input stream */
    private TabbedLineReader controlStream;

    /** help option */
    @Option(name = "-h", aliases = { "--help" }, help = true)
    protected boolean help;
    /** TRUE if we want progress messages */
    @Option(name = "-v", aliases = { "--verbose", "--debug" }, usage = "display progress on STDERR")
    protected boolean debug;
    /** output format -- default is DL4J */
    @Option(name = "--format", aliases = { "-o" }, usage = "format for output sequences")
    private SequenceWriter.Type format;
    /** output directory */
    @Argument(index = 0, metaVar = "outDir", usage = "output directory", required = true)
    private File outDir;
    /** control file */
    @Argument(index = 1, metaVar = "control.tbl", usage = "control file with model names and widths", required = true)
    private File controlFile;

    public PredictProcessor() {
        super();
        // Set the parameter defaults.
        this.help = false;
        this.debug = false;
        this.format = SequenceWriter.Type.DL4J;
    }

    /**
     * Set up the control file and the output directory.
     *
     * @throws IOException
     */
    protected void initializeOutput() throws IOException {
        // Verify the output directory.
        if (! this.outDir.isDirectory()) {
            // The directory does not exist.  Try to create it.
            if (this.debug) System.err.println("Creating directory " + this.outDir + ".");
            if (! this.outDir.mkdirs())
                throw new IOException("Could not create " + this.outDir + ".");
        }
        // Open the control file as headerless.
        this.controlStream = new TabbedLineReader(this.controlFile, 2);
    }

    /**
     * Write the sequences as prediction input to each of the model input files indicated in the
     * control file.
     *
     * @param inSequences	list of sequences to process
     *
     * @throws FileNotFoundException
     */
    protected void writeSequences(List<Sequence> inSequences) throws FileNotFoundException {
        // Loop through the control stream.  For each input record, we produce an output file.
        for (TabbedLineReader.Line line : this.controlStream) {
            String modelName = line.get(0);
            int modelWidth = line.getInt(1);
            // Create the output file.
            File proteinFile = new File(this.outDir, modelName + SequenceWriter.suffix(this.format));
            if (this.debug) System.err.println("Creating output in " + proteinFile + " with width " + modelWidth + ".");
            FileOutputStream proteinStream = new FileOutputStream(proteinFile);
            try (SequenceWriter proteinWriter = SequenceWriter.predictStream(this.format, proteinStream, modelWidth)) {
                for (Sequence seq : inSequences) {
                    proteinWriter.write(seq);
                }
            }
        }
    }

    @Override
    public void finalize() {
        if (this.controlStream != null)
            this.controlStream.close();
    }

}
