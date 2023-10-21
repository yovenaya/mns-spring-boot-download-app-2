package mu.mns.demo.download.app2.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;

@Slf4j
@RestController
@RequestMapping("/file")
@CrossOrigin(origins = "*")
public class FileController {

    @Value("${mns.demo.document.path}")
    private String path;

    @GetMapping
    public String hello() {
        return "Hello";
    }

    @GetMapping("/create-test-file")
    public void createTestFile(String filename, String content) {
        String path = this.path + File.separator + filename;
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path))) {
            bw.write(content);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
