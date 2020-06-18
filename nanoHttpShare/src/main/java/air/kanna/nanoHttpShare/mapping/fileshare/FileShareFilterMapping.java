package air.kanna.nanoHttpShare.mapping.fileshare;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
    private static final String DEFAULT_URI = "fileShare";
    
    private static final String DOWNLOAD_URI = "@orgDownload";
    private static final String ICON_FILE = "@file.png";
    private static final String ICON_FOLDER = "@folder.png";
    private static final String ICON_DOWNLOAD = "@download.png";
    
    private static final MappingFunction DEFAULT_FUNCTION = new MappingFunction("文件共享", DEFAULT_URI);
    
    private final Logger logger = LoggerProvider.getLogger(ShareHttpService.class);
    
    private File basePath;
    private String basePathStr;
    private MappingFunction function = DEFAULT_FUNCTION;
    
    private String listTitle = "文件列表";
    private String homeString = "根目录";
    
    public FileShareFilterMapping(String path, MappingFunction function) {
        if(StringTool.isAllSpacesString(path)) {
            throw new NullPointerException("Base path is null");
        }
        initBasePath(new File(path));
        initFunction(function);
    }
    
    public FileShareFilterMapping(File path, MappingFunction function) {
        initBasePath(path);
        initFunction(function);
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
    
    private void initFunction(MappingFunction function) {
        if(function == null) {
            return;
        }
        try {
            this.function = new MappingFunction(function.getFunctionName(), function.getFunctionUri());
        }catch(Exception e) {
            
        }
    }
    
    @Override
    public MappingFunction getFunction() {
        return new MappingFunction(function.getFunctionName(), function.getFunctionUri());
    }

    @Override
    public boolean isAccept(IHTTPSession session) {
        if(session.getMethod() != Method.GET) {
            return false;
        }
        String uri = session.getUri();
        if(uri.startsWith("/" + function.getFunctionUri())) {
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
                "        <center><h1>" + listTitle + "</h1></center>\r\n" + 
                "        <center><table class=\"tableMain\">\r\n");
        
        if(parent != null) {
            count++;
            builder.append(getParentHtml(path));
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
                .append(function.getFunctionUri()).append('/').append(item.isFile() ? ICON_FILE : ICON_FOLDER)
                .append("\"></td>\r\n");
            builder.append(
                    "                <td class=\"fileUrl\"><a href=\"")
                .append(itemPath[0]).append("\">").append(item.getName()).append("</a></td>\r\n");
            
            if(item.isFile() && itemPath[1] != null){
                builder.append(
                        "                <td class=\"tableIcon\">")
                    .append("<a href=\"").append(itemPath[1]).append("\">")
                    .append("<img class=\"iconStyle\" src=\"/")
                    .append(function.getFunctionUri()).append('/').append(ICON_DOWNLOAD)
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
    
    private String getParentHtml(File path) {
        List<File> parents = new ArrayList<>();
        File temp = path;
        StringBuilder builder = new StringBuilder();
        
        for(;!temp.getAbsolutePath().equals(basePath.getAbsolutePath()); temp = temp.getParentFile()) {
            parents.add(temp);
        }
        
        if(parents.size() <= 0) {
            builder.append("<tr><td class=\"tableIcon\"><img class=\"iconStyle\" src=\"/")
                .append(function.getFunctionUri()).append('/').append(ICON_FOLDER).append("\"></td>");
            builder.append("<td class=\"fileUrl\"><td><td class=\"tableIcon\"></td></tr>");
        }else {
            parents.add(basePath);
            
            builder.append(
                    "            <tr>\r\n" + 
                    "                <td class=\"tableIcon\"><img class=\"iconStyle\" src=\"/")
                .append(function.getFunctionUri()).append('/').append(ICON_FOLDER).append("\"></td>\r\n");
            builder.append(
                    "                <td class=\"fileUrl\">");
            for(int i=parents.size() - 1; i>=0; i--) {
                File file = parents.get(i);
                String parentStr = getFileUri(file)[0];
                String name = file.getName();
                
                //根目录显示专有名称
                if(i == parents.size() - 1) {
                    name = homeString;
                }
                //最后的目录就是当前目录，无需超链接
                if(parentStr == null || i == 0) {
                    builder.append(name);
                }else {
                    builder.append("<a href=\"").append(parentStr).append("\">").append(name).append("</a>");
                }
                builder.append(" / ");
            }
            
            builder.append("</td>\r\n");
            builder.append(
                    "                <td class=\"tableIcon\"></td>\r\n" +
                    "            </tr>\r\n");
        }
        return builder.toString();
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
            result[0] = '/' + function.getFunctionUri() + pathStr;
            result[1] = '/' + function.getFunctionUri() + '/' + DOWNLOAD_URI + pathStr;
        }else {
            result[0] = '/' + function.getFunctionUri() + '/' + pathStr;
            result[1] = '/' + function.getFunctionUri() + '/' + DOWNLOAD_URI + '/' + pathStr;
        }
        //URLDecoder.decode 里面，加号会被转成空格（JDK1.8是这样，其他版本不知道）
        if(result[0] != null) {
            result[0] = result[0].replaceAll("\\+", "%2B");
        }
        if(result[1] != null) {
            result[1] = result[1].replaceAll("\\+", "%2B");
        }
        return result;
    }
    
    private String getSubUri(String uri) {
        if(uri == null || uri.length() < (function.getFunctionUri().length() + 1)) {
            return null;
        }
        uri = uri.substring(function.getFunctionUri().length() + 1);
        if(uri.length() <= 0) {
            return "";
        }
        if(uri.charAt(0) == '/') {
            return uri.substring(1);
        }
        return uri;
    }

    public String getListTitle() {
        return listTitle;
    }

    public String getHomeString() {
        return homeString;
    }

    public void setListTitle(String listTitle) {
        this.listTitle = listTitle;
    }

    public void setHomeString(String homeString) {
        this.homeString = homeString;
    }
}
