package mu.mns.demo.download.app2.controller;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.crypto.SecretKey;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@RestController
@RequestMapping("/file")
@CrossOrigin(origins = "*")
public class FileController {

    /**
     * Directory to store the uploaded files.
     */
    @Value("${mns.demo.document.path}")
    private String path;

    /**
     * Max buffer size in RAM memory before flushing to disk when uploading a file.
     */
    @Value("${mns.demo.document.upload.buffer-size}")
    private Integer uploadBufferSizeByte;

    /**
     * Max buffer size in RAM memory before flushing to the output stream when downloading a file.
     */
    @Value("${mns.demo.document.download.buffer-size}")
    private Integer downloadBufferSizeByte;

    /**
     * The JWT token secret key (For document download purposes).
     */
    @Value("${mns.demo.document.security.jwt-secret}")
    private String documentJwtTokenSecretKey;

    /**
     * REST Endpoint to ping the Spring Boot App 2.
     */
    @GetMapping
    public String hello() {
        return "Hello from App 2";
    }

    /**
     * <p>REST Endpoint to upload a file using {@link MultipartFile}.</p>
     * <p>
     * Set the property <b>spring.servlet.multipart.enabled=true</b> before calling this endpoint.
     *
     * @param file The file to be uploaded.
     */
    @PostMapping("/upload/with_multipart_file")
    public void uploadMultiPartFile(@RequestParam MultipartFile file) {
        log.info("[Multipart] Saving the file " + file.getOriginalFilename() + " to disk in the directory " + path);

        try (InputStream inputStream = file.getInputStream();
             BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(path + File.separator + file.getOriginalFilename()))) {
            byte[] buffer = new byte[uploadBufferSizeByte];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        log.info("[Multipart]" + file.getOriginalFilename() + " saved successfully to disk in the directory " + path);
    }

    /**
     * <p>REST Endpoint to upload a file without using {@link MultipartFile}.</p>
     * <p>
     * Set the property <b>spring.servlet.multipart.enabled=false</b> before calling this endpoint.
     *
     * @param request {@link HttpServletRequest}
     */
    @PostMapping("/upload/without_multipart_file")
    public void uploadWithoutMultiPartFile(HttpServletRequest request) {

        String filename = request.getHeader("filename");
        log.info("[Without MultipartFile] Saving the file " + filename + " to disk in the directory " + path);

        try (InputStream inputStream = request.getInputStream();
             BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(path + File.separator + filename))) {
            byte[] buffer = new byte[uploadBufferSizeByte];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        log.info("[Without MultipartFile]" + filename + " saved successfully to disk in the directory " + path);
    }

    /**
     * REST Endpoint to download a file from disk.
     *
     * @param filename The file to be downloaded (include extension).
     * @return {@link ResponseEntity} of {@link StreamingResponseBody}.
     */
    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> downloadFile(String filename) {
        log.info("[download] Downloading the file " + filename + " from the directory " + path);

        // Get content length
        String filePath = path + File.separator + filename;
        File file = new File(filePath);
        long contentLength = file.length();

        // Stream the file from disk to the output stream
        StreamingResponseBody response = outputStream -> {
            log.info("[download] Streaming to output stream");
            try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(filePath))) {
                byte[] buffer = new byte[downloadBufferSizeByte];
                int bytesRead;
                while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            log.info("[download] Streaming done");
        };

        // HTTP Response Header
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Disposition", "attachment; filename=" + filename);
        httpHeaders.add("Content-Length", String.valueOf(contentLength));

        return ResponseEntity.ok()
                .headers(httpHeaders)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(response);
    }

    /**
     * REST Endpoint to check if the user has the right to download a certain file. A short-lived JWT token will be
     * returned if the user is allowed to download the file.
     *
     * @param filename The file to be downloaded.
     * @return Short-lived JWT token
     */
    @GetMapping("/download/check")
    public ResponseEntity<String> checkDownload(String filename) {
        log.info("[download/check] Generating short-lived JWT token for downloading documents");

        File file = new File(path + File.separator + filename);
        if (!file.exists()) {
            throw new RuntimeException("File not found");
        }

        SecretKey key = Keys.hmacShaKeyFor(documentJwtTokenSecretKey.getBytes(StandardCharsets.UTF_8));
        String documentDownloadToken = Jwts.builder()
                .setIssuer("MNS")
                .setSubject("MNS Download Token")
                .setIssuedAt(new Date())
                .setExpiration(new Date((new Date()).getTime() + 120000)) // 2 mins
                .signWith(key)
                .compact();

        return ResponseEntity.ok(documentDownloadToken);
    }

    /**
     * REST Endpoint to download a file from disk if the provided download token is valid.
     *
     * @param filename The file to be downloaded (include extension).
     * @param downloadToken The download JWT token.
     * @return {@link ResponseEntity} of {@link StreamingResponseBody}.
     */
    @GetMapping("/download/token")
    public ResponseEntity<StreamingResponseBody> downloadFileWithDownloadToken(String filename, String downloadToken) {
        log.info("[download/token] Downloading the file " + filename + " from the directory " + path);

        // Verify download token
        SecretKey secretKey = Keys.hmacShaKeyFor(documentJwtTokenSecretKey.getBytes(StandardCharsets.UTF_8));
        Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(downloadToken)
                .getBody();

        // Get content length
        String filePath = path + File.separator + filename;
        File file = new File(filePath);
        long contentLength = file.length();

        // Stream the file from disk to the output stream
        StreamingResponseBody response = outputStream -> {
            log.info("[download/token] Streaming to output stream");
            try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(filePath))) {
                byte[] buffer = new byte[downloadBufferSizeByte];
                int bytesRead;
                while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            log.info("[download/token] Streaming done");
        };

        // HTTP Response Header
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Disposition", "attachment; filename=" + filename);
        httpHeaders.add("Content-Length", String.valueOf(contentLength));

        return ResponseEntity.ok()
                .headers(httpHeaders)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(response);
    }
}
