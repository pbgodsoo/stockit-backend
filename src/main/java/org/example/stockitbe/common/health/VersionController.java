package org.example.stockitbe.common.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class VersionController {

    @GetMapping(value = "/version", produces = "text/plain;charset=UTF-8")
    public String version() {
        return "v3";
    }
}
