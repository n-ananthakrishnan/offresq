package com.offresq.gateway.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ForwardController {

  @GetMapping({"/", "/map"})
  public String index() {
    return "forward:/index.html";
  }
}