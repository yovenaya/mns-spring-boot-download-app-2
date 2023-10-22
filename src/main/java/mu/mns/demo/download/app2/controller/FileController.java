package mu.mns.demo.download.app2.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

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
     * Max buffer size in RAM memory before flushing to disk.
     */
    @Value("${mns.demo.document.upload.size-byte}")
    private Integer bufferSizeBytes;

    /**
     * REST Endpoint to ping the Spring Boot App 2.
     */
    @GetMapping
    public String hello() {
        return "Hello from App 2";
    }

    /**
     * <p>REST Endpoint to upload a file using {@link MultipartFile}.</p>
     *
     * Set the property <b>spring.servlet.multipart.enabled=true</b> before calling this endpoint.
     *
     * @param file The file to be uploaded.
     */
    @PostMapping("/with_multipart_file")
    public void uploadMultiPartFile(@RequestParam MultipartFile file) {
        log.info("[Multipart] Saving the file " + file.getOriginalFilename() + " to disk in the directory " + path);

        try (InputStream inputStream = file.getInputStream();
             BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(path + File.separator + file.getOriginalFilename()))) {
            byte[] buffer = new byte[bufferSizeBytes];
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
     *
     * Set the property <b>spring.servlet.multipart.enabled=false</b> before calling this endpoint.
     *
     * @param request {@link HttpServletRequest}
     */
    @PostMapping("/without_multipart_file")
    public void uploadWithoutMultiPartFile(HttpServletRequest request) {

        String filename = request.getHeader("filename");
        log.info("[Without MultipartFile] Saving the file " + filename + " to disk in the directory " + path);

        try (InputStream inputStream = request.getInputStream();
             BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(path + File.separator + filename))) {
            byte[] buffer = new byte[bufferSizeBytes];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        log.info("[Without MultipartFile]" + filename + " saved successfully to disk in the directory " + path);
    }
}
