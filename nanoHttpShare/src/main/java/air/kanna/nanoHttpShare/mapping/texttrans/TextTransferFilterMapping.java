package air.kanna.nanoHttpShare.mapping.texttrans;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import air.kanna.nanoHttpShare.ShareHttpService;
import air.kanna.nanoHttpShare.logger.Logger;
import air.kanna.nanoHttpShare.logger.LoggerProvider;
import air.kanna.nanoHttpShare.mapping.FilterMapping;
import air.kanna.nanoHttpShare.mapping.FilterMappingUtil;
import air.kanna.nanoHttpShare.mapping.MappingFunction;
import air.kanna.nanoHttpShare.util.StringTool;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Method;
import fi.iki.elonen.NanoHTTPD.Response;
import fi.iki.elonen.NanoHTTPD.Response.Status;
import fi.iki.elonen.NanoHTTPD.ResponseException;

public class TextTransferFilterMapping implements FilterMapping {
    private static final String DEFAULT_URI = "textTransfer";
    
    private static final String SUBMIT_URI = "sendText";
    private static final String MESSAGE_URI = "message";
    private static final String JQUERY_URI = "@jquery";
    
    private static final String MESSAGE_PARAM = "messageText";
    
    private static final MappingFunction DEFAULT_FUNCTION = new MappingFunction("文字传输", DEFAULT_URI);
    
    private final Logger logger = LoggerProvider.getLogger(TextTransferFilterMapping.class);
    
    private List<String> textList;
    private MappingFunction function = DEFAULT_FUNCTION;
    private String indexUri;
    private String scriptUri;
    private String messageUri;
    private String submitUri;
    
    private String htmlStr = null;
    
    public TextTransferFilterMapping() {
        textList = new ArrayList<>();
        initFunction(DEFAULT_FUNCTION);
    }
    
    public TextTransferFilterMapping(MappingFunction function) {
        textList = new ArrayList<>();
        initFunction(function);
    }
    
    @Override
    public MappingFunction getFunction() {
        return new MappingFunction(function.getFunctionName(), function.getFunctionUri());
    }

