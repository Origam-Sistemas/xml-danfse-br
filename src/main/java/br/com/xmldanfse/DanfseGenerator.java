package br.com.xmldanfse;

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Objects;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;

/**
 * Facade do modulo: gera o PDF do DANFSe a partir do XML autorizado da NFS-e, conforme o modelo
 * oficial da NT 008/2026 (Anexo I).
 *
 * <p>Fluxo: {@link NfseXmlReader} extrai o modelo, {@link QrCodeGenerator} produz o QR,
 * {@link DanfseHtmlRenderer} monta o XHTML e o OpenHTMLtoPDF rasteriza em PDF com a fonte
 * Liberation Sans embutida (metrica compativel com a Arial exigida pela NT, licenca OFL).
 */
public final class DanfseGenerator {

    private static volatile String logoDataUri;

    private DanfseGenerator() {
    }

    /**
     * Gera o PDF do DANFSe inferindo o ambiente do proprio XML ({@code tpAmb} da DPS):
     * producao usa a URL de consulta publica de producao no QR; producao restrita usa a de
     * homologacao e estampa "NFS-e SEM VALIDADE JURIDICA".
     */
    public static byte[] gerarPdf(String nfseXml) {
        return gerarPdf(nfseXml, DanfseSituacao.NORMAL, DanfseConfig.vazio());
    }

    /** Como {@link #gerarPdf(String)}, com configuracao opcional (logo do emitente etc.). */
    public static byte[] gerarPdf(String nfseXml, DanfseConfig config) {
        return gerarPdf(nfseXml, DanfseSituacao.NORMAL, config);
    }

    /**
     * Como {@link #gerarPdf(String)}, considerando a situacao atual obtida nos eventos da NFS-e.
     * Notas canceladas ou substituidas recebem a marca d'agua exigida pela NT 008/2026.
     */
    public static byte[] gerarPdf(String nfseXml, DanfseSituacao situacao) {
        return gerarPdf(nfseXml, situacao, DanfseConfig.vazio());
    }

    /** Como {@link #gerarPdf(String, DanfseSituacao)}, com configuracao opcional. */
    public static byte[] gerarPdf(String nfseXml, DanfseSituacao situacao, DanfseConfig config) {
        Objects.requireNonNull(nfseXml, "nfseXml is required");
        Objects.requireNonNull(situacao, "situacao is required");
        Danfse danfse = NfseXmlReader.read(nfseXml);
        return gerar(danfse, !danfse.homologacao(), config, situacao);
    }

    /** Gera o PDF do DANFSe a partir do XML da NFS-e. {@code producao} controla a URL do QR. */
    public static byte[] gerarPdf(String nfseXml, boolean producao) {
        return gerarPdf(nfseXml, producao, DanfseSituacao.NORMAL, DanfseConfig.vazio());
    }

    /** Como {@link #gerarPdf(String, boolean)}, com identificacao opcional do municipio. */
    public static byte[] gerarPdf(String nfseXml, boolean producao, DanfseConfig config) {
        return gerarPdf(nfseXml, producao, DanfseSituacao.NORMAL, config);
    }

    /** Como {@link #gerarPdf(String, boolean)}, considerando a situacao atual da NFS-e. */
    public static byte[] gerarPdf(String nfseXml, boolean producao, DanfseSituacao situacao) {
        return gerarPdf(nfseXml, producao, situacao, DanfseConfig.vazio());
    }

    /** Como {@link #gerarPdf(String, boolean, DanfseSituacao)}, com configuracao opcional. */
    public static byte[] gerarPdf(String nfseXml, boolean producao, DanfseSituacao situacao,
            DanfseConfig config) {
        Objects.requireNonNull(nfseXml, "nfseXml is required");
        Objects.requireNonNull(situacao, "situacao is required");
        return gerar(NfseXmlReader.read(nfseXml), producao, config, situacao);
    }

