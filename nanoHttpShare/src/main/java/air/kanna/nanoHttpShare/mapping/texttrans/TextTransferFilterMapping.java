package air.kanna.nanoHttpShare.mapping.texttrans;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
    private static final String MESSAGE_COUNT = "count";
    private static final String JQUERY_URI = "@jquery";
    
    private static final String MESSAGE_PARAM = "messageText";
    
    private static final MappingFunction DEFAULT_FUNCTION = new MappingFunction("文字传输", DEFAULT_URI);
    
    private final Logger logger = LoggerProvider.getLogger(TextTransferFilterMapping.class);
    
    private List<TransferMessage> textList;
    private MappingFunction function = DEFAULT_FUNCTION;
    private String indexUri;
    private String scriptUri;
    private String messageUri;
    private String messageCount;
    private String submitUri;
    
    private String htmlStr = null;
    private String sendBtn = "发送";
    private String flushBtn = "刷新";
    
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
            if(uri.equals(messageCount)) {
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
        String ip = FilterMappingUtil.getRequestIP(session);
        
        if(uri.equals(scriptUri)) {
            return FilterMappingUtil.getResourceResponse("air/kanna/nanoHttpShare/mapping/texttrans/data/jquery-3.5.1.min.js");
        }
        
        if(uri.equals(messageUri)) {
            return ShareHttpService.newFixedLengthResponse(Status.OK, ShareHttpService.FIXED_MIME_HTML, getMessageList(ip));
        }
        if(uri.equals(messageCount)) {
            return ShareHttpService.newFixedLengthResponse(Status.OK, ShareHttpService.FIXED_MIME_HTML, getMessageCount());
        }
        if(uri.equals(indexUri)) {
            return ShareHttpService.newFixedLengthResponse(Status.OK, ShareHttpService.FIXED_MIME_HTML, getIndexString());
        }
        if(uri.equals(submitUri)) {
            return addMessage(session, ip);
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
        messageCount = indexUri + '/' + MESSAGE_COUNT;
    }
    
    private Response addMessage(IHTTPSession session, String ip) {
        Map<String, String> params = new HashMap<String, String>();
        try {
            session.parseBody(params);
            String message = session.getParms().get(MESSAGE_PARAM);
            
            if(!StringTool.isAllSpacesString(message)) {
                TransferMessage msg = new TransferMessage();
                
                msg.setSendIP(ip);
                msg.setSendDate(new Date());
                msg.setMessage(message);
                msg.setFixedMessage(fixMessage(message));
                textList.add(msg);
            }
            
            return ShareHttpService.newFixedLengthResponse(Status.OK, ShareHttpService.FIXED_MIME_HTML, "");
        } catch (IOException | ResponseException e) {
            logger.error("Cannot parse message", e);
            return ShareHttpService.newFixedLengthResponse(Status.INTERNAL_ERROR, ShareHttpService.FIXED_MIME_HTML, e.getMessage());
        }
    }
    
    private String fixMessage(String orgMessage) {
        if(StringTool.isAllSpacesString(orgMessage)) {
            return "";
        }
        String fixed = orgMessage.replaceAll("\\\\", "\\\\\\\\");
        fixed = orgMessage.replaceAll("\\\"", "\\\\\"");
        fixed = orgMessage.replaceAll("\n", "\\\\n");
        
        return fixed;
    }
    
    private String getMessageCount() {
        return new StringBuilder()
                .append('{').append("\"count\":").append(textList.size()).append('}')
                .toString();
    }
    
    private String getMessageList(String ip) {
        StringBuilder sb = new StringBuilder();
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        
        sb.append('[');
        for(TransferMessage msg : textList) {
            String dateStr = format.format(msg.getSendDate());

            sb.append('{').append("\"date\":\"").append(dateStr)
                .append("\",\"isSender\":").append(StringTool.equals(ip, msg.getSendIP()))
                .append(",\"text\":\"").append(msg.getFixedMessage()).append("\"},");
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
                "            html,body{\r\n" + 
                "                height: 100%;\r\n" + 
                "                margin: 0px;\r\n" + 
                "                padding: 0px;\r\n" + 
                "                overflow: hidden;\r\n" + 
                "            }\r\n" + 
                "            body{\r\n" + 
                "                display: block;\r\n" + 
                "            }\r\n" + 
                "            div{\r\n" + 
                "                display: block;\r\n" + 
                "            }\r\n" + 
                "            footer {\r\n" + 
                "                position: fixed;\r\n" + 
                "                width: 100%;\r\n" + 
                "                height: 5%;\r\n" + 
                "                min-height: 80px;\r\n" + 
                "                border-top: solid 1px #ddd;\r\n" + 
                "                left: 0px;\r\n" + 
                "                bottom: 0px;\r\n" + 
                "                overflow: hidden;\r\n" + 
                "                background-color: #F5F5F5;\r\n" + 
                "            }\r\n" + 
                "            textarea{\r\n" + 
                "                box-sizing: border-box;\r\n" + 
                "                font: inherit;\r\n" + 
                "                resize: none;\r\n" + 
                "                margin: 0;\r\n" + 
                "                width: 100%;\r\n" + 
                "                height: 100%;\r\n" + 
                "                border: none;\r\n" + 
                "                border-bottom: 1.5px solid #ddd;\r\n" + 
                "                padding: 10px;\r\n" + 
                "            }\r\n" + 
                "            ul{\r\n" + 
                "                padding: 10px;\r\n" + 
                "                margin: 0;\r\n" + 
                "            }\r\n" + 
                "            ul li{\r\n" + 
                "                list-style-type: none;\r\n" + 
                "                padding-right: 20%;\r\n" + 
                "                margin-bottom: 10px;\r\n" + 
                "            }\r\n" + 
                "            .message-box{\r\n" + 
                "                background: #eee;\r\n" + 
                "                height: 100%;\r\n" + 
                "                padding: 5px 5px 80px;\r\n" + 
                "                overflow: auto;\r\n" + 
                "                box-sizing: border-box;\r\n" + 
                "            }\r\n" + 
                "            .footer-text {\r\n" + 
                "                height: 100%;\r\n" + 
                "                padding-right: 15%;\r\n" + 
                "                padding-left: 0;\r\n" + 
                "            }\r\n" + 
                "            .footer-rbutton {\r\n" + 
                "                position: absolute;\r\n" + 
                "                margin-left: 3px;\r\n" + 
                "                width: 15%;\r\n" + 
                "                height: 100%;\r\n" + 
                "                right: 0px;\r\n" + 
                "                bottom: 0px;\r\n" + 
                "                text-align: center;\r\n" + 
                "                vertical-align: middle;\r\n" + 
                "            }\r\n" + 
                "            .footer-lbutton {\r\n" + 
                "                position: absolute;\r\n" + 
                "                margin-right: 3px;\r\n" + 
                "                width: 10%;\r\n" + 
                "                height: 100%;\r\n" + 
                "                left: 0px;\r\n" + 
                "                bottom: 0px;\r\n" + 
                "                text-align: center;\r\n" + 
                "                vertical-align: middle;\r\n" + 
                "            }\r\n" + 
                "            .send_button{\r\n" + 
                "                width: 100%;\r\n" + 
                "                height: 100%;\r\n" + 
                "                margin: 0;\r\n" + 
                "                padding: 0;\r\n" + 
                "            }\r\n" + 
                "            .chart_self{\r\n" + 
                "                text-align: right;\r\n" + 
                "                padding-left: 20%;\r\n" + 
                "                padding-right: 0;\r\n" + 
                "            }\r\n" + 
                "            .chart_text, .chart_text_self{\r\n" + 
                "                word-break: break-all;\r\n" + 
                "                padding: 10px 15px;\r\n" + 
                "                position: relative;\r\n" + 
                "                background-color: white;\r\n" + 
                "                display: inline-block;\r\n" + 
                "                font-size: 3vw;\r\n" + 
                "            }\r\n" + 
                "            .chart_text_self{\r\n" + 
                "                background-color: #5FD05E;\r\n" + 
                "            }\r\n" + 
                "        </style>\r\n" + 
                "        <script src=\"" + scriptUri + "\" type=\"text/javascript\"></script>\r\n" + 
                "    </head>\r\n" + 
                "    <body>" +
                "        <div class=\"message-box\">\r\n" + 
                "            <ul id=\"msg_list\">\r\n" + 
                "            </ul>\r\n" + 
                "        </div>\r\n" + 
                "        <!-- 底部 -->\r\n" + 
                "        <footer>\r\n" + 
                "            <div class=\"footer-text\">\r\n" + 
                "                <textarea id=\"messageText\" type=\"text\" class=\"input-text\"></textarea>\r\n" + 
                "            </div>\r\n" + 
                "            <div class=\"footer-rbutton\">\r\n" + 
                "                <button id=\"sendBtn\" class=\"send_button\">" + sendBtn + "</button>\r\n" + 
                "            </div>\r\n" + 
                "        </footer>\r\n" + 
                "    </body>\r\n" + 
                "    <script type=\"text/javascript\">\r\n" + 
                "        var msgCount = 0;\r\n" + 
                "        $(\"#sendBtn\").click(function(){\r\n" + 
                "            var message = $('#messageText').val();\r\n" + 
                "            if(message == undefined || message == null || message == ''){\r\n" + 
                "                return;\r\n" + 
                "            }\r\n" + 
                "            $.post(\r\n" + 
                "                \"" + submitUri + "\", \r\n" + 
                "                {" + MESSAGE_PARAM + ": message},\r\n" + 
                "                function(data, textStatus){\r\n" + 
                "                    $('#messageText').val('');\r\n" + 
                "                    reloadMessage();\r\n" + 
                "                }\r\n" + 
                "            );\r\n" + 
                "        });\r\n" + 
                "\r\n" + 
                "        $(\"#flushBtn\").click(function(){\r\n" + 
                "            reloadMessage();\r\n" + 
                "        });\r\n" + 
                "\r\n" + 
                "        function reloadMessage(){\r\n" + 
                "            $.get(\"" + messageUri + "\", function(data){" + 
                "                var messageList = JSON.parse(data);\r\n" + 
                "                var htmlStr = '';\r\n" + 
                "\r\n" + 
                "                for(var idx=0; idx<messageList.length; idx++){\r\n" + 
                "                    htmlStr += \"<li\";\r\n" + 
                "                    if(messageList[idx].isSender){\r\n" + 
                "                        htmlStr += \" class=\\\"chart_self\\\"\";\r\n" + 
                "                    }\r\n" + 
                "                    htmlStr += \"><div id=\\\"textAt\" + idx + \"\\\" class=\\\"\";\r\n" + 
                "                    if(messageList[idx].isSender){\r\n" + 
                "                        htmlStr += \"chart_text_self\";\r\n" + 
                "                    }else{\r\n" + 
                "                        htmlStr += \"chart_text\";\r\n" + 
                "                    }\r\n" + 
                "                    htmlStr += \"\\\"></div></li>\";\r\n" + 
                "                }\r\n" + 
                "                $('#msg_list').html(htmlStr);\r\n" + 
                "\r\n" + 
                "                for(var idx=0; idx<messageList.length; idx++){\r\n" + 
                "                    $('#textAt' + idx).text(messageList[idx].text);\r\n" + 
                "                }\r\n" +
                "            });\r\n" + 
                "        }\r\n" + 
                "\r\n" + 
                "        $(function(){\r\n" + 
                "            reloadMessage();\r\n" +
                "        });\r\n" + 
                "\r\n" + 
                "        function reflush(){\r\n" + 
                "            $.get(\"" + messageCount + "\", function(data){\r\n" + 
                "                var count = JSON.parse(data);\r\n" + 
                "                var number = -1;\r\n" + 
                "                if(count == undefined || count == null || count.count == undefined || count.count == null){\r\n" + 
                "                    number = -1;\r\n" + 
                "                }else{\r\n" + 
                "                    number = count.count;\r\n" + 
                "                }\r\n" + 
                "\r\n" + 
                "                if(number < 0 || number != msgCount){\r\n" + 
                "                    msgCount = count.count;\r\n" + 
                "                    reloadMessage();\r\n" + 
                "                }\r\n" + 
                "            });\r\n" + 
                "        }\r\n" + 
                "\r\n" + 
                "        setInterval('reflush()', 2000);\r\n" + 
                "    </script>\r\n" + 
                "</html>";
        
        return htmlStr;
    }

    public String getSendBtn() {
        return sendBtn;
    }

    public String getFlushBtn() {
        return flushBtn;
    }

    public void setSendBtn(String sendBtn) {
        this.sendBtn = sendBtn;
        htmlStr = null;
    }

    public void setFlushBtn(String flushBtn) {
        this.flushBtn = flushBtn;
        htmlStr = null;
    }
}
