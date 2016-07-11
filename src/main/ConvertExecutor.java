package main;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.intellij.codeInsight.EditorInfo;
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
import main.view.EditActionListener;
import main.view.OnClick;
import main.view.OnRefresh;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.io.InputStream;
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

    public void execute(Project project, VirtualFile file) {
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

        Tree viewNameTree = ConvertConfig.getInstance().useSmartType ? prepareViewNames(project) : null;
        String javaCode = generateJavaCode(infos, viewNameTree);

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
     * @param node Node
     * @return List AndroidViewInfo
     */
    private List<AndroidViewInfo> traverseViewInfos(Node node) {
        List<AndroidViewInfo> infos = Lists.newArrayList();

        if (node.getNodeType() == Node.ELEMENT_NODE) {
            Node idNode = node.getAttributes().getNamedItem("android:id");

            //第一个过滤条件 ID不为空
            if (idNode != null) {
                String value = idNode.getNodeValue();
                String[] values = value.split("/");

                if (values.length != 2) {
                    throw new IllegalStateException("android:id value is invalid");
                }

                String[] elements = node.getNodeName().split("\\.");
                // 第二个过滤条件 临时ID 特殊标签
                if(!elements[elements.length - 1].equals("include") && !elements[elements.length - 1].equals("merge") && !values[1].startsWith("tem")) {


                    AndroidViewInfo viewInfo = new AndroidViewInfo(elements[elements.length - 1], values[1]);

                    //onClick
                    if (node.getAttributes().getNamedItem("android:clickable") != null && node.getAttributes().getNamedItem("android:clickable").getNodeValue().equals("true")) {
                        viewInfo.addListener(OnClick.getInstance().addId(viewInfo.id));
                    }
                    //EditText imeOptions : Search|Send
                    if (node.getAttributes().getNamedItem("android:imeOptions") != null) {
                        String action = node.getAttributes().getNamedItem("android:imeOptions").getNodeValue();
                        if (AndroidViewInfo.ON_SEARCH_LISTENER.equals(action) || AndroidViewInfo.ON_SEND_LISTENER.equals(action))
                            viewInfo.addListener(new EditActionListener(action));

                    }
                    //SwipeRefreshLayout onRefresh
                    if (StringUtils.equals(viewInfo.type, AndroidViewInfo.SWIPE_REFRESH_LAYOUT)) {
                        viewInfo.addListener(new OnRefresh());
                    }

                    infos.add(viewInfo);
                }
            }
        }

        NodeList children = node.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            infos.addAll(traverseViewInfos(children.item(index)));
        }

        return infos;
    }

    private String generateJavaCode(List<AndroidViewInfo> infos, Tree viewNameTree) {
        StringBuilder fieldJavaCode = new StringBuilder();
        StringBuilder methodJavaCode = new StringBuilder();
        StringBuilder listenerJavaCode = new StringBuilder();

        String NL = "\n";


        final String visibility;
        switch (ConvertConfig.getInstance().visibility) {
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
            fieldJavaCode.append(info.toFieldString(visibility));
            methodJavaCode.append(info.toMethodString(viewNameTree));
            listenerJavaCode.append(info.toListenerString());
        }

        methodJavaCode.append(listenerJavaCode);

        methodJavaCode.append("}");
        methodJavaCode.append(NL);

        if (ConvertConfig.getInstance().format.requireAssignMethod()) {
            return fieldJavaCode.toString() + NL + methodJavaCode.toString() + NL + OnClick.getInstance().toListenerString(null);
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
