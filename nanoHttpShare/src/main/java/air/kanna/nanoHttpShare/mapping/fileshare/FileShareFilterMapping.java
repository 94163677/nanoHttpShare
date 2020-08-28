package air.kanna.nanoHttpShare.mapping.fileshare;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import air.kanna.nanoHttpShare.ShareHttpService;
import air.kanna.nanoHttpShare.logger.Logger;
import air.kanna.nanoHttpShare.logger.LoggerProvider;
import air.kanna.nanoHttpShare.mapping.FilterMapping;
import air.kanna.nanoHttpShare.mapping.FilterMappingUtil;
import air.kanna.nanoHttpShare.mapping.MappingFunction;
import air.kanna.nanoHttpShare.mapping.impl.RootFilterMapping;
import air.kanna.nanoHttpShare.util.StringTool;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import fi.iki.elonen.NanoHTTPD.ResponseException;

public class FileShareFilterMapping implements FilterMapping {
    private static final String DEFAULT_URI = "fileShare";
    
    private static final String DOWNLOAD_URI = "@orgDownload";
    private static final String UPLOAD_URI = "@upload";
    
    private static final String ICON_FILE = "@file.png";
    private static final String ICON_FOLDER = "@folder.png";
    private static final String ICON_DOWNLOAD = "@download.png";
    private static final String ICON_UPLOAD = "@upload.png";
    
    private static final String UPLOAD_PARAM = "uploadFile";
    
    private static final MappingFunction DEFAULT_FUNCTION = new MappingFunction("文件共享", DEFAULT_URI);
    
    private final Logger logger = LoggerProvider.getLogger(ShareHttpService.class);
    
