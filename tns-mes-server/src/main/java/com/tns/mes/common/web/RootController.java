package com.tns.mes.common.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Public local entry point for operators; business APIs remain authenticated. */
@Controller
public class RootController {
    @GetMapping({"", "/"})
    public String index() {
        return "forward:/index.html";
    }
}
