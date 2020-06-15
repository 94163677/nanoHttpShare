package air.kanna.nanoHttpSharePC;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.log4j.Logger;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import air.kanna.nanoHttpShare.ShareHttpService;
import air.kanna.nanoHttpShare.logger.LoggerFactory;
import air.kanna.nanoHttpShare.logger.LoggerProvider;
import air.kanna.nanoHttpShare.mapping.MappingFunction;
import air.kanna.nanoHttpShare.mapping.fileshare.FileShareFilterMapping;
import air.kanna.nanoHttpShare.mapping.texttrans.TextTransferFilterMapping;
import air.kanna.nanoHttpShare.util.StringTool;
import air.kanna.nanoHttpSharePC.config.NanoSharePCConfig;
import air.kanna.nanoHttpSharePC.config.NanoSharePCConfigService;
import air.kanna.nanoHttpSharePC.config.impl.SyncConfigServicePropertiesImpl;
import air.kanna.nanoHttpSharePC.logger.impl.Log4jLoggerFactory;
import air.kanna.nanoHttpSharePC.util.NetworkUtil;
import fi.iki.elonen.NanoHTTPD;

public class ShareStartUp {
    private static final Logger logger = Logger.getLogger(ShareStartUp.class);
    
    private static final String CONFIG_FILE = "config.cfg";
    
    private JFrame frmHttp;
    private JComboBox<String> basePathCb;
    private JButton baseSelectPathBtn;
    private JLabel imageLb;
    private JButton startStopBtn;
    private JTextField ipAddrTf;
    private JTextField portTf;
    
    private JFileChooser chooser;
    
    private String ipAddr;
    private int port;
    private NanoSharePCConfig config;
    private NanoSharePCConfigService configService;
    private ShareHttpService service;
    

    /**
     * Create the application.
     */
    public ShareStartUp() {
        initialize();
        initData();
        initControl();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {
        frmHttp = new JFrame();
        frmHttp.setTitle("HTTP分享");
        frmHttp.setBounds(100, 100, 450, 530);
        frmHttp.setResizable(false);
        frmHttp.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frmHttp.getContentPane().setLayout(new BorderLayout(0, 0));
        
        JPanel panel = new JPanel();
        frmHttp.getContentPane().add(panel, BorderLayout.NORTH);
        panel.setLayout(new GridLayout(2, 1, 0, 0));
        
        JPanel panel_2 = new JPanel();
        FlowLayout flowLayout = (FlowLayout) panel_2.getLayout();
        flowLayout.setAlignment(FlowLayout.LEFT);
        panel.add(panel_2);
        
        JLabel lblNewLabel = new JLabel("分享目录：");
        panel_2.add(lblNewLabel);
        
        basePathCb = new JComboBox<>();
        panel_2.add(basePathCb);
        basePathCb.setModel(new DefaultComboBoxModel<String>(new String[] {"5555555555555555555"}));
        basePathCb.setEditable(true);
        
        baseSelectPathBtn = new JButton("...");
        panel_2.add(baseSelectPathBtn);
        
        JPanel panel_3 = new JPanel();
        FlowLayout flowLayout_1 = (FlowLayout) panel_3.getLayout();
        flowLayout_1.setAlignment(FlowLayout.LEFT);
        panel.add(panel_3);
        
        JLabel lblIp = new JLabel("IP：");
        panel_3.add(lblIp);
        
        ipAddrTf = new JTextField();
        ipAddrTf.setEditable(false);
        panel_3.add(ipAddrTf);
        ipAddrTf.setColumns(10);
        
        JLabel label = new JLabel("  ");
        panel_3.add(label);
        
        JLabel lblNewLabel_1 = new JLabel("PORT：");
        panel_3.add(lblNewLabel_1);
        
        portTf = new JTextField();
        portTf.setEditable(false);
        panel_3.add(portTf);
        portTf.setColumns(10);
        
        JPanel panel_1 = new JPanel();
        frmHttp.getContentPane().add(panel_1, BorderLayout.SOUTH);
        
        startStopBtn = new JButton("开始共享");
        panel_1.add(startStopBtn);
        
        imageLb = new JLabel("");
        frmHttp.getContentPane().add(imageLb, BorderLayout.CENTER);
        
        chooser = new JFileChooser();
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setDialogTitle("请选择目录");
        chooser.setApproveButtonText("选择该目录");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setMultiSelectionEnabled(false);
    }
    
    private void initData() {
        File configFile = new File(CONFIG_FILE);
        logger.info("config file: " + configFile.getAbsolutePath());
        configService = new SyncConfigServicePropertiesImpl(configFile);
        
        config = configService.getConfig();
        if(config == null) {
            config = new NanoSharePCConfig();
        }
        
        setFromConfig();
        resetConnectParam();
        
        ipAddrTf.setText(ipAddr);
        portTf.setText("" + port);
        
        imageLb.setText("");
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        Map hints = new HashMap();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.Q);//纠错等级，从低到高为LMQH
        //hints.put(EncodeHintType.MARGIN, 2);//边距
        String url = "http://" + ipAddr + ':' + port;
        try {
            BitMatrix bitMatrix = multiFormatWriter.encode(url, BarcodeFormat.QR_CODE, 400, 400, hints);
            BufferedImage image = toBufferedImage(bitMatrix);
            
            Icon icon = new ImageIcon(image);
            imageLb.setIcon(icon);
        }catch(Exception e) {
            logger.error("Create qrcode error", e);
            imageLb.setText("创建二维码失败，请手动输入链接");
        }
        
        service = new ShareHttpService(port);
    }
    
