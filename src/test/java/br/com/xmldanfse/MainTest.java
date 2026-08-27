package br.com.xmldanfse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {

    @Test
    void cliGeraDanfseCancelado(@TempDir Path dir) throws Exception {
        Path xml = dir.resolve("nfse.xml");
        Path pdf = dir.resolve("nfse-cancelada.pdf");
        Files.writeString(xml, NfseXmlReaderTest.xmlExemplo());

        int codigo = Main.executar(new String[]{
            xml.toString(), "--cancelada", "--saida", pdf.toString(), "--quiet"
        });

        assertEquals(0, codigo);
        assertTrue(Files.size(pdf) > 1000);
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(Files.readAllBytes(pdf)))) {
            String texto = new PDFTextStripper().getText(document).replaceAll("\\s", "");
            assertTrue(texto.contains("CANCELADA"));
        }
    }

    @Test
    void cliRejeitaSituacoesConflitantes(@TempDir Path dir) throws Exception {
        Path xml = dir.resolve("nfse.xml");
        Files.writeString(xml, NfseXmlReaderTest.xmlExemplo());

        int codigo = Main.executar(new String[]{
            xml.toString(), "--cancelada", "--substituida", "--quiet"
        });

        assertEquals(1, codigo);
    }
}