    @Override
    public boolean isAccept(IHTTPSession session) {
        String uri = session.getUri();
        
        if(session.getMethod() == Method.GET) {
            if(uri.equals(indexUri)) {
                return true;
            }else
            if(uri.equals(messageUri)) {
                return true;
            }else
            if(uri.equals(scriptUri)) {
                return true;
            }
        }
        if(session.getMethod() == Method.POST) {
            if(uri.equals(submitUri)){
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
        String uri = session.getUri();
        
        if(uri.equals(scriptUri)) {
            return FilterMappingUtil.getResourceResponse("air/kanna/nanoHttpShare/mapping/texttrans/data/jquery-3.5.1.min.js");
        }
        
        if(uri.equals(messageUri)) {
            return ShareHttpService.newFixedLengthResponse(Status.OK, ShareHttpService.FIXED_MIME_HTML, getMessageList());
        }
        if(uri.equals(indexUri)) {
            return ShareHttpService.newFixedLengthResponse(Status.OK, ShareHttpService.FIXED_MIME_HTML, getIndexString());
        }
        if(uri.equals(submitUri)) {
            return addMessage(session);
        }
        
        return null;
    }

    private void initFunction(MappingFunction function) {
        if(function == null) {
            return;
        }
        try {
            this.function = new MappingFunction(function.getFunctionName(), function.getFunctionUri());
        }catch(Exception e) {
            
        }
        indexUri = '/' + this.function.getFunctionUri();
        submitUri = indexUri + '/' + SUBMIT_URI;
        scriptUri = indexUri + '/' + JQUERY_URI;
        messageUri = indexUri + '/' + MESSAGE_URI;
    }
    
    private Response addMessage(IHTTPSession session) {
        Map<String, String> params = new HashMap<String, String>();
        try {
            session.parseBody(params);
            String message = session.getParms().get(MESSAGE_PARAM);
            
            if(!StringTool.isAllSpacesString(message)) {
                textList.add(message);
            }
            
            return ShareHttpService.newFixedLengthResponse(Status.OK, ShareHttpService.FIXED_MIME_HTML, "");
        } catch (IOException | ResponseException e) {
            logger.error("Cannot parse message", e);
            return ShareHttpService.newFixedLengthResponse(Status.INTERNAL_ERROR, ShareHttpService.FIXED_MIME_HTML, e.getMessage());
        }
    }
    
    private String getMessageList() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        for(String text : textList) {
            text = text.replaceAll("\\\\", "\\\\\\\\");
            text = text.replaceAll("\\\"", "\\\\\"");
            sb.append('\"').append(text).append('\"').append(',');
        }
        if(textList.size() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }
        sb.append(']');
        return sb.toString();
    }
    
    private String getIndexString() {
        if(htmlStr != null) {
            return htmlStr;
        }
        htmlStr = "<!DOCTYPE html>\r\n" + 
                "<html>\r\n" + 
                "    <head>\r\n" + 
                "        <meta charset=\"UTF-8\">\r\n" + 
                "        <style type=\"text/css\">\r\n" + 
                "            .tableMain{\r\n" + 
                "                table-layout:fixed;\r\n" + 
                "                width: 98%;\r\n" + 
                "            }\r\n" + 
                "            .tableIcon{\r\n" + 
                "                width: 120px;\r\n" + 
                "            }\r\n" + 
                "            .messageDiv{\r\n" + 
                "                width: 100%;\r\n" + 
                "                height: 600px;\r\n" + 
                "                background-color: #eee;\r\n" + 
                "                overflow: auto;\r\n" + 
                "            }\r\n" + 
                "            .messageBox{\r\n" + 
                "                width: 99%;\r\n" + 
                "                height: 56px;\r\n" + 
                "            }\r\n" + 
                "            .messageEdit{\r\n" + 
                "                width: 98%;\r\n" + 
                "                height: 60px;\r\n" + 
                "                background-color: #ccc;\r\n" + 
                "                border: 1px solid black;\r\n" + 
                "                white-space: nowrap;\r\n" + 
                "                overflow: hidden;\r\n" + 
                "                position: absolute;\r\n" + 
                "                bottom: 5px;\r\n" + 
                "            }\r\n" + 
                "            .messageArea{\r\n" + 
                "                width: 100%;\r\n" + 
                "                height: 54px;\r\n" + 
                "            }\r\n" + 
                "        </style>\r\n" + 
                "        <script src=\"" + scriptUri + "\" type=\"text/javascript\"></script>\r\n" + 
                "    </head>\r\n" + 
                "    <body>" +
                "        <div id=\"logs\" class=\"messageDiv\">\r\n" + 
                "                <textarea type=\"text\" class=\"messageBox\">DDDDDDDDDDDD</textarea>\r\n" + 
                "                <textarea type=\"text\" class=\"messageBox\">DDDDDDDDDDDD</textarea>\r\n" + 
                "                <textarea type=\"text\" class=\"messageBox\">DDDDDDDDDDDD</textarea>\r\n" + 
                "        </div>\r\n" + 
                "        <div class=\"messageEdit\">\r\n" + 
                "            <table class=\"tableMain\">\r\n" + 
                "                <tr>\r\n" + 
                "                    <td><textarea id=\"messageText\" name=\"logText\" type=\"text\" class=\"messageArea\"></textarea></td>\r\n" + 
                "                    <td class=\"tableIcon\"><input id=\"sendBtn\" type=\"button\" value=\" 发送 \" class=\"tableIcon\" style=\"height: 54px;\"></td>\r\n" + 
                "                </tr>\r\n" + 
                "            </table>\r\n" + 
                "        </div>\r\n" + 
                "    </body>\r\n" + 
                "    <script type=\"text/javascript\">\r\n" + 
                "        $(\"#sendBtn\").click(function(){\r\n" + 
                "            $.post(\r\n" + 
                "                \"" + submitUri + "\", \r\n" + 
                "                {" + MESSAGE_PARAM + ": $('#messageText').val()},\r\n" + 
                "                function(data, textStatus){\r\n" + 
                "                    $('#messageText').val('');\r\n" + 
                "                    reloadMessage();\r\n" + 
                "                }\r\n" + 
                "            );\r\n" + 
                "        });\r\n" + 
                "\r\n" + 
                "        function reloadMessage(){\r\n" + 
                "            $.get(\"" + messageUri + "\", function(data){\r\n" + 
                "                var messageList = JSON.parse(data);\r\n" + 
                "                var htmlStr = '';\r\n" + 
                "                for(var idx=0; idx<messageList.length; idx++){\r\n" + 
                "                    htmlStr += \"<textarea id=\\\"textAt\" + idx + \"\\\" type=\\\"text\\\" class=\\\"messageBox\\\"></textarea>\";\r\n" + 
                "                }\r\n" + 
                "                $('#logs').html(htmlStr);\r\n" + 
                "                for(var idx=0; idx<messageList.length; idx++){\r\n" + 
                "                    $('#textAt' + idx).val(messageList[idx]);\r\n" + 
                "                }\r\n" + 
                "            });\r\n" +
                "        }\r\n" + 
                "\r\n" + 
                "        $(function(){\r\n" + 
                "            reloadMessage();\r\n" +
                "        });\r\n" + 
                "    </script>\r\n" + 
                "</html>";
        
        return htmlStr;
    }
}
