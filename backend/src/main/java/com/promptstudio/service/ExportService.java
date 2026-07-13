package com.promptstudio.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.promptstudio.entity.Prompt;
import com.promptstudio.enums.ExportFormat;
import com.promptstudio.exception.ApiException;
import com.promptstudio.repository.PromptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service layer implementing the Export feature: converts a saved
 * Prompt record into a downloadable file in PDF, Markdown, or plain
 * text format. PDF generation uses iText directly within this service
 * rather than a separate util class, since the logic is simple enough
 * (a handful of paragraphs) not to warrant its own layer.
 */
@Service
@RequiredArgsConstructor
public class ExportService {

    private final PromptRepository promptRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy 'at' h:mm a");

    /**
     * Simple container for a generated export file's bytes and the
     * suggested filename, returned to the controller for streaming
     * back as a download response.
     *
     * @param fileBytes the raw file content
     * @param fileName  the suggested download filename, including extension
     * @param mimeType  the MIME type to set in the Content-Type response header
     */
    public record ExportResult(byte[] fileBytes, String fileName, String mimeType) {
    }

    /**
     * Exports a user's prompt record to the requested file format.
     *
     * @param userId   the requesting user's ID
     * @param promptId the ID of the prompt to export
     * @param format   the desired export format, as a string ("PDF", "MARKDOWN", "TXT")
     * @return the generated ExportResult containing file bytes, name, and MIME type
     * @throws ApiException with 404 NOT_FOUND if the prompt isn't found or owned by this user,
     *                       or 400 BAD_REQUEST if the format string is invalid
     */
    public ExportResult exportPrompt(String userId, String promptId, String format) {
        Prompt prompt = promptRepository.findByIdAndUserId(promptId, userId)
                .orElseThrow(() -> new ApiException("Prompt not found", HttpStatus.NOT_FOUND));

        ExportFormat exportFormat = parseFormat(format);
        String baseFileName = sanitizeFileName(prompt.getTitle());

        byte[] fileBytes = switch (exportFormat) {
            case PDF -> buildPdf(prompt);
            case MARKDOWN -> buildMarkdown(prompt).getBytes(StandardCharsets.UTF_8);
            case TXT -> buildTxt(prompt).getBytes(StandardCharsets.UTF_8);
        };

        return new ExportResult(fileBytes, exportFormat.buildFileName(baseFileName), exportFormat.getMimeType());
    }

