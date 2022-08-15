package github.jlyyxd.controller;

import github.jlyyxd.Data;
import github.jlyyxd.MyService;
import github.jlyyxd.annotation.RpcReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class IndexController {

    @RpcReference(version = "1.0", group = "test")
    private MyService myService;

    @GetMapping("/index")
    public String index(){
        return myService.hello(new Data("show",21));
    }
}
