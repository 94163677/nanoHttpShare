package air.kanna.nanoHttpShare.mapping;

import air.kanna.nanoHttpShare.util.StringTool;

public class MappingFunction {
    
    private String functionName;
    private String functionUri;
    
    public MappingFunction(String name, String uri) {
        if(StringTool.isAllSpacesString(name) || StringTool.isAllSpacesString(uri)) {
            throw new NullPointerException("functionName or functionUri is null");
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
    
    
}
