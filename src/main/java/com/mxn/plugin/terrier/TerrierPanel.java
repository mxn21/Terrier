package com.mxn.plugin.terrier;

import com.android.ddmlib.IDevice;
import com.intellij.icons.AllIcons;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.KtFile;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import static com.mxn.plugin.terrier.ActivityStackCommand.RESUME_TAG;

public class TerrierPanel extends SimpleToolWindowPanel implements DeviceService.DevicesListener, DeviceCellEditor.WatchListener {


    private final BackAction backAction = new BackAction();
    private final SendAction sendAction = new SendAction();
    private final RefreshAction refreshAction = new RefreshAction();
    private final EditorTextField editorText = new EditorTextField();

    private final DevicesTableModel devicesTableModel = new DevicesTableModel();
    private final JBTable jTable = new JBTable(devicesTableModel);
    private JScrollPane devicePanel;
    private JPanel noDevicesPanel;
    private JComponent myContent;
    private JPanel noAdbPanel;

    private boolean shouldShowDevice = true;
    private IDevice[] devices;
    private IDevice currentDevice;
    private final Project project;
    private final DefaultMutableTreeNode allTree = new DefaultMutableTreeNode();


    private String objectName ;

    TerrierPanel(@NotNull Project project) {
        super(true, true);
        this.project = project ;
        setToolbar(createToolbarPanel());
        scanDevices(project);
        createDevicePanel();
        createNoDevicePanel();
    }

    private void scanDevices(Project project) {
        DeviceService deviceService = new DeviceService(project);
        deviceService.setDevicesListener(this);
        deviceService.startService();
    }

    private void createDevicePanel() {
        DeviceCellEditor deviceCellEditor = new DeviceCellEditor(this);
        jTable.getColumnModel().getColumn(0).setCellRenderer(deviceCellEditor);
        jTable.getColumnModel().getColumn(0).setCellEditor(deviceCellEditor);
        devicePanel = ScrollPaneFactory.createScrollPane(jTable);
    }

    private void createNoDevicePanel() {
        noDevicesPanel = new JPanel(new BorderLayout());
        noDevicesPanel.add(new JLabel("not found devices", JLabel.CENTER), BorderLayout.CENTER);
    }

    private void refreshTree() {
        List<DefaultMutableTreeNode> activityStack = ActivityStackCommand.getActivityDumps2(currentDevice);
        allTree.setUserObject(currentDevice.isEmulator() ? currentDevice.getAvdName() : currentDevice.getProperty(IDevice.PROP_DEVICE_MODEL));
        allTree.removeAllChildren();
        for (DefaultMutableTreeNode node : activityStack) {
            allTree.add(node);
        }
        showAll();
    }

