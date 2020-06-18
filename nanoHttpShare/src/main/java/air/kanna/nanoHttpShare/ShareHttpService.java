package air.kanna.nanoHttpShare;

import java.util.ArrayList;
import java.util.List;

import air.kanna.nanoHttpShare.logger.Logger;
import air.kanna.nanoHttpShare.logger.LoggerProvider;
import air.kanna.nanoHttpShare.mapping.FilterMapping;
import air.kanna.nanoHttpShare.mapping.FilterMappingUtil;
import air.kanna.nanoHttpShare.mapping.MappingFunction;
import air.kanna.nanoHttpShare.mapping.impl.RootFilterMapping;
import fi.iki.elonen.NanoHTTPD;
import fi.iki.elonen.NanoHTTPD.Response.Status;

public class ShareHttpService extends NanoHTTPD{
    public static final String FIXED_MIME_HTML = "text/html;application/json;charset=UTF-8";
    
    private final Logger logger = LoggerProvider.getLogger(ShareHttpService.class);
    
    private RootFilterMapping rootMapping;
    private List<FilterMapping> mappingList = new ArrayList<>();
    
    public ShareHttpService(int port) {
        super(port);
        rootMapping = new RootFilterMapping();
    }
    
    public ShareHttpService(String hostname, int port){
        super(hostname, port);
        rootMapping = new RootFilterMapping();
    }
    
    @Override
    public Response serve(IHTTPSession session){
        try {
            logger.info(FilterMappingUtil.getRequestIP(session) + " - " + session.getUri());
            
            Response response = null;
            if(rootMapping.isAccept(session)) {
                response = rootMapping.response(session);
            }else {
                for(FilterMapping mapping : mappingList) {
                    if(!mapping.isAccept(session)) {
                        continue;
                    }
                    response = mapping.response(session);
                    break;
                }
            }
            
            if(response == null) {
                return newFixedLengthResponse(Status.NOT_FOUND, FIXED_MIME_HTML, "");
            }
            return response;
        }catch(Exception e) {
            logger.error("", e);
            return newFixedLengthResponse(Status.INTERNAL_ERROR, FIXED_MIME_HTML, e.getMessage());
        }
    }
    
    public synchronized boolean addFilterMapping(FilterMapping mapping) {
        if(mapping == null || mappingList.contains(mapping)) {
            return false;
        }
        if(!checkFilterMapping(mapping)) {
            return false;
        }
        boolean result = mappingList.add(mapping);
        mappingListChange();
        return result;
    }
    
    public synchronized boolean removeFilterMapping(FilterMapping mapping) {
        if(mapping == null || !mappingList.contains(mapping)) {
            return false;
        }
        boolean result = mappingList.remove(mapping);
        mappingListChange();
        return result;
    }
    
    public synchronized void clearFilterMapping() {
        rootMapping.getFunctions().clear();
        mappingList.clear();
    }
    
    public String getFunctionNotFound() {
        return rootMapping.getFunctionNotFound();
    }

    public void setFunctionNotFound(String functionNotFound) {
        rootMapping.setFunctionNotFound(functionNotFound);
    }
    
    private void mappingListChange() {
        rootMapping.getFunctions().clear();
        for(FilterMapping mapping : mappingList) {
            MappingFunction function = mapping.getFunction();
            if(function != null) {
                rootMapping.getFunctions().add(function);
            }
        }
    }
    
    private boolean checkFilterMapping(FilterMapping mapping) {
        MappingFunction func = mapping.getFunction();
        if(func == null) {
            return false;
        }
        
        for(FilterMapping filter : mappingList) {
            if(filter.getFunction() == null) {
                continue;
            }
            MappingFunction tempFunc = filter.getFunction();
            if(tempFunc.getFunctionName().equalsIgnoreCase(func.getFunctionName())) {
                logger.error("Same Function Name: " + func.getFunctionName());
                return false;
            }
            if(tempFunc.getFunctionUri().equalsIgnoreCase(func.getFunctionUri())) {
                logger.error("Same Function URI: " + func.getFunctionUri());
                return false;
            }
        }
        return true;
    }
}
