package com.tradingzone.services.load.reader.zipextra;

import org.springframework.batch.item.file.BufferedReaderFactory;
import org.springframework.core.io.Resource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

public class ZipBufferedReaderFactory implements BufferedReaderFactory {
    private ZipResource zipResource;

    public ZipBufferedReaderFactory (ZipResource zipResource){
        this.zipResource = zipResource;
    }

    @Override
    public BufferedReader create(Resource resource, String encoding) throws UnsupportedEncodingException, IOException {

        return new BufferedReader(new InputStreamReader(zipResource.getInputStream()));

        //return new BufferedReader(new InputStreamReader(new ZipInputStream(resource.getInputStream())));

    }
}