    private void showAll() {
        Tree tree = new Tree(allTree);
        final TreePopup treePopup = new TreePopup();
        tree.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                int x = e.getX();
                int y = e.getY();
                if(e.getButton()==MouseEvent.BUTTON3){
                    TreePath pathForLocation = tree.getPathForLocation(x, y);//获取右键点击所在树节点路径
                    // 最后一个层级再弹窗
                    if (pathForLocation == null) {
                        return;
                    }
                    if (pathForLocation.getPathCount() > 3 ||
                            pathForLocation.getParentPath().toString().contains(RESUME_TAG)) {
                        tree.setSelectionPath(pathForLocation);
                        treePopup.show(tree, x, y);
                    }
                }
            }
        });

        tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode sel_node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
            if(null!=sel_node){
                Object userObject = sel_node.getUserObject();
                if(null!=userObject){
                    if(userObject instanceof String){
                        objectName = userObject.toString();
                    }
                }
            }
        });
        setContent(ScrollPaneFactory.createScrollPane(tree));
    }

    @Override
    public void watch() {
        shouldShowDevice = false;
        currentDevice = devices[jTable.getEditingRow()];
        if (currentDevice.isOnline()) {
            refreshTree();
        }
    }

    @Override
    public void setContent(@NotNull JComponent c) {
        super.setContent(c);
        myContent = c;

    }

    private JComponent getMyContent() {
        return myContent;
    }

    @Override
    public void findDevices(IDevice[] devices) {
        this.devices = devices;
        devicesTableModel.updateDevice(devices);
        if (devices.length > 0) {
            if (shouldShowDevice) {
                if (devicePanel != getMyContent()) {
                    setContent(devicePanel);
                }
            } else {
                boolean haveDevice = false;
                for (IDevice device : devices) {
                    if (device.getSerialNumber().equals(currentDevice.getSerialNumber())) {
                        haveDevice = true;
                        break;
                    }
                }
                if (!haveDevice) {
                    shouldShowDevice = true;
                    setContent(devicePanel);
                }
            }
        } else {
            if (noDevicesPanel != getMyContent()) {
                setContent(noDevicesPanel);
            }
        }
    }

    @Override
    public void adbNotFind() {
        if (noAdbPanel == null) {
            noAdbPanel = new JPanel(new BorderLayout());
            noAdbPanel.add(new JLabel("'adb' command not found. Review your Android SDK installation.", JLabel.CENTER), BorderLayout.CENTER);
        }
        if (noAdbPanel != getMyContent()) {
            setContent(noAdbPanel);
        }
    }

    private JPanel createToolbarPanel() {
        DefaultActionGroup group = new DefaultActionGroup();
        group.add(sendAction);
        Separator separator = new Separator() ;
        group.add(separator);
        group.add(backAction);
        group.add(refreshAction);
        ActionToolbar actionToolBar = ActionManager.getInstance().createActionToolbar("Toolbar", group, true);
        JToolBar toolBar = new JToolBar() ;
        editorText.setToolTipText("make editText get focus and then enter the content");
        toolBar.add(editorText) ;
        toolBar.add(actionToolBar.getComponent()) ;
        return JBUI.Panels.simplePanel(toolBar);
    }


    private class SendAction extends AnAction {
        SendAction() {
            super("send", "send to EditText", AllIcons.Duplicates.SendToTheRight);
        }
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            ActivityStackCommand.pushToMobile(currentDevice,editorText.getText());
            System.out.println(editorText.getText());
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(!shouldShowDevice);
        }
    }
    private class BackAction extends AnAction {
        BackAction() {
            super("back", "back", AllIcons.Actions.Back);
        }
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            setContent(devicePanel);
            shouldShowDevice = true;
        }
        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(!shouldShowDevice);
        }
    }

    private class RefreshAction extends AnAction {
        RefreshAction() {
            super("refresh activity", "refresh activity stack", AllIcons.Actions.Refresh);
        }
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
            refreshTree();
        }

        @Override
        public void update(@NotNull AnActionEvent e) {
            e.getPresentation().setEnabled(!shouldShowDevice);
        }
    }


    class TreePopup extends JPopupMenu {
        public TreePopup() {
            JMenuItem copyItem = new JMenuItem("copy");
            JMenuItem jumpItem = new JMenuItem("jump");
            copyItem.addActionListener(ae -> {
                StringSelection selection = new StringSelection(objectName);
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                clipboard.setContents(selection,selection);
                NotificationGroup notificationGroup = new NotificationGroup("terrier", NotificationDisplayType.BALLOON, false);
                Notification notification = notificationGroup.createNotification("copy suc: " + objectName, MessageType.INFO);
                Notifications.Bus.notify(notification);
            });
            jumpItem.addActionListener(ae -> {
                System.out.println("jumpItem actionPerformed");
                String className = objectName.substring(objectName.lastIndexOf(".")+1);
                System.out.println("className :" + className);
                String javaName = className + ".java" ;
                String kotlinName = className + ".kt" ;
                PsiFile[] javaFiles = PsiShortNamesCache.getInstance(project).getFilesByName(javaName) ;
                PsiFile[] kotlinFiles = PsiShortNamesCache.getInstance(project).getFilesByName(kotlinName) ;
                boolean hasMatch = false ;
                for (PsiFile file : javaFiles) {
                    if (file instanceof PsiJavaFile) {
                        PsiJavaFile psiJavaFile = (PsiJavaFile) file;
                        String packageName = psiJavaFile.getPackageName() ;
                        VirtualFile vf = file.getViewProvider().getVirtualFile();
                        String path = vf.getPath() ;
                        String tempPath = packageName.replaceAll("\\.","/") + "/" + javaName;
                        System.out.println("java tempPath1 :" + tempPath);
                        if (path.contains(tempPath)) {
                            System.out.println("java contains :" );
                            openFile(vf) ;
                            hasMatch = true ;
                        }
                    }
                }
                for (PsiFile file : kotlinFiles) {
                    if (file instanceof KtFile) {
                        KtFile psiKtFile = (KtFile) file;
                        String packageName = psiKtFile.getPackageFqName().asString() ;
                        VirtualFile vf = file.getViewProvider().getVirtualFile();
                        String path = vf.getPath() ;
                        String tempPath = packageName.replaceAll("\\.","/") + "/" + kotlinName;
                        System.out.println("kt tempPath2 :" + tempPath);
                        if (path.contains(tempPath)) {
                            System.out.println("kt contains :" );
                            openFile(vf) ;
                            hasMatch = true ;
                        }
                    }
                }
                if (!hasMatch) {
                    NotificationGroup notificationGroup = new NotificationGroup("terrier", NotificationDisplayType.BALLOON, false);
                    Notification notification = notificationGroup.createNotification("can't find file:" + className, MessageType.INFO);
                    Notifications.Bus.notify(notification);
                }
            });
            add(copyItem);
            add(new JSeparator());
            add(jumpItem);
        }
    }

    private void openFile(VirtualFile virtualFile){
        virtualFile.refresh(false, true) ;
        new OpenFileDescriptor(project, virtualFile).navigate(true) ;
    }
}
