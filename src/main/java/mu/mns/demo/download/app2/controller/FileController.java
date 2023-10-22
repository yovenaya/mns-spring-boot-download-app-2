package mu.mns.demo.download.app2.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;

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

    @Value("${mns.demo.document.download.buffer-size}")
    private Integer downloadBufferSizeByte;

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
     * @param filename The filename together with the extension of the file.
     * @return {@link ResponseEntity} of {@link StreamingResponseBody}.
     */
    @GetMapping("/download")
    public ResponseEntity<StreamingResponseBody> downloadFile(String filename) {
        log.info("[download] Downloading the file " + filename + " from the directory " + path);

        // Stream the file from disk to the output stream
        StreamingResponseBody response = outputStream -> {
            try (BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(path + File.separator + filename))) {
                byte[] buffer = new byte[downloadBufferSizeByte];
                int bytesRead;
                while ((bytesRead = bufferedInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        // HTTP Response Header
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Content-Disposition", "attachment; filename=" + filename);

        return ResponseEntity.ok()
                .headers(httpHeaders)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(response);
    }
}
