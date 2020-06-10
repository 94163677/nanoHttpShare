package air.kanna.nanoHttpShare.mapping.fileshare;

import java.io.File;

import air.kanna.nanoHttpShare.ShareHttpService;
import air.kanna.nanoHttpShare.logger.Logger;
import air.kanna.nanoHttpShare.logger.LoggerProvider;
import air.kanna.nanoHttpShare.mapping.FilterMapping;
import air.kanna.nanoHttpShare.mapping.FilterMappingUtil;
import air.kanna.nanoHttpShare.mapping.MappingFunction;
import air.kanna.nanoHttpShare.util.StringTool;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class FileShareFilterMapping implements FilterMapping {
    private static final String SHARE_URI = "fileShare";
    private static final String DOWNLOAD_URI = "@orgDownload";
    private static final MappingFunction FILE_SHARE = new MappingFunction("文件共享", SHARE_URI);
    
    private static final String ICON_FILE = "@file.png";
    private static final String ICON_FOLDER = "@folder.png";
    private static final String ICON_DOWNLOAD = "@download.png";
    
    private final Logger logger = LoggerProvider.getLogger(ShareHttpService.class);
    
    private File basePath;
    private String basePathStr;
    
    public FileShareFilterMapping(String path) {
        if(StringTool.isAllSpacesString(path)) {
            throw new NullPointerException("Base path is null");
        }
        initBasePath(new File(path));
    }
    
    public FileShareFilterMapping(File path) {
        initBasePath(path);
    }
    
    private void initBasePath(File path) {
        if(path == null) {
            throw new NullPointerException("Base path is null");
        }
        if(!path.exists() || !path.isDirectory()) {
            throw new java.lang.IllegalArgumentException("Base path is not exists or not a directory");
        }
        this.basePath = path;
        this.basePathStr = path.getAbsolutePath().replaceAll("\\\\", "/");
    }
    
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
        if(!checkURI(uri)) {
            return ShareHttpService.newFixedLengthResponse(Status.FORBIDDEN, NanoHTTPD.MIME_HTML, "");
        }
        
        switch(uri) {
            case ICON_FILE: return FilterMappingUtil.getResourceResponse("air/kanna/nanoHttpShare/mapping/fileshare/data/HttpShareFileIcon.png");
            case ICON_FOLDER: return FilterMappingUtil.getResourceResponse("air/kanna/nanoHttpShare/mapping/fileshare/data/HttpShareFolderIcon.png");
            case ICON_DOWNLOAD: return FilterMappingUtil.getResourceResponse("air/kanna/nanoHttpShare/mapping/fileshare/data/HttpShareDownloadIcon.png");
            default: return getFilePathResponse(uri);
        }
    }
    
    /**
     * 检查URI，看看有没有什么非法URI
     * 当前检查是有没有“.”和“..”的路径，防止越权
     * @param uri
     * @return
     */
    private boolean checkURI(String uri) {
        if(StringTool.isAllSpacesString(uri)) {
            return true;
        }
        String[] sepUri = uri.replaceAll("\\\\", "/").split("/");
        for(String subName: sepUri) {
            if(StringTool.isAllSpacesString(subName)) {
                continue;
            }
            if(".".equals(subName)) {
                return false;
            }
            if("..".equals(subName)) {
                return false;
            }
        }
        return true;
    }
    
    private Response getFilePathResponse(String path) {
        File showPath = null;
        
        if(StringTool.isAllSpacesString(path)) {
            showPath = basePath;
        }else {
            if(path.startsWith(DOWNLOAD_URI)) {
                return getOrgDownload(path);
            }
            showPath = new File(basePath, path);
        }
        
        if(!showPath.exists()) {
            return null;
        }
        
        if(showPath.isFile()) {
            return FilterMappingUtil.getFileResponse(showPath);
        }else {
            return getFilePathResponse(showPath);
        }
    }
    
    private Response getOrgDownload(String path) {
        path = path.substring(DOWNLOAD_URI.length());
        
        if(StringTool.isAllSpacesString(path)) {
            return null;
        }

        if(path.charAt(0) == '/') {
            path = path.substring(1);
        }
        if(StringTool.isAllSpacesString(path)) {
            return null;
        }
        
        File file = new File(basePath, path);
        return FilterMappingUtil.getFileDownloadResponse(file);
    }
    
    
    private Response getFilePathResponse(File path) {
        StringBuilder builder = new StringBuilder();
        File parent = null;
        int count = 0;
        
        if(path != basePath) {
            parent = path.getParentFile();
        }
        
        builder.append("<!DOCTYPE html>\r\n" + 
                "<html>\r\n" + 
                "    <head>\r\n" + 
                "        <meta charset=\"UTF-8\">\r\n" + 
                "        <style type=\"text/css\">\r\n" + 
                "            .tableMain{\r\n" + 
                "                table-layout:fixed;\r\n" + 
                "                width: 98%;\r\n" + 
                "            }\r\n" + 
                "            .tableIcon{\r\n" + 
                "                width: 36px;\r\n" + 
                "                text-align: center;\r\n" + 
                "            }\r\n" + 
                "            .iconStyle{\r\n" + 
                "                width: 28px;\r\n" + 
                "            }\r\n" + 
                "            .fileUrl{\r\n" + 
                "                font-size: larger;\r\n" + 
                "            }\r\n" + 
                "            .sepHighLight{\r\n" + 
                "                background: #eeeeee;\r\n" + 
                "            }\r\n" + 
                "        </style>\r\n" + 
                "    </head>\r\n" + 
                "    <body>\r\n" + 
                "        <center><h1>文件列表</h1></center>\r\n" + 
                "        <center><table class=\"tableMain\">\r\n");
        
        if(parent != null) {
            String parentStr = getFileUri(parent)[0];
            if(parentStr != null) {
                count++;
                builder.append(
                        "            <tr>\r\n" + 
                        "                <td class=\"tableIcon\"><img class=\"iconStyle\" src=\"/")
                    .append(SHARE_URI).append('/').append(ICON_FOLDER)
                    .append("\"></td>\r\n");
                builder.append(
                        "                <td class=\"fileUrl\"><a href=\"")
                    .append(parentStr).append("\">上一级</a></td>\r\n");
                builder.append(
                        "                <td class=\"tableIcon\"></td>\r\n" +
                        "            </tr>\r\n");
                
            }
        }
        
        File[] list = path.listFiles();
        for(File item: list) {
            if(item == null 
                    || !item.exists()
                    || item.getName().equals(".") 
                    || item.getName().equals("..")) {
                continue;
            }
            
            String[] itemPath = getFileUri(item);
            if(itemPath == null || itemPath[0] == null) {
                continue;
            }
            
            builder.append("            <tr class=\"").append(count % 2 == 1 ? "sepHighLight" : "").append("\">\r\n");
            builder.append(
                    "                <td class=\"tableIcon\"><img class=\"iconStyle\" src=\"/")
                .append(SHARE_URI).append('/').append(item.isFile() ? ICON_FILE : ICON_FOLDER)
                .append("\"></td>\r\n");
            builder.append(
                    "                <td class=\"fileUrl\"><a href=\"")
                .append(itemPath[0]).append("\">").append(item.getName()).append("</a></td>\r\n");
            
            if(item.isFile() && itemPath[1] != null){
                builder.append(
                        "                <td class=\"tableIcon\">")
                    .append("<a href=\"").append(itemPath[1]).append("\">")
                    .append("<img class=\"iconStyle\" src=\"/")
                    .append(SHARE_URI).append('/').append(ICON_DOWNLOAD)
                    .append("\"></a></td>\r\n");
            }else {
                builder.append(
                        "                <td class=\"tableIcon\"></td>\r\n");
            }
            
            builder.append("            </tr>\r\n");
            count++;
        }
                
        builder.append("        </table></center>\r\n" + 
                "    </body>\r\n" + 
                "</html>\r\n");
        
        return ShareHttpService.newFixedLengthResponse(Status.OK, ShareHttpService.FIXED_MIME_HTML, builder.toString());
    }
    
    private String[] getFileUri(File path) {
        String pathStr = path.getAbsolutePath().replaceAll("\\\\", "/");
        String[] result = new String[] {null, null};
        
        if(!pathStr.startsWith(basePathStr)) {
            logger.error("Not match base path file: " + pathStr);
            return result;
        }
        
        if(pathStr.equalsIgnoreCase(basePathStr)) {
            pathStr = "/";
        }else {
            pathStr = pathStr.substring(basePathStr.length());
        }
        
        if(pathStr.charAt(0) == '/') {
            result[0] = '/' + SHARE_URI + pathStr;
            result[1] = '/' + SHARE_URI + '/' + DOWNLOAD_URI + pathStr;
        }else {
            result[0] = '/' + SHARE_URI + '/' + pathStr;
            result[1] = '/' + SHARE_URI + '/' + DOWNLOAD_URI + '/' + pathStr;
        }
        return result;
    }
    
    private String getSubUri(String uri) {
        if(uri == null || uri.length() < (SHARE_URI.length() + 1)) {
            return null;
        }
        uri = uri.substring(SHARE_URI.length() + 1);
        if(uri.length() <= 0) {
            return "";
        }
        if(uri.charAt(0) == '/') {
            return uri.substring(1);
        }
        return uri;
    }
}
