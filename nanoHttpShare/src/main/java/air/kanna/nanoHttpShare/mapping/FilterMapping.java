package air.kanna.nanoHttpShare.mapping;

import fi.iki.elonen.NanoHTTPD.IHTTPSession;
import fi.iki.elonen.NanoHTTPD.Response;

public interface FilterMapping {
    /**
     * 当前处理器是否注册到首页
     * @return
     */
    MappingFunction getFunction();
    
    
    /**
     * 是否可以处理当前请求
     * @param session
     * @return
     */
    boolean isAccept(IHTTPSession session);

    
    /**
     * 处理当前请求并返回结果
     * @param session
     * @return
     */
    Response response(IHTTPSession session);
}
