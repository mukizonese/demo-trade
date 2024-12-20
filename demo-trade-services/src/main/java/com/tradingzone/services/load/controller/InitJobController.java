package com.tradingzone.services.load.controller;

import com.tradingzone.services.load.job.InitAwsJob;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tradingzone/load")
public class InitJobController {

    @Autowired
    private InitAwsJob initAwsJob;

    @CrossOrigin(origins = "*")
    @GetMapping("/loaddataall/")
    public String loadDataAll(){
        return initAwsJob.loadDataAll();
    }

    @CrossOrigin(origins = "*")
    @GetMapping("/loaddata/")
    public String loadData(@RequestParam List<String> files){
        return initAwsJob.loadData(files);
    }
}
