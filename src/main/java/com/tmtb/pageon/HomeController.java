package com.tmtb.pageon;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class HomeController {
    @GetMapping("/")
    public String home() {
        log.info("인덱스 페이지");
        return "index";
    }

    @GetMapping("/work_index")
    public String workPage() {
        log.info("작품 메인 페이지");
        return "work/work_index";
    }
}
