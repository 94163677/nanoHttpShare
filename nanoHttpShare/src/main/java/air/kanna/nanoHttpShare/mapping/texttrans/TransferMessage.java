package air.kanna.nanoHttpShare.mapping.texttrans;

import java.util.Date;

public class TransferMessage {
    private String sendIP;
    private Date sendDate;
    
    private String message;

    public String getSendIP() {
        return sendIP;
    }

    public Date getSendDate() {
        return sendDate;
    }

    public String getMessage() {
        return message;
    }

    public void setSendIP(String sendIP) {
        this.sendIP = sendIP;
    }

    public void setSendDate(Date sendDate) {
        this.sendDate = sendDate;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
