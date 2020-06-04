package air.kanna.nanoHttpShare.mapping.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import air.kanna.nanoHttpShare.ShareHttpService;
import air.kanna.nanoHttpShare.logger.Logger;
import air.kanna.nanoHttpShare.logger.LoggerProvider;
import air.kanna.nanoHttpShare.mapping.FilterMapping;
import air.kanna.nanoHttpShare.mapping.MappingFunction;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class RootFilterMapping implements FilterMapping {
    private final Logger logger = LoggerProvider.getLogger(ShareHttpService.class);
    
    private static final String ROOT = "/";
    private static final String ICON = "/favicon.ico";
    
    private List<MappingFunction> functions = new ArrayList<>();

    @Override
    public boolean isAccept(IHTTPSession session) {
        if(session.getMethod() != Method.GET) {
            return false;
        }
        String uri = session.getUri();
        switch(uri) {
            case ROOT: return true;
            case ICON: return true;
        }
        return false;
    }

    @Override
    public MappingFunction getFunction() {
        return null;
    }

    @Override
    public Response response(IHTTPSession session) {
        if(!isAccept(session)) {
            return null;
        }
        String uri = session.getUri();
        switch(uri) {
            case ROOT: return getRootResponse(session);
            case ICON: return getIconResponse(session);
        }
        return null;
    }

    private Response getRootResponse(IHTTPSession session) {
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html>\n<html>\n<head>\n<style type=\"text/css\">\n.rootMenu {\nwidth: 90%;\nheight: 50px;\nmargin-top: 20px;\n")
            .append("font-size: large;\n}\n</style>\n</head><body><center><h1>NanoHttpShare</h1></center>\n");
        
        if(functions.size() <= 0) {
            builder.append("<center><h1>暂无功能可选</h1></center>\n");
        }else {
            for(MappingFunction function : functions) {
                builder.append("<center><input type=\"button\" value=\"").append(function.getFunctionName())
                    .append("\" class=\"rootMenu\" onclick=\"window.open('").append(function.getFunctionUri())
                    .append("')\"></center>\n");
            }
        }
        builder.append("</body>\n</html>\n");
        
        return ShareHttpService.newFixedLengthResponse(Status.OK, ShareHttpService.FIXED_MIME_HTML, builder.toString());
    }
    
    private Response getIconResponse(IHTTPSession session) {
        try {
            InputStream ins = ClassLoader.getSystemResourceAsStream("air/kanna/nanoHttpShare/data/HttpShareIcon.ico");
            if(ins == null || ins.available() <= 0) {
                return ShareHttpService.newFixedLengthResponse(Status.NOT_FOUND, NanoHTTPD.MIME_HTML, "");
            }
            return ShareHttpService.newFixedLengthResponse(Status.OK, "image/x-icon", ins, ins.available());
        }catch(IOException e) {
            logger.warn("", e);
            return ShareHttpService.newFixedLengthResponse(Status.INTERNAL_ERROR, NanoHTTPD.MIME_HTML, e.getMessage());
        }
    }

    public List<MappingFunction> getFunctions() {
        return functions;
    }

    public void setFunctions(List<MappingFunction> functions) {
        this.functions = functions;
    }
}
