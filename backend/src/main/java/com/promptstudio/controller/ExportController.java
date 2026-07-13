package com.promptstudio.controller;

import com.promptstudio.security.CustomUserDetailsService.UserPrincipal;
import com.promptstudio.service.ExportService;
import com.promptstudio.service.ExportService.ExportResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the Export feature. Unlike every other
 * controller in this application, these endpoints return a raw binary
 * file download rather than a JSON {@code ApiResponse} envelope, since
 * the client needs to trigger a browser download directly from the
 * response.
 */
@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
@Tag(name = "Export", description = "Export saved prompts to PDF, Markdown, or TXT")
public class ExportController {

    private final ExportService exportService;

    /**
     * Exports a saved prompt to the requested file format and streams
     * it back as a downloadable file.
     *
     * @param principal the authenticated user
     * @param promptId  the ID of the prompt to export
     * @param format    the desired format: "pdf", "markdown", or "txt" (case-insensitive)
     * @return a binary file download response with appropriate headers
     */
    @GetMapping("/{promptId}/{format}")
    @Operation(summary = "Export a saved prompt as PDF, Markdown, or TXT")
    public ResponseEntity<ByteArrayResource> exportPrompt(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String promptId,
            @PathVariable String format
    ) {
        ExportResult result = exportService.exportPrompt(principal.getUserId(), promptId, format);
        ByteArrayResource resource = new ByteArrayResource(result.fileBytes());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(result.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(result.fileName()).build().toString())
                .contentLength(result.fileBytes().length)
                .body(resource);
    }
}