package com.tradingzone.services.load.reader.zipextra;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.zip.ZipInputStream;

class ZipResource extends InputStreamResource implements Resource {

    public ZipResource(Resource delegate) throws IOException {
        super(new ZipInputStream(delegate.getInputStream()));
    }
}