    private String uploadUrl;
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
            function = DEFAULT_FUNCTION;
        }
        try {
            this.function = new MappingFunction(function.getFunctionName(), function.getFunctionUri());
            this.uploadUrl = "/" + function.getFunctionUri() + '/' + UPLOAD_URI;
        }catch(Exception e) {
            
        }
    }
    
    @Override
    public MappingFunction getFunction() {
        return new MappingFunction(function.getFunctionName(), function.getFunctionUri());
    }

    @Override
    public boolean isAccept(IHTTPSession session) {
        String uri = session.getUri();
        
        if(session.getMethod() == Method.GET) {
            if(uri.startsWith("/" + function.getFunctionUri())) {
                return true;
            }
        }
        if(session.getMethod() == Method.POST) {
            if(uri.startsWith(uploadUrl + '/')) {
                return true;
            }
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
            return ShareHttpService.newFixedLengthResponse(Status.FORBIDDEN, NanoHTTPD.MIME_HTML, uri);
        }
        
        switch(uri) {
            case ICON_FILE: return FilterMappingUtil.getResourceResponse("air/kanna/nanoHttpShare/mapping/fileshare/data/HttpShareFileIcon.png");
            case ICON_FOLDER: return FilterMappingUtil.getResourceResponse("air/kanna/nanoHttpShare/mapping/fileshare/data/HttpShareFolderIcon.png");
            case ICON_DOWNLOAD: return FilterMappingUtil.getResourceResponse("air/kanna/nanoHttpShare/mapping/fileshare/data/HttpShareDownloadIcon.png");
            case ICON_UPLOAD: return FilterMappingUtil.getResourceResponse("air/kanna/nanoHttpShare/mapping/fileshare/data/HttpShareUploadIcon.png");
            default: {
                if(uri.startsWith(UPLOAD_URI + '/')) {
                    return uploadFileResponse(session, uri);
                }
                return getFilePathResponse(uri);
            }
        }
    }
    
    private Response uploadFileResponse(IHTTPSession session, String uri) {
        Map<String, String> params = new HashMap<String, String>();
        
        //uri检查
        String sessionUri = session.getUri();
        if(!sessionUri.endsWith(uri) || !uri.startsWith(UPLOAD_URI + '/')) {
            logger.error("Cannot parse Uri: " + uri);
            return ShareHttpService.newFixedLengthResponse(Status.NOT_FOUND, ShareHttpService.FIXED_MIME_HTML, "URI NOT FOUND: " + uri);
        }
        uri = uri.substring(UPLOAD_URI.length() + 1);
        
        //实际目录检查
        File uploadPath = null;
        if(StringTool.isAllSpacesString(uri)) {
            uploadPath = basePath;
        }else {
            uploadPath = new File(basePath, uri);
        }
        if(!uploadPath.isDirectory() || !uploadPath.exists()) {
            logger.error("Upload Path Not Found: " + uploadPath.getAbsolutePath());
            return ShareHttpService.newFixedLengthResponse(Status.NOT_FOUND, ShareHttpService.FIXED_MIME_HTML, "URI NOT FOUND: " + uri);
        }
        
        //加载参数和文件到临时文件区，获取临时文件目录和上传文件名称
        String tempPath = null, fileName = null;
        try {
            session.parseBody(params);
            Set<String> keys = params.keySet();
            for(String key: keys){
                logger.info("key: " + key);
                logger.info("value: " + params.get(key));
                if(UPLOAD_PARAM.equalsIgnoreCase(key)) {
                    tempPath = params.get(key);
                }
            }
        }catch (IOException | ResponseException e) {
            logger.error("Cannot parse message", e);
            return ShareHttpService.newFixedLengthResponse(Status.INTERNAL_ERROR, ShareHttpService.FIXED_MIME_HTML, e.getMessage());
        }
        Map<String, List<String>> httpParams = session.getParameters();
        List<String> fileNames = httpParams.get(UPLOAD_PARAM);
        fileName = (fileNames == null || fileNames.size() <= 0) ? null : fileNames.get(0);
        
        //检查临时文件和上传文件名称
        if(StringTool.isNullString(tempPath) || StringTool.isNullString(fileName)) {
            logger.error("Cannot found temp path or upload file name");
            return ShareHttpService.newFixedLengthResponse(Status.INTERNAL_ERROR, ShareHttpService.FIXED_MIME_HTML, "Cannot found temp path or upload file name");
        }
        File tempFile = new File(tempPath);
        if(!tempFile.isFile() || !tempFile.exists()) {
            logger.error("Cannot found temp file: " + tempFile.getAbsolutePath());
            return ShareHttpService.newFixedLengthResponse(Status.INTERNAL_ERROR, ShareHttpService.FIXED_MIME_HTML, "Cannot found temp file");
        }
        
        //获取文件系统没有的文件，并复制
        File copyFile = getNotExists(uploadPath, fileName);
        try {
            fileCopy(tempFile, copyFile);
        } catch (Exception e) {
            logger.error("Copy File error: " + e.getMessage(), e);
            return ShareHttpService.newFixedLengthResponse(Status.INTERNAL_ERROR, ShareHttpService.FIXED_MIME_HTML, "Upload File error");
        }
        
        return ShareHttpService.newFixedLengthResponse(Status.OK, NanoHTTPD.MIME_HTML, Status.OK.name());
    }
    
    
    protected void fileCopy(File src, File dec) throws Exception{
        Files.copy(src.toPath(), dec.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
    }
    
    /**
     * 根据文件名，生成不存在的文件对象
     * @param fileName
     * @return
     */
    private File getNotExists(File uploadPath, String fileName) {
        if(uploadPath == null || !uploadPath.exists() 
                || !uploadPath.isDirectory() || StringTool.isNullString(fileName)) {
            return null;
        }
        String name = "", end = "";
        int idx = fileName.lastIndexOf('.');
        
        if(idx > 0 && idx < (fileName.length() - 1)) {
            name = fileName.substring(0, idx);
            end = fileName.substring(idx + 1);
        }else {
            name = fileName;
        }
        
        File file = new File(uploadPath, fileName);
        for(int i=1; file.exists(); i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append('(').append(i).append(')');
            
            if(!StringTool.isNullString(end)) {
                sb.append('.').append(end);
            }
            file = new File(uploadPath, sb.toString());
        }
        return file;
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
        
        //创建html头，包括导入的js和css
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
                "            .txt{ height:28px;line-height:28px; border:1px solid #cdcdcd; width:180px;}\r\n" +
                "            .btn{width:70px; color:#fff;background-color:#3598dc; border:0 none;height:28px; line-height:16px!important;cursor:pointer;}\r\n" +
                "            .btn:hover{background-color:#63bfff;color:#fff;}\r\n" +
                "            .file{ position:absolute; top:0; right:85px; height:30px;line-height:30px; filter:alpha(opacity:0);opacity: 0;width:254px }\r\n" +
                "        </style>\r\n" + 
                "        <script src=\"" + RootFilterMapping.JQUERY + "\" type=\"text/javascript\"></script>\r\n" + 
                "    </head>\r\n" + 
                "    <body>\r\n" + 
                "        <center><h1>" + listTitle + "</h1></center>\r\n" + 
                "        <iframe name=\"frame1\" frameborder=\"0\" height=\"40\" style=\"display: none;\"></iframe>\r\n" +
                "        <center><table class=\"tableMain\">\r\n");

        //创建文件上传
        String uploadPath = encodePathURL(getBasePathFromAbsolutePath(path));
        builder.append("            <tr class=\"").append(count % 2 == 1 ? "sepHighLight" : "").append("\">\r\n");
        builder.append("                <td class=\"tableIcon\"></td>\r\n");
        builder.append("                <td class=\"fileUrl\"><form action=\"");
        builder.append(uploadUrl);
        if(!uploadPath.startsWith("/")) {
            builder.append('/');
        }
        builder.append(uploadPath);
        builder.append("\" method=\"post\" target=\"frame1\" enctype ='multipart/form-data'>\r\n" + 
//                "            <input type ='text' id='filePath' name='filePath' class=\"txt\" readonly/>\r\n" + 
//                "            <input type=\"button\" id=\"selectBtn\" class=\"btn\" value=\"浏览...\" />\r\n" + 
//                "            <input type=\"file\" name=\"file\" class=\"file\" id=\"fileField\" onchange=\"document.getElementById('filePath').value=this.files[0].name\"/>\r\n" +
                "            <input type=\"file\" name=\"" + UPLOAD_PARAM + "\"/>\r\n" + 
                "            <input type=\"submit\" name=\"submit\" class=\"btn\" value=\"上传\">\r\n" + 
                "        </form></td>\r\n");
        builder.append("                <td class=\"tableIcon\"></td>\r\n");
        builder.append("            </tr>\r\n");
        count++;
        
        //创建目录导航条
        if(parent != null) {
            count++;
            builder.append(getParentHtml(path));
        }
        
        //创建文件列表
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
                "    <script type=\"text/javascript\">\r\n" + 
                "    </script>\r\n" + 
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
    
    /**
     * 获取一个文件在本页面的访问路径
     * 返回一个长度为2的字符串数组
     * 第一个为直接访问路径，文件格式根据文件后缀正确返回给浏览器处理
     * 第二个为下载路径，文件格式直接写死二进制，浏览器作为下载文件处理
     * @param path
     * @return
     */
    private String[] getFileUri(File path) {
        String pathStr = getBasePathFromAbsolutePath(path);
        String[] result = new String[] {null, null};
        
        if(pathStr == null) {
            return result;
        }
        
        if(pathStr.charAt(0) == '/') {
            result[0] = '/' + function.getFunctionUri() + pathStr;
            result[1] = '/' + function.getFunctionUri() + '/' + DOWNLOAD_URI + pathStr;
        }else {
            result[0] = '/' + function.getFunctionUri() + '/' + pathStr;
            result[1] = '/' + function.getFunctionUri() + '/' + DOWNLOAD_URI + '/' + pathStr;
        }
        
        if(result[0] != null) {
            result[0] = encodePathURL(result[0]);
        }
        if(result[1] != null) {
            result[1] = encodePathURL(result[1]);
        }
        return result;
    }
    
    /**
     * 编码URL里面的不可显示字符
     * @param url
     * @return
     */
    private String encodePathURL(String url) {
        if(StringTool.isNullString(url)) {
            return "";
        }
      //URLDecoder.decode 里面，加号会被转成空格（JDK1.8是这样，其他版本不知道）
        return url.replaceAll("\\+", "%2B");
    }
    
    /**
     * 根据文件实际路径，获取相对路径的字符串。
     * 该字符串未编码，如需加入html中，应先用encodePathURL方法编码
     * @param path
     * @return
     */
    private String getBasePathFromAbsolutePath(File path) {
        if(path == null) {
            logger.error("Path is null");
            return null;
        }
        
        String pathStr = path.getAbsolutePath().replaceAll("\\\\", "/");
        
        if(!pathStr.startsWith(basePathStr)) {
            logger.error("Not match base path file: " + pathStr);
            return null;
        }
        if(pathStr.equalsIgnoreCase(basePathStr)) {
            pathStr = "/";
        }else {
            pathStr = pathStr.substring(basePathStr.length());
        }
        return pathStr;
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
