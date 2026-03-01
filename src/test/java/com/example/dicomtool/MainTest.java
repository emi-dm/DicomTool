package com.example.dicomtool;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.FileWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;
import org.dcm4che3.data.VR;
import org.dcm4che3.io.DicomOutputStream;
import org.junit.jupiter.api.Test;

public class MainTest {

    private File makeDummy(File f, int rows, int cols) throws IOException {
        Attributes attr = new Attributes();
        attr.setString(Tag.SOPClassUID, VR.UI, UID.SecondaryCaptureImageStorage);
        attr.setString(Tag.SOPInstanceUID, VR.UI, "1.2.3.4.5.6.7.8.9");
        attr.setInt(Tag.Rows, VR.US, rows);
        attr.setInt(Tag.Columns, VR.US, cols);
        attr.setInt(Tag.BitsAllocated, VR.US, 16);
        short[] vals = new short[rows * cols];
        for (int i = 0; i < vals.length; i++) {
            vals[i] = (short) i;
        }
        ByteBuffer buf = ByteBuffer.allocate(vals.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (short s : vals) buf.putShort(s);
        attr.setBytes(Tag.PixelData, VR.OW, buf.array());
        try (DicomOutputStream dos = new DicomOutputStream(f)) {
            dos.writeDataset(attr.createFileMetaInformation(UID.ImplicitVRLittleEndian), attr);
        }
        return f;
    }

    @Test
    void testLoadMean() throws IOException {
        File tmp = File.createTempFile("dummy", ".dcm");
        tmp.deleteOnExit();
        makeDummy(tmp, 10, 10);
        Main m = new Main();
        double mean = m.loadMean(tmp);
        assertEquals((99.0 / 2.0) , mean); // average of 0..99
    }

    @Test
    void testCsvOutput() throws IOException {
        File f1 = File.createTempFile("dcm1_", ".dcm");
        File f2 = File.createTempFile("dcm2_", ".dcm");
        f1.deleteOnExit();
        f2.deleteOnExit();
        makeDummy(f1, 2, 2); // values 0..3, mean 1.5
        makeDummy(f2, 2, 2); // same mean
        Main m = new Main();
        m.file1 = f1;
        m.file2 = f2;
        File csv = File.createTempFile("out", ".csv");
        csv.deleteOnExit();
        m.csv = csv;
        m.run();
        String content = java.nio.file.Files.readString(csv.toPath());
        // check first lines
        String[] lines = content.split("\n");
        assertEquals("file,mean", lines[0]);
        assertEquals(f1.toString() + ",1.5000", lines[1]);
        assertEquals(f2.toString() + ",1.5000", lines[2]);
    }

    @Test
    void testCsvAppend() throws IOException {
        File csv = File.createTempFile("out", ".csv");
        csv.deleteOnExit();
        // write initial header and a row
        try (FileWriter fw = new FileWriter(csv)) {
            fw.write("file,mean\n");
            fw.write("initial,0.0000\n");
        }
        File f1 = File.createTempFile("dcm3_", ".dcm");
        File f2 = File.createTempFile("dcm4_", ".dcm");
        f1.deleteOnExit();
        f2.deleteOnExit();
        makeDummy(f1, 1, 1);
        makeDummy(f2, 1, 1);
        Main m = new Main();
        m.file1 = f1;
        m.file2 = f2;
        m.csv = csv;
        m.run();
        String content = java.nio.file.Files.readString(csv.toPath());
        String[] lines = content.split("\n");
        // first three lines should be original header+row, then appended two entries
        assertEquals("file,mean", lines[0]);
        assertEquals("initial,0.0000", lines[1]);
        assertEquals(f1.toString() + ",0.0000", lines[2]);
        assertEquals(f2.toString() + ",0.0000", lines[3]);
    }
}
