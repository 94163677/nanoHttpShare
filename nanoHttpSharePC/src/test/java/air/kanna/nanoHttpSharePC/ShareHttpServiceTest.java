package air.kanna.nanoHttpSharePC;

import air.kanna.nanoHttpShare.ShareHttpService;
import air.kanna.nanoHttpShare.logger.LoggerFactory;
import air.kanna.nanoHttpShare.logger.LoggerProvider;
import air.kanna.nanoHttpShare.mapping.fileshare.FileShareFilterMapping;
import air.kanna.nanoHttpShare.mapping.texttrans.TextTransferFilterMapping;
import air.kanna.nanoHttpSharePC.logger.impl.Log4jLoggerFactory;
import fi.iki.elonen.NanoHTTPD;

public class ShareHttpServiceTest{

    public static void main(String[] args) {
        try {
            LoggerFactory factory = new Log4jLoggerFactory();
            LoggerProvider.resetLoggerFactory(factory);
            
            ShareHttpService service = new ShareHttpService(8090);
            FileShareFilterMapping fileShare = new FileShareFilterMapping(".", null);
            TextTransferFilterMapping textTrans = new TextTransferFilterMapping();
            
            service.addFilterMapping(fileShare);
            service.addFilterMapping(textTrans);
            service.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        }catch(Exception e) {
            e.printStackTrace();
        }
    }
}
