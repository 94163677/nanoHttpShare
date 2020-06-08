package air.kanna.nanoHttpShare.mapping;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import air.kanna.nanoHttpShare.ShareHttpService;
import air.kanna.nanoHttpShare.logger.Logger;
import air.kanna.nanoHttpShare.logger.LoggerProvider;
import air.kanna.nanoHttpShare.util.MIMEUtil;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class FilterMappingUtil {
    private static final Logger logger = LoggerProvider.getLogger(FilterMappingUtil.class);
    
    public static Response getResourceResponse(String resourcePath) {
        try {
            InputStream ins = ClassLoader.getSystemResourceAsStream(resourcePath);
            if(ins == null || ins.available() <= 0) {
                return ShareHttpService.newFixedLengthResponse(Status.NOT_FOUND, NanoHTTPD.MIME_HTML, "");
            }
            
            return ShareHttpService.newFixedLengthResponse(
                    Status.OK, MIMEUtil.getMIMEWithDefault(resourcePath), ins, ins.available());
        }catch(IOException e) {
            logger.warn("getResourceResponse error: " + resourcePath, e);
            return ShareHttpService.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, e.getMessage());
        }
    }
    
    public static Response getFileResponse(File file) {
        try {
            if(file == null || !file.exists() || !file.isFile()) {
                return null;
            }
            InputStream ins = new BufferedInputStream(new FileInputStream(file), 10240);
            String mime = MIMEUtil.getMIMEWithDefault(file);
            
            return ShareHttpService.newFixedLengthResponse(Status.OK, mime, ins, file.length());
        }catch(IOException e) {
            logger.warn("getFileResponse error: " + file, e);
            return ShareHttpService.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, e.getMessage());
        }
    }
    
    public static Response getFileDownloadResponse(File file) {
        try {
            if(file == null || !file.exists() || !file.isFile()) {
                return null;
            }
            InputStream ins = new BufferedInputStream(new FileInputStream(file), 10240);
            return ShareHttpService.newFixedLengthResponse(Status.OK, MIMEUtil.getDefaultMIME(), ins, file.length());
        }catch(IOException e) {
            logger.warn("getFileResponse error: " + file, e);
            return ShareHttpService.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, e.getMessage());
        }
    }
}