    private static byte[] gerar(Danfse danfse, boolean producao, DanfseConfig config,
            DanfseSituacao situacao) {
        // QR em bitmap de 300 px para nitidez de impressao; o CSS fixa 1,52 x 1,52 cm (NT 008).
        String qr = danfse.chaveAcesso() == null
            ? null
            : QrCodeGenerator.dataUri(QrCodeGenerator.consultaUrl(danfse.chaveAcesso(), producao), 300);
        String html = DanfseHtmlRenderer.render(danfse, qr, logoOficial(),
            config == null ? DanfseConfig.vazio() : config);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(() -> fonte("LiberationSans-Regular.ttf"), "Liberation Sans",
                400, BaseRendererBuilder.FontStyle.NORMAL, true);
            builder.useFont(() -> fonte("LiberationSans-Bold.ttf"), "Liberation Sans",
                700, BaseRendererBuilder.FontStyle.NORMAL, true);
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return aplicarMarcaDagua(out.toByteArray(), situacao);
        } catch (Exception exception) {
            throw new DanfseException("Nao foi possivel gerar o PDF do DANFSe.", exception);
        }
    }

    /** Gera o PDF e grava no caminho informado, retornando os bytes. */
    public static byte[] gerarPdf(String nfseXml, boolean producao, Path saida) {
        return gerarPdf(nfseXml, producao, DanfseConfig.vazio(), saida);
    }

    public static byte[] gerarPdf(String nfseXml, boolean producao, DanfseConfig config, Path saida) {
        Objects.requireNonNull(saida, "saida is required");
        return gravar(gerarPdf(nfseXml, producao, config), saida);
    }

    /** Como {@link #gerarPdf(String, boolean, DanfseSituacao)}, gravando no caminho informado. */
    public static byte[] gerarPdf(String nfseXml, boolean producao, DanfseSituacao situacao,
            Path saida) {
        return gerarPdf(nfseXml, producao, situacao, DanfseConfig.vazio(), saida);
    }

    /** Como {@link #gerarPdf(String, boolean, DanfseSituacao, DanfseConfig)}, gravando em arquivo. */
    public static byte[] gerarPdf(String nfseXml, boolean producao, DanfseSituacao situacao,
            DanfseConfig config, Path saida) {
        Objects.requireNonNull(saida, "saida is required");
        return gravar(gerarPdf(nfseXml, producao, situacao, config), saida);
    }

    /** Como {@link #gerarPdf(String, DanfseConfig)}, gravando no caminho informado. */
    public static byte[] gerarPdf(String nfseXml, DanfseConfig config, Path saida) {
        Objects.requireNonNull(saida, "saida is required");
        return gravar(gerarPdf(nfseXml, config), saida);
    }

    /** Como {@link #gerarPdf(String, DanfseSituacao)}, gravando no caminho informado. */
    public static byte[] gerarPdf(String nfseXml, DanfseSituacao situacao, Path saida) {
        return gerarPdf(nfseXml, situacao, DanfseConfig.vazio(), saida);
    }

    /** Como {@link #gerarPdf(String, DanfseSituacao, DanfseConfig)}, gravando em arquivo. */
    public static byte[] gerarPdf(String nfseXml, DanfseSituacao situacao, DanfseConfig config,
            Path saida) {
        Objects.requireNonNull(saida, "saida is required");
        return gravar(gerarPdf(nfseXml, situacao, config), saida);
    }

    private static byte[] aplicarMarcaDagua(byte[] pdf, DanfseSituacao situacao) throws IOException {
        String texto = situacao.marcaDagua();
        if (texto == null) {
            return pdf;
        }

        try (PDDocument document = PDDocument.load(pdf);
             InputStream fonte = fonte("LiberationSans-Bold.ttf");
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDType0Font font = PDType0Font.load(document, fonte, true);
            for (PDPage page : document.getPages()) {
                aplicarMarcaDagua(document, page, font, texto);
            }
            document.save(out);
            return out.toByteArray();
        }
    }

    private static void aplicarMarcaDagua(PDDocument document, PDPage page, PDType0Font font,
            String texto) throws IOException {
        float fontSize = 72f;
        float larguraTexto = font.getStringWidth(texto) / 1000f * fontSize;
        float centroX = page.getCropBox().getLowerLeftX() + page.getCropBox().getWidth() / 2f;
        float centroY = page.getCropBox().getLowerLeftY() + page.getCropBox().getHeight() / 2f;

        PDExtendedGraphicsState transparencia = new PDExtendedGraphicsState();
        transparencia.setNonStrokingAlphaConstant(0.32f);

        try (PDPageContentStream content = new PDPageContentStream(document, page,
                AppendMode.APPEND, true, true)) {
            content.setGraphicsStateParameters(transparencia);
            content.setNonStrokingColor(new Color(128, 128, 128));
            content.beginText();
            content.setFont(font, fontSize);
            content.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(45), centroX, centroY));
            content.newLineAtOffset(-larguraTexto / 2f, -fontSize / 3f);
            content.showText(texto);
            content.endText();
        }
    }

    private static byte[] gravar(byte[] pdf, Path saida) {
        try {
            Path parent = saida.toAbsolutePath().normalize().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(saida, pdf);
        } catch (IOException exception) {
            throw new DanfseException("Nao foi possivel gravar o PDF do DANFSe em " + saida, exception);
        }
        return pdf;
    }

    /**
     * Le um arquivo de imagem (PNG/JPG/GIF/SVG) e devolve uma data URI pronta para o DANFSe
     * (ex.: logo do emitente). Util para CLI/MCP que recebem um caminho de arquivo.
     */
    public static String dataUriImagem(Path arquivo) {
        Objects.requireNonNull(arquivo, "arquivo is required");
        return ImagemDataUri.de(arquivo);
    }

    private static InputStream fonte(String nome) {
        InputStream in = DanfseGenerator.class.getResourceAsStream("/danfse/fonts/" + nome);
        if (in == null) {
            throw new DanfseException("Fonte nao encontrada no classpath: " + nome);
        }
        return in;
    }

    /** Logo oficial da NFS-e (CC BY-ND), embutido como data URI. Carregado uma vez do classpath. */
    private static String logoOficial() {
        String cached = logoDataUri;
        if (cached != null) {
            return cached;
        }
        try (InputStream in = DanfseGenerator.class.getResourceAsStream("/danfse/nfse-logo.png")) {
            if (in == null) {
                return null;
            }
            String uri = "data:image/png;base64," + Base64.getEncoder().encodeToString(in.readAllBytes());
            logoDataUri = uri;
            return uri;
        } catch (IOException exception) {
            return null;
        }
    }
}
