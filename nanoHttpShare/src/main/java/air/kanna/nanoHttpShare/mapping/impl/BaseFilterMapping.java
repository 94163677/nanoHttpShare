package air.kanna.nanoHttpShare.mapping.impl;

import java.util.regex.Pattern;
import java.util.regex.Matcher;

import air.kanna.nanoHttpShare.mapping.FilterMapping;
import fi.iki.elonen.NanoHTTPD.IHTTPSession;

public abstract class BaseFilterMapping implements FilterMapping{
    protected Pattern[] acceptPatterns;
    
    @Override
    public boolean isAccept(IHTTPSession session) {
        if(session == null || session.getUri() == null) {
            return false;
        }
        String uri = session.getUri();
        for(Pattern accept : acceptPatterns) {
            Matcher matcher = accept.matcher(uri);
            if(matcher.find()) {
                return true;
            }
        }
        return false;
    }
}
