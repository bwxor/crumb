package com.bwxor.piejfxsdk.service;

import java.io.InputStream;
import java.net.URL;

public class ResourceService {
    private static final String PACKAGE_NAME = "/com/bwxor/piejfxsdk/";

    public URL getResourceByName(String name) {
        String fullResourceName = name.startsWith(PACKAGE_NAME) ? name : PACKAGE_NAME + name;
        return ResourceService.class.getResource(fullResourceName);
    }

    public InputStream getResourceByNameAsStream(String name) {
        String fullResourceName = name.startsWith(PACKAGE_NAME) ? name : PACKAGE_NAME + name;
        return ResourceService.class.getResourceAsStream(fullResourceName);
    }
}
