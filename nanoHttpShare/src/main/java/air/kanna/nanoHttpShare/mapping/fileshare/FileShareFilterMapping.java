package air.kanna.nanoHttpShare.mapping.fileshare;

import air.kanna.nanoHttpShare.ShareHttpService;
import air.kanna.nanoHttpShare.mapping.FilterMapping;
import air.kanna.nanoHttpShare.mapping.MappingFunction;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class FileShareFilterMapping implements FilterMapping {
    private static final String SHARE_URI = "fileShare";
    private static final MappingFunction FILE_SHARE = new MappingFunction("文件共享", SHARE_URI);
    
    private static final String ICON_FILE = "@file.png";
    private static final String ICON_FOLDER = "@folder.png";
    
    @Override
    public MappingFunction getFunction() {
        return FILE_SHARE;
    }

    @Override
    public boolean isAccept(IHTTPSession session) {
        if(session.getMethod() != Method.GET) {
            return false;
        }
        String uri = session.getUri();
        if(uri.startsWith("/" + SHARE_URI)) {
            return true;
        }
        return false;
    }

    @Override
    public Response response(IHTTPSession session) {
        if(!isAccept(session)) {
            return null;
        }
        String uri = getSubUri(session.getUri());
        
        StringBuilder builder = new StringBuilder();
        builder.append("<!DOCTYPE html>\n<html>\n<head>\n<style type=\"text/css\">\n.rootMenu {\nwidth: 90%;\nheight: 50px;\nmargin-top: 20px;\n")
            .append("font-size: large;\n}\n</style>\n</head><body><center><h1>NanoHttpShare</h1></center>\n");
        
        builder.append("</body>\n</html>\n");
        
        return ShareHttpService.newFixedLengthResponse(Status.OK, ShareHttpService.FIXED_MIME_HTML, builder.toString());
    }
    
    private String getSubUri(String uri) {
        if(uri == null || uri.length() < (SHARE_URI.length() + 1)) {
            return null;
        }
        uri = uri.substring(SHARE_URI.length());
        if(uri.length() <= 0) {
            return "";
        }
        if(uri.charAt(0) == '/') {
            return uri.substring(1);
        }
        return uri;
    }

}
