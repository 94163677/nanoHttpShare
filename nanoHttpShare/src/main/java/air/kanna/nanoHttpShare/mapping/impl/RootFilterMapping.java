package air.kanna.nanoHttpShare.mapping.impl;

import java.util.ArrayList;
import java.util.List;

import air.kanna.nanoHttpShare.ShareHttpService;
import air.kanna.nanoHttpShare.logger.Logger;
import air.kanna.nanoHttpShare.logger.LoggerProvider;
import air.kanna.nanoHttpShare.mapping.FilterMapping;
import air.kanna.nanoHttpShare.mapping.FilterMappingUtil;
import air.kanna.nanoHttpShare.mapping.MappingFunction;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class RootFilterMapping implements FilterMapping {
    private final Logger logger = LoggerProvider.getLogger(ShareHttpService.class);
    
    public static final String ROOT = "/";
    public static final String ICON = "/favicon.ico";
    public static final String JQUERY = "/jquery.js";
    
    private List<MappingFunction> functions = new ArrayList<>();

    private String functionNotFound = "暂无功能可选";
    
    @Override
    public boolean isAccept(IHTTPSession session) {
        if(session.getMethod() != Method.GET) {
            return false;
        }
        String uri = session.getUri();
        switch(uri) {
            case ROOT: return true;
            case ICON: return true;
            case JQUERY: return true;
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
            case ICON: return FilterMappingUtil.getResourceResponse("air/kanna/nanoHttpShare/data/HttpShareIcon.ico");
            case JQUERY: return FilterMappingUtil.getResourceResponse("air/kanna/nanoHttpShare/data/jquery-3.5.1.min.js");
        }
        return null;
    }

    private Response getRootResponse(IHTTPSession session) {
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n<style type=\"text/css\">\n.rootMenu {\nwidth: 90%;\nheight: 50px;\nmargin-top: 20px;\n")
            .append("font-size: large;\n}\n</style>\n</head><body><center><h1>NanoHttpShare</h1></center>\n");
        
        if(functions.size() <= 0) {
            builder.append("<center><h1>").append(functionNotFound).append("</h1></center>\n");
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

    public List<MappingFunction> getFunctions() {
        return functions;
    }

    public void setFunctions(List<MappingFunction> functions) {
        this.functions = functions;
    }

    public String getFunctionNotFound() {
        return functionNotFound;
    }

    public void setFunctionNotFound(String functionNotFound) {
        this.functionNotFound = functionNotFound;
    }
}
