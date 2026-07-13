package com.promptstudio.enums;

/**
 * Represents the file format a prompt or result can be exported to.
 * Used by ExportService to determine content type, file extension,
 * and generation strategy.
 */
public enum ExportFormat {

    PDF("application/pdf", ".pdf"),
    MARKDOWN("text/markdown", ".md"),
    TXT("text/plain", ".txt");

    private final String mimeType;
    private final String fileExtension;

    ExportFormat(String mimeType, String fileExtension) {
        this.mimeType = mimeType;
        this.fileExtension = fileExtension;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    /**
     * Builds a full downloadable file name by combining a base name
     * with this format's extension.
     *
     * @param baseName file name without extension, e.g. "my-prompt"
     * @return full file name, e.g. "my-prompt.pdf"
     */
    public String buildFileName(String baseName) {
        return baseName + this.fileExtension;
    }
}