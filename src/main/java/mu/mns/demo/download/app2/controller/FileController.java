package mu.mns.demo.download.app2.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/file")
@CrossOrigin(origins = "*")
public class FileController {

    @GetMapping
    public String hello() {
        return "Hello";
    }
}
