package com.henrj;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class App {

    public static void main( String[] args ) throws IOException {
        new App().run(Arrays.asList(
                "pdf1.pdf",
                "pdf2.pdf",
                "pdf3.pdf"
        ));
    }

    void run(List<String> filePaths) throws IOException {
        List<byte[]> pdfs = filePaths.stream()
                .map(this::readFile)
                .collect(Collectors.toList());

        byte[] merged = mergePdfs(pdfs);

        Files.write(Paths.get("out.pdf"), merged);
    }

    byte[] mergePdfs(List<byte[]> pdfs) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PdfDocument resultDoc = new PdfDocument(new PdfWriter(byteArrayOutputStream));

        pdfs.stream()
                .map(ByteArrayInputStream::new)
                .map(this::readInputStream)
                .map(PdfDocument::new)
                .forEach(doc -> {
                    doc.copyPagesTo(1, doc.getNumberOfPages(), resultDoc);
                    doc.close();
                });

        resultDoc.close();

        return byteArrayOutputStream.toByteArray();
    }

    private PdfReader readInputStream(ByteArrayInputStream byteArrayInputStream) {
        try {
            return new PdfReader(byteArrayInputStream);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read InputStream", e);
        }
    }

    private byte[] readFile(String path)  {
        try {
            return Files.readAllBytes(Paths.get(path));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file", e);
        }
    }
}
