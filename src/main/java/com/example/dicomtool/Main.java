package com.example.dicomtool;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "dicomtool", mixinStandardHelpOptions = true,
        description = "Herramienta Java para leer dos ficheros DICOM y calcular la media de sus pixeles.")
public class Main implements Runnable {

    @Parameters(index = "0", description = "Primer archivo DICOM")
    File file1;

    @Parameters(index = "1", description = "Segundo archivo DICOM")
    File file2;

    @CommandLine.Option(names = {"-c", "--csv"}, description = "Archivo CSV de salida (opcional). Si existe, se añadirán las filas al final.")
    File csv;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        try {
            double mean1 = loadMean(file1);
            double mean2 = loadMean(file2);
            double diff = Math.abs(mean1 - mean2);
            System.out.printf("%s: media = %.4f\n", file1, mean1);
            System.out.printf("%s: media = %.4f\n", file2, mean2);
            System.out.printf("Diferencia absoluta de medias = %.4f\n", diff);
            if (csv != null) {
                writeCsv(mean1, mean2, diff);
                System.out.println("Resultados guardados en CSV: " + csv);
            }
        } catch (IOException e) {
            System.err.println("Error leyendo DICOM: " + e.getMessage());
            System.exit(1);
        }
    }

    double loadMean(File f) throws IOException {
        try (DicomInputStream dis = new DicomInputStream(f)) {
            Attributes attr = dis.readDataset();
            // PixelData suele estar en Tag.PixelData y es un byte[]
            byte[] pixels = attr.getBytes(Tag.PixelData);
            if (pixels == null) {
                throw new IOException("No se encontró PixelData en " + f);
            }
            // obtener filas/columnas/bits
            int rows = attr.getInt(Tag.Rows, 0);
            int cols = attr.getInt(Tag.Columns, 0);
            int bitsAllocated = attr.getInt(Tag.BitsAllocated, 0);
            if (rows <= 0 || cols <= 0 || bitsAllocated <= 0) {
                throw new IOException("Metadatos de imagen inválidos en " + f);
            }
            int samples = pixels.length;
            // asumimos little endian implícito, convertir a unsigned short si es 16 bits
            double sum = 0;
            if (bitsAllocated == 16) {

                ByteBuffer buf = ByteBuffer.wrap(pixels).order(ByteOrder.LITTLE_ENDIAN);
                int count = rows * cols;
                for (int i = 0; i < count; i++) {
                    int val = buf.getShort() & 0xFFFF;
                    sum += val;
                }
                return sum / count;
            } else if (bitsAllocated == 8) {
                for (byte b : pixels) {
                    sum += (b & 0xFF);
                }
                return sum / pixels.length;
            } else {
                throw new IOException("BitsAllocated " + bitsAllocated + " no soportado");
            }
        }
    }

    void writeCsv(double mean1, double mean2, double diff) throws IOException {
        boolean needsHeader = !csv.exists() || csv.length() == 0;
        try (FileWriter fw = new FileWriter(csv, true)) { // append mode
            if (needsHeader) {
                fw.write("file,mean\n");
            }
            fw.write(file1 + "," + String.format(Locale.ROOT, "%.4f", mean1) + "\n");
            fw.write(file2 + "," + String.format(Locale.ROOT, "%.4f", mean2) + "\n");
            if (needsHeader) {
                fw.write("difference," + String.format(Locale.ROOT, "%.4f", diff) + "\n");
            }
        }
    }
}