    private BufferedImage toBufferedImage(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return image;
    }
    
    private void initControl() {
        frmHttp.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveToConfig();
                if(!configService.saveConfig(config)) {
                    logger.error("Save Config to file error");
                }
                stopService();
                System.exit(0);
            }
        });
        
        baseSelectPathBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                selectAndAddPath(basePathCb);
            }
        });
        
        startStopBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent event) {
                if(service.isAlive()) {
                    stopService();
                }else {
                    startService();
                }
            }
        });
        
        basePathCb.addItemListener(new ItemListener() {
            private String lastSelected = null;
            @Override
            public void itemStateChanged(ItemEvent event) {
                if(event.getStateChange() == ItemEvent.DESELECTED) {
                    lastSelected = (String)event.getItem();
                }else
                if(event.getStateChange() == ItemEvent.SELECTED) {
                    if(lastSelected != null && lastSelected.equalsIgnoreCase((String)event.getItem())) {
                        return;
                    }
                    resetShareFile();
                }
            }
        });
    }
    
    private void startService() {
        if(service.isAlive()) {
            return;
        }
        resetShareFile();
        try {
            service.start(NanoHTTPD.SOCKET_READ_TIMEOUT);
            startStopBtn.setText("停止共享");
        }catch(Exception e) {
            logger.error("", e);
            JOptionPane.showMessageDialog(frmHttp, "启动共享服务失败：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private void stopService() {
        if(!service.isAlive()) {
            return;
        }
        service.closeAllConnections();
        service.stop();
        startStopBtn.setText("开始共享");
    }
    
    private boolean resetShareFile() {
        String path = (String)basePathCb.getSelectedItem();
        if(StringTool.isAllSpacesString(path)) {
            JOptionPane.showMessageDialog(frmHttp, "请选中要共享的目录", "提示", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        try {
            File file = new File(path);
            MappingFunction function = new MappingFunction(file.getName(), UUID.randomUUID().toString().replaceAll("-", ""));
            FileShareFilterMapping mapping = new FileShareFilterMapping(file, function);
            service.clearFilterMapping();
            service.addFilterMapping(mapping);
            
            function = new MappingFunction("文字传输", UUID.randomUUID().toString().replaceAll("-", ""));
            TextTransferFilterMapping transfer = new TextTransferFilterMapping(function);
            service.addFilterMapping(transfer);
        }catch(Exception e) {
            logger.error("", e);
            JOptionPane.showMessageDialog(frmHttp, "更新共享的目录错误：" + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }
    
    private void setFromConfig() {
        DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>)basePathCb.getModel();
        
        model.removeAllElements();
        
        if(StringTool.isNullString(config.getBasePath())) {
            
        }else {
            String[] paths = config.getBasePath().split(";");
            if(paths == null) {
                paths = new String[] {config.getBasePath()};
            }
            for(int i=0; i<paths.length; i++) {
                if(StringTool.isNullString(paths[i])) {
                    continue;
                }
                model.addElement(paths[i]);
            }
            basePathCb.setSelectedIndex(0);
        }
        
    }
    
    private void saveToConfig() {
        config.setBasePath(getStringsFromComboBox(basePathCb));
    }
    
    private String getStringsFromComboBox(JComboBox<String> box) {
        if(box == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if(box.getSelectedIndex() >= 0) {
            sb.append(box.getSelectedItem()).append(';');
        }
        for(int i=0; i<box.getItemCount(); i++) {
            if(i == box.getSelectedIndex()) {
                continue;
            }
            sb.append(box.getItemAt(i)).append(';');
        }
        return sb.toString();
    }
    
    private void selectAndAddPath(JComboBox<String> box) {
        File old = null;
        if(box.getSelectedIndex() >= 0) {
            if(!StringTool.isNullString((String)box.getSelectedItem())) {
                old = new File((String)box.getSelectedItem());
            }
        }
        if(old == null) {
            old = new File(".");
        }
        
        chooser.setSelectedFile(old);
        if(JFileChooser.APPROVE_OPTION == chooser.showOpenDialog(frmHttp)){
            File selected = chooser.getSelectedFile();
            String path = selected.getAbsolutePath();
            int findIdx = -1;
            for(int i=0; i<box.getItemCount(); i++) {
                if(path.equals(box.getItemAt(i))) {
                    findIdx = i;
                    break;
                }
            }
            if(findIdx >= 0) {
                box.setSelectedIndex(findIdx);
            }else {
                box.addItem(path);
                box.setSelectedItem(path);
            }
//            reset();
        }
    }
    
    private void resetConnectParam() {
        try {
            ipAddr = NetworkUtil.getLocalIpv4Address().get(0).getHostAddress();
            port = getRandomPort();
        }catch(Exception e) {
            logger.error("", e);
            JOptionPane.showMessageDialog(frmHttp, "生成链接参数错误，具体请看日志", "错误", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    private int getRandomPort() {
        return (int)((Math.random() * 50000) + 10000);
    }
    
    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        LoggerFactory factory = new Log4jLoggerFactory();
        LoggerProvider.resetLoggerFactory(factory);
        
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    ShareStartUp window = new ShareStartUp();
                    window.frmHttp.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

}
