package br.com.xmldanfse;

/**
 * Situação atual da NFS-e usada na representação do DANFSe.
 *
 * <p>O XML autorizado permanece com status de autorização mesmo depois de a NFS-e ser cancelada
 * ou substituída, pois essas mudanças são registradas como eventos no ADN. Cabe à aplicação
 * consultar os eventos e informar a situação atual ao gerar o documento.
 */
public enum DanfseSituacao {
    /** NFS-e sem evento de cancelamento ou substituição. */
    NORMAL(null),

    /** NFS-e com evento de cancelamento registrado. */
    CANCELADA("CANCELADA"),

    /** NFS-e cancelada por substituição. */
    SUBSTITUIDA("SUBSTITUÍDA");

    private final String marcaDagua;

    DanfseSituacao(String marcaDagua) {
        this.marcaDagua = marcaDagua;
    }

    String marcaDagua() {
        return marcaDagua;
    }
}
