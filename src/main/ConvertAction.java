package main;

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKeys;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 主要用来找Layout文件(建议从Layout目录直接右键使用速度快)
 * Created by Qiu on 2016/07/08.
 */
public class ConvertAction extends AnAction {
    public void actionPerformed(AnActionEvent e) {
        VirtualFile file = DataKeys.VIRTUAL_FILE.getData(e.getDataContext());

        if (file == null) {
            showError("Cannot find target file");
            return;
        }
        //获取文件拓展名称
        String extension = file.getExtension();

        if (extension != null && extension.equalsIgnoreCase("xml")) {//XML
            if (!file.getParent().getName().startsWith("layout")) {
                showError(String.format("Selected file directory (%s) is seems not Android layout directory\n(没有可用的Layout文件)", file.getParent().getName()));
                return;
            }

            new ConvertConfigDialog(e.getProject(), file).show();
        } else if (extension != null && extension.equalsIgnoreCase("java")) {//JAVA
            String name = extractLayoutFileNameFromJavaFile(file);

            if (name == null) {
                showError(String.format("Cannot find layout file id from [%s]", file.getName()));
                return;
            }

            VirtualFile layoutFile = traverseLayoutFileByName(name, e.getProject().getBaseDir(), file, null);

            if (layoutFile == null) {
                showError(String.format("Cannot find layout file for name [%s]", name));
                return;
            }

            new ConvertConfigDialog(e.getProject(), layoutFile).show();
        } else { //other 先忽略
            showError(String.format("This file (%s) extension is not supported", file.getName()));
        }
    }

    /**
     * 根据Java文件内容检匹配出其中的R.layout.xxx关键字
     * @param file VirtualFile
     * @return String R.layout.xxx
     */
    private String extractLayoutFileNameFromJavaFile(VirtualFile file) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(file.getInputStream(), file.getCharset()));

            Pattern pattern = Pattern.compile("R\\.layout\\.([a-z0-9_]+)");
            for (String l; (l = reader.readLine()) != null; ) {
                Matcher matcher = pattern.matcher(l);
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }

            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try{
                if(reader != null)
                    reader.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }

    /**
     * 根据R.layout.xxx name找出File
     * @param name R.layout.xxx
     * @param baseDir Project Dir
     * @param file 预匹配的File
     * @param traverseFrom 递归源路径
     * @return File
     */
    private VirtualFile traverseLayoutFileByName(String name, VirtualFile baseDir, VirtualFile file, VirtualFile traverseFrom) {
        VirtualFile parent = file.getParent();

        // 先看预匹配的FileName 与 Name是否一致
        if (!file.isDirectory() && file.getName().equalsIgnoreCase(name + ".xml")) {
            if (parent != null && parent.getName().startsWith("layout")) {
                VirtualFile grandParent = parent.getParent();
                if (grandParent != null && grandParent.getName().equalsIgnoreCase("res")) {
                    return file;
                }
            }
        }
        //没有匹配
        String traverseFromUrl = traverseFrom != null ? traverseFrom.getUrl() : null;

        //遍历预匹配文件的子文件夹
        for (VirtualFile child : file.getChildren()) {
            if (!child.getUrl().equals(traverseFromUrl) && !child.getName().startsWith(".")) {
                VirtualFile layoutFile = traverseLayoutFileByName(name, baseDir, child, file);
                if (layoutFile != null) {
                    return layoutFile;
                }
            }
        }
        //如果预匹配文件不是根目录, 用上级目录递归
        if (!file.getUrl().equals(baseDir.getUrl())) {
            if (parent != null) {
                if (!parent.getUrl().equals(traverseFromUrl)) {
                    return traverseLayoutFileByName(name, baseDir, parent, file);
                }
            }
        }

        return null;
    }

    private void showError(String content) {
        Notifications.Bus.notify(new Notification("OffingHarbor", "OffingHarbor", content, NotificationType.ERROR));
    }
}
