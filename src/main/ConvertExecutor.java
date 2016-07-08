package main;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import main.view.AndroidViewInfo;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

/**
 * Created by toyama.yosaku on 13/12/31.
 */
public class ConvertExecutor {


    public static class Tree {
        public String name;
        public List<Tree> children; // Set is enough

        private Tree(String name) {
            this.name = name;
            children = Lists.newArrayList();
        }
    }

    public void execute(Project project, VirtualFile file, ConvertConfig config) {
        List<AndroidViewInfo> infos;
        InputStream is = null;
        try {
            is = file.getInputStream();
            infos = extractViewInfos(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try{
                if(is != null)
                    is.close();
            }catch (Exception e){
                e.printStackTrace();
            }
        }

        Tree viewNameTree = config.useSmartType ? prepareViewNames(project) : null;
        String javaCode = generateJavaCode(infos, viewNameTree, config);

        CopyPasteManager.getInstance().setContents(new StringSelection(javaCode));

        // add layout file name to message
        Notifications.Bus.notify(new Notification("OffingHarbor", "OffingHarbor", "Code is copied to clipboard", NotificationType.INFORMATION), project);
    }

    private List<AndroidViewInfo> extractViewInfos(InputStream is) {
        try {
            return traverseViewInfos(DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is));
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        } catch (SAXException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * create ViewInfo
     * package.name.Button
     * android:id="@+id/view"
     *
     * ViewInfo(Button, view)
     * @param node
     * @return
     */
    private List<AndroidViewInfo> traverseViewInfos(Node node) {
        List<AndroidViewInfo> infos = Lists.newArrayList();

        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Node idNode = node.getAttributes().getNamedItem("android:id");


            if (idNode != null) {
                String value = idNode.getNodeValue();
                String[] values = value.split("/");

                if (values.length != 2) {
                    throw new IllegalStateException("android:id value is invalid");
                }

                String[] elements = node.getNodeName().split("\\.");
                AndroidViewInfo viewInfo = new AndroidViewInfo(elements[elements.length - 1], values[1]);

                //onClick
                if(node.getAttributes().getNamedItem("android:onClick") != null){
                    viewInfo.addListener(AndroidViewInfo.ON_CLICK_LISTENER);
                }
                //EditText imeOptions : Search|Send
                if(node.getAttributes().getNamedItem("android:imeOptions") != null){
                    viewInfo.addListener(node.getAttributes().getNamedItem("android:imeOptions").getNodeValue());
                }
                //SwipeRefreshLayout onRefresh
                if(StringUtils.equals(viewInfo.type, AndroidViewInfo.SWIPE_REFRESH_LAYOUT)) {
                    viewInfo.addListener(AndroidViewInfo.ON_REFRESH_LISTENER);
                }

                infos.add(viewInfo);
            }
        }

        NodeList children = node.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            infos.addAll(traverseViewInfos(children.item(index)));
        }

        return infos;
    }

    private String generateJavaCode(List<AndroidViewInfo> infos, Tree viewNameTree, ConvertConfig config) {
        StringBuilder fieldJavaCode = new StringBuilder();
        StringBuilder methodJavaCode = new StringBuilder();

        String NL = "\n";


        final String visibility;
        switch (config.visibility) {
            case PROTECTED:
                visibility = "protected ";
                break;
            case PACKAGE_PRIVATE:
                visibility = "";
                break;
            case PRIVATE:
                visibility = "private ";
                break;
            default:
                throw new IllegalStateException("assert");
        }

        methodJavaCode.append("private void initViewsAndListener() {");
        methodJavaCode.append(NL);

        for (AndroidViewInfo info : infos) {
            fieldJavaCode.append(info.toFieldString(config, visibility));
            methodJavaCode.append(info.toMethodString(config, viewNameTree));
        }

        methodJavaCode.append("}");
        methodJavaCode.append(NL);

        if (config.format.requireAssignMethod()) {
            return fieldJavaCode.toString() + NL + methodJavaCode.toString();
        } else {
            return fieldJavaCode.toString();
        }
    }

    private Tree prepareViewNames(Project project) {
        GlobalSearchScope scope = GlobalSearchScope.allScope(project);
        return traverseViewNames(JavaPsiFacade.getInstance(project).findClass("android.view.View", scope), scope);
    }

    private Tree traverseViewNames(PsiClass psiClass, SearchScope scope) {
        Tree tree = new Tree(psiClass.getName());

        for (PsiClass child : ClassInheritorsSearch.search(psiClass, psiClass.getUseScope().intersectWith(scope), false).findAll()) {
            tree.children.add(traverseViewNames(child, scope));
        }

        return tree;
    }
}