    /**
     * Parses the requested format string into an ExportFormat enum,
     * case-insensitively.
     *
     * @param format the raw format string from the request
     * @return the matched ExportFormat
     * @throws ApiException with 400 BAD_REQUEST if the format is not recognized
     */
    private ExportFormat parseFormat(String format) {
        try {
            return ExportFormat.valueOf(format.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ApiException("Invalid export format. Use PDF, MARKDOWN, or TXT", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Strips characters that are unsafe in file names, replacing them
     * with underscores, and truncates to a reasonable length.
     *
     * @param title the prompt's title to base the filename on
     * @return a filesystem-safe base filename (no extension)
     */
    private String sanitizeFileName(String title) {
        String safe = title == null || title.isBlank() ? "prompt" : title;
        safe = safe.replaceAll("[^a-zA-Z0-9-_ ]", "").trim().replaceAll("\\s+", "-");
        return safe.length() > 60 ? safe.substring(0, 60) : safe;
    }

    /**
     * Builds a plain text export of the prompt, including title,
     * metadata, and the full generated prompt text.
     *
     * @param prompt the prompt record to export
     * @return the formatted plain text content
     */
    private String buildTxt(Prompt prompt) {
        StringBuilder sb = new StringBuilder();
        sb.append(prompt.getTitle()).append("\n");
        sb.append("=".repeat(prompt.getTitle().length())).append("\n\n");

        appendMetadataLine(sb, "Type", prompt.getPromptType() != null ? prompt.getPromptType().getDisplayName() : null);
        appendMetadataLine(sb, "Created", prompt.getCreatedAt() != null ? prompt.getCreatedAt().format(DATE_FORMAT) : null);
        if (prompt.getTags() != null && !prompt.getTags().isEmpty()) {
            appendMetadataLine(sb, "Tags", String.join(", ", prompt.getTags()));
        }
        sb.append("\n");

        if (prompt.getContext() != null && !prompt.getContext().isBlank()) {
            sb.append("Context:\n").append(prompt.getContext()).append("\n\n");
        }

        sb.append("Prompt:\n").append("-".repeat(40)).append("\n");
        sb.append(prompt.getGeneratedPrompt()).append("\n");

        appendAnalysisSection(sb, prompt);

        return sb.toString();
    }

    /**
     * Builds a Markdown export of the prompt, formatted with headers
     * and metadata suitable for pasting into documentation, GitHub
     * READMEs, or note-taking apps.
     *
     * @param prompt the prompt record to export
     * @return the formatted Markdown content
     */
    private String buildMarkdown(Prompt prompt) {
        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(prompt.getTitle()).append("\n\n");

        if (prompt.getPromptType() != null) {
            sb.append("**Type:** ").append(prompt.getPromptType().getDisplayName()).append("  \n");
        }
        if (prompt.getCreatedAt() != null) {
            sb.append("**Created:** ").append(prompt.getCreatedAt().format(DATE_FORMAT)).append("  \n");
        }
        if (prompt.getTags() != null && !prompt.getTags().isEmpty()) {
            sb.append("**Tags:** ").append(String.join(", ", prompt.getTags())).append("  \n");
        }
        sb.append("\n");

        if (prompt.getContext() != null && !prompt.getContext().isBlank()) {
            sb.append("## Context\n\n").append(prompt.getContext()).append("\n\n");
        }

        sb.append("## Prompt\n\n```\n").append(prompt.getGeneratedPrompt()).append("\n```\n");

        if (prompt.getAnalysis() != null) {
            Prompt.AnalysisResult a = prompt.getAnalysis();
            sb.append("\n## Analysis\n\n");
            sb.append("| Metric | Score |\n|---|---|\n");
            sb.append("| Grammar | ").append(a.getGrammarScore()).append("/100 |\n");
            sb.append("| Clarity | ").append(a.getClarityScore()).append("/100 |\n");
            sb.append("| Context | ").append(a.getContextScore()).append("/100 |\n");
            sb.append("| Hallucination Risk | ").append(a.getHallucinationRisk()).append("/100 |\n");
            sb.append("| Complexity | ").append(a.getComplexityScore()).append("/100 |\n");
            sb.append("| **Overall** | **").append(a.getOverallScore()).append("/100** |\n");

            if (a.getSuggestions() != null && !a.getSuggestions().isEmpty()) {
                sb.append("\n### Suggestions\n\n");
                for (String suggestion : a.getSuggestions()) {
                    sb.append("- ").append(suggestion).append("\n");
                }
            }
        }

        return sb.toString();
    }

    /**
     * Builds a PDF export of the prompt using iText, with a title,
     * metadata block, and the full generated prompt text laid out on
     * a single flowing document.
     *
     * @param prompt the prompt record to export
     * @return the generated PDF file as a byte array
     * @throws ApiException with 500 INTERNAL_SERVER_ERROR if PDF generation fails
     */
    private byte[] buildPdf(Prompt prompt) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(outputStream);
            PdfDocument pdfDocument = new PdfDocument(writer);
            Document document = new Document(pdfDocument);

            Paragraph title = new Paragraph(prompt.getTitle())
                    .setBold()
                    .setFontSize(20)
                    .setTextAlignment(TextAlignment.LEFT);
            document.add(title);

            StringBuilder metaLine = new StringBuilder();
            if (prompt.getPromptType() != null) {
                metaLine.append("Type: ").append(prompt.getPromptType().getDisplayName());
            }
            if (prompt.getCreatedAt() != null) {
                if (!metaLine.isEmpty()) {
                    metaLine.append("   |   ");
                }
                metaLine.append("Created: ").append(prompt.getCreatedAt().format(DATE_FORMAT));
            }
            if (!metaLine.isEmpty()) {
                Paragraph meta = new Paragraph(metaLine.toString())
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY)
                        .setMarginBottom(15);
                document.add(meta);
            }

            if (prompt.getTags() != null && !prompt.getTags().isEmpty()) {
                Paragraph tags = new Paragraph("Tags: " + String.join(", ", prompt.getTags()))
                        .setFontSize(10)
                        .setFontColor(ColorConstants.GRAY)
                        .setMarginBottom(15);
                document.add(tags);
            }

            if (prompt.getContext() != null && !prompt.getContext().isBlank()) {
                document.add(new Paragraph("Context").setBold().setFontSize(13).setMarginTop(10));
                document.add(new Paragraph(prompt.getContext()).setFontSize(11));
            }

            document.add(new Paragraph("Prompt").setBold().setFontSize(13).setMarginTop(15));
            document.add(new Paragraph(prompt.getGeneratedPrompt()).setFontSize(11));

            if (prompt.getAnalysis() != null) {
                Prompt.AnalysisResult a = prompt.getAnalysis();
                document.add(new Paragraph("Analysis").setBold().setFontSize(13).setMarginTop(15));

                document.add(new Paragraph(
                        "Grammar: " + a.getGrammarScore() + "/100   |   " +
                        "Clarity: " + a.getClarityScore() + "/100   |   " +
                        "Context: " + a.getContextScore() + "/100"
                ).setFontSize(11));

                document.add(new Paragraph(
                        "Hallucination Risk: " + a.getHallucinationRisk() + "/100   |   " +
                        "Complexity: " + a.getComplexityScore() + "/100   |   " +
                        "Overall: " + a.getOverallScore() + "/100"
                ).setFontSize(11).setBold());

                if (a.getSuggestions() != null && !a.getSuggestions().isEmpty()) {
                    document.add(new Paragraph("Suggestions").setBold().setFontSize(12).setMarginTop(10));
                    for (String suggestion : a.getSuggestions()) {
                        document.add(new Paragraph("• " + suggestion).setFontSize(11));
                    }
                }
            }

            document.close();
            return outputStream.toByteArray();

        } catch (IOException ex) {
            throw new ApiException("Failed to generate PDF export", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Appends an analysis summary section to a plain text export, if
     * the prompt record has analysis data attached.
     *
     * @param sb     the StringBuilder to append to
     * @param prompt the prompt record being exported
     */
    private void appendAnalysisSection(StringBuilder sb, Prompt prompt) {
        if (prompt.getAnalysis() == null) {
            return;
        }
        Prompt.AnalysisResult a = prompt.getAnalysis();

        sb.append("\nAnalysis:\n").append("-".repeat(40)).append("\n");
        sb.append("Grammar Score: ").append(a.getGrammarScore()).append("/100\n");
        sb.append("Clarity Score: ").append(a.getClarityScore()).append("/100\n");
        sb.append("Context Score: ").append(a.getContextScore()).append("/100\n");
        sb.append("Hallucination Risk: ").append(a.getHallucinationRisk()).append("/100\n");
        sb.append("Complexity Score: ").append(a.getComplexityScore()).append("/100\n");
        sb.append("Overall Score: ").append(a.getOverallScore()).append("/100\n");

        if (a.getSuggestions() != null && !a.getSuggestions().isEmpty()) {
            sb.append("\nSuggestions:\n");
            List<String> suggestions = a.getSuggestions();
            for (int i = 0; i < suggestions.size(); i++) {
                sb.append(i + 1).append(". ").append(suggestions.get(i)).append("\n");
            }
        }
    }

    /**
     * Appends a single "Label: value" metadata line to a StringBuilder,
     * skipping it entirely if the value is null or blank.
     *
     * @param sb    the StringBuilder to append to
     * @param label the metadata field label
     * @param value the metadata field value (nullable)
     */
    private void appendMetadataLine(StringBuilder sb, String label, String value) {
        if (value != null && !value.isBlank()) {
            sb.append(label).append(": ").append(value).append("\n");
        }
    }
}