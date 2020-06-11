package air.kanna.nanoHttpShare.mapping;

import air.kanna.nanoHttpShare.util.StringTool;

public class MappingFunction {
    
    private String functionName;
    private String functionUri;
    
    public MappingFunction(String name, String uri) {
        if(StringTool.isAllSpacesString(name) || StringTool.isAllSpacesString(uri)) {
            throw new NullPointerException("functionName or functionUri is null");
        }
        if(!checkUri(uri)) {
            throw new java.lang.IllegalArgumentException("URI check error");
        }
        
        functionName = name;
        functionUri = uri;
    }
    
    public String getFunctionName() {
        return functionName;
    }
    public String getFunctionUri() {
        return functionUri;
    }
    public void setFunctionName(String functionName) {
        this.functionName = functionName;
    }
    public void setFunctionUri(String functionUri) {
        this.functionUri = functionUri;
    }
    
    /**
     * 检查输入的功能URI，暂时只支持字母和数字的组合
     * @param uri
     * @return
     */
    private boolean checkUri(String uri) {
        for(int i=0; i<uri.length(); i++) {
            char ch = uri.charAt(i);
            if((ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'Z') || (ch >= 'a' && ch <= 'z')) {
                continue;
            }
            return false;
        }
        return true;
    }
    
}
