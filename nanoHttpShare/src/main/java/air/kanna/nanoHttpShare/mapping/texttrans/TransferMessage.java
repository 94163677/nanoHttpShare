package air.kanna.nanoHttpShare.mapping.texttrans;

import java.util.Date;

public class TransferMessage {
    private String sendIP;
    private Date sendDate;
    
    private String message;
    private String fixedMessage;

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

    public String getFixedMessage() {
        return fixedMessage;
    }

    public void setFixedMessage(String fixedMessage) {
        this.fixedMessage = fixedMessage;
    }
}
