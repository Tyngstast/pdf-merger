package com.henrj.pdfmerger;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootApplication
public class PdfMergerApplication {

	public static void main(String[] args) {
		SpringApplication.run(PdfMergerApplication.class, args);
	}

	@Bean
	Function<Message<LinkedMultiValueMap<String, MultipartFile>>, Message<byte[]>> merge() {
		return input -> {
			List<byte[]> pdfs = input.getPayload().values().stream()
					.map(multipartFiles -> multipartFiles.stream()
							.map(file -> {
								try {
									return file.getBytes();
								} catch (IOException e) {
									throw new RuntimeException("Failed to read bytes", e);
								}
							}).collect(Collectors.toList()))
					.flatMap(Collection::stream)
					.collect(Collectors.toList());

			byte[] merged = mergePdfs(pdfs);

			return MessageBuilder
					.withPayload(merged)
					.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE)
					.setHeader(HttpHeaders.CONTENT_LENGTH, merged.length)
					.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=merged.pdf")
					.build();
		};
	}

	private byte[] mergePdfs(List<byte[]> pdfs) {
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
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to read InputStream", e);
		}
	}
}
