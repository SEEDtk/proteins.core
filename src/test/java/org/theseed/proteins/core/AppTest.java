package org.theseed.proteins.core;

import junit.framework.Test;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;

import org.theseed.sequence.FastaInputStream;
import org.theseed.sequence.Sequence;

/**
 * Unit test for simple App.
 */
public class AppTest extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }


    /**
     * Test the sequence writers
     *
     * @throws IOException
     */
    public void testSeqWriters() throws IOException {
        File testFile = new File("src/test", "seqs.ser");
        OutputStream stream1 = new FileOutputStream(testFile);
        SequenceWriter sWriter = SequenceWriter.create(SequenceWriter.Type.DL4J, stream1, 10);
        sWriter.write("id1", "0", "MSWVAKYLPTRPTVPVLSGVLLTGSDSGL");
        sWriter.write("id2", "1", "MPRLL");
        sWriter.close();
        InputStream stream2 = new FileInputStream(testFile);
        Scanner sReader = new Scanner(stream2);
        assertTrue(sReader.hasNextLine());
        String line = sReader.nextLine();
        assertThat(line, equalTo("found\tp1\tp2\tp3\tp4\tp5\tp6\tp7\tp8\tp9\tp10"));
        assertTrue(sReader.hasNextLine());
        line = sReader.nextLine();
        assertThat(line, equalTo("0.0\tM\tS\tW\tV\tA\tK\tY\tL\tP\tT"));
        assertTrue(sReader.hasNextLine());
        line = sReader.nextLine();
        assertThat(line, equalTo("1.0\tM\tP\tR\tL\tL\t*\t-\t-\t-\t-"));
        assertFalse(sReader.hasNextLine());
        sReader.close();
        stream1 = new FileOutputStream(testFile);
        sWriter = SequenceWriter.create(SequenceWriter.Type.ORANGE, stream1, 10);
        sWriter.write("id1", "0", "MSWVAKYLPTRPTVPVLSGVLLTGSDSGL");
        sWriter.write("id2", "1", "MPRLL");
        sWriter.close();
        stream2 = new FileInputStream(testFile);
        sReader = new Scanner(stream2);
        assertTrue(sReader.hasNextLine());
        line = sReader.nextLine();
        assertThat(line, equalTo("p1,p2,p3,p4,p5,p6,p7,p8,p9,p10,class"));
        assertTrue(sReader.hasNextLine());
        line = sReader.nextLine();
        assertThat(line, equalTo("M,S,W,V,A,K,Y,L,P,T,0"));
        assertTrue(sReader.hasNextLine());
        line = sReader.nextLine();
        assertThat(line, equalTo("M,P,R,L,L,*,-,-,-,-,1"));
        assertFalse(sReader.hasNextLine());
        sReader.close();
        stream1 = new FileOutputStream(testFile);
        sWriter = SequenceWriter.create(SequenceWriter.Type.FASTA, stream1, 10);
        sWriter.write("id1", "0", "MSWVAKYLPTRPTVPVLSGVLLTGSDSGL");
        sWriter.write("id2", "1", "MPRLL");
        sWriter.close();
        FastaInputStream stream3 = new FastaInputStream(testFile);
        assertTrue(stream3.hasNext());
        Sequence seq = stream3.next();
        assertThat(seq.getLabel(), equalTo("id1"));
        assertThat(seq.getComment(), equalTo("0"));
        assertThat(seq.getSequence(), equalTo("MSWVAKYLPTRPTVPVLSGVLLTGSDSGL"));
        assertTrue(stream3.hasNext());
        seq = stream3.next();
        assertThat(seq.getLabel(), equalTo("id2"));
        assertThat(seq.getComment(), equalTo("1"));
        assertThat(seq.getSequence(), equalTo("MPRLL"));
        assertFalse(stream3.hasNext());
        stream3.close();
        stream1 = new FileOutputStream(testFile);
        sWriter = new ProteinSequenceWriter(stream1, 10);
        sWriter.write("id1", "0", "MSWVAKYLPTRPTVPVLSGVLLTGSDSGL");
        sWriter.write("id2", "1", "MPRLL");
        sWriter.close();
        stream2 = new FileInputStream(testFile);
        sReader = new Scanner(stream2);
        assertTrue(sReader.hasNextLine());
        line = sReader.nextLine();
        assertThat(line, equalTo("seq_id\tp1\tp2\tp3\tp4\tp5\tp6\tp7\tp8\tp9\tp10"));
        assertTrue(sReader.hasNextLine());
        line = sReader.nextLine();
        assertThat(line, equalTo("id1\tM\tS\tW\tV\tA\tK\tY\tL\tP\tT"));
        assertTrue(sReader.hasNextLine());
        line = sReader.nextLine();
        assertThat(line, equalTo("id2\tM\tP\tR\tL\tL\t*\t-\t-\t-\t-"));
        assertFalse(sReader.hasNextLine());
        sReader.close();
    }

    /**
     * Test the sequence batch.
     * @throws IOException
     */
    public void testSeqBatch() throws IOException {
        File inFile = new File("src/test", "test2.fa");
        SequenceBatch seqBatch = new SequenceBatch(20);
        FastaInputStream inStream = new FastaInputStream(inFile);
        for (Sequence seq : inStream)
            seqBatch.add(seq);
        assertThat(seqBatch.longest(), equalTo(87));
        inStream.close();
    }


    /**
     * simple test
     */
    public void testObjectivism() {
        assertThat("Ayn Rand is wrong.", 'A', equalTo('A'));
    }

}
