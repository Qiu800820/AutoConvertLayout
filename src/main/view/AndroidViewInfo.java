package main.view;

import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import main.ConvertConfig;
import main.ConvertExecutor;
import org.apache.batik.css.engine.value.svg.BaselineShiftManager;
import org.apache.commons.lang.StringUtils;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by TUS on 2016/7/8.
 */
public class AndroidViewInfo {
    
    private static final String NL = "\n";

    public static final String SWIPE_REFRESH_LAYOUT = "SwipeRefreshLayout";

    public static final String ON_SEARCH_LISTENER = "actionSearch";
    public static final String ON_SEND_LISTENER = "actionSend";

    
    public String type;
    public String id;
    public String field;

    public List<BaseListener> listeners;

    public AndroidViewInfo(String type, String id) {
        this.type = type;
        this.id = id;
        field = getJavaSymbolName(ConvertConfig.getInstance());
    }

    public void addListener(BaseListener listener){
        if(listeners == null)
            listeners = new ArrayList<BaseListener>();
        listeners.add(listener);
    }

    public String toListenerString(){
        if(listeners == null || listeners.size() == 0)
            return "";

        StringBuilder listenerJavaCode = new StringBuilder();

        for(BaseListener listener : listeners){
            // OnClick
            if(listener instanceof OnClick) {
                listenerJavaCode.append(String.format("\t%s.setOnClickListener(listener)\n", field));
            }else{
                //Other
                listenerJavaCode.append(listener.toListenerString(field));
            }
        }


        return listenerJavaCode.toString();
    }

    public String toFieldString(String visibility){
        StringBuilder fieldJavaCode = new StringBuilder();


        if (ConvertConfig.getInstance().format == ConvertConfig.ConvertFormat.ANDROID_ANNOTATIONS) {
            if (ConvertConfig.getInstance().prefix.willModify()) {
                fieldJavaCode.append(String.format("@ViewById(R.id.%s)" + NL + "%s%s %s;" + NL, this.id, visibility, type, field));
            } else {
                fieldJavaCode.append(String.format("@ViewById" + NL + "%s%s %s;" + NL, visibility, type, field));
            }
        } else if (ConvertConfig.getInstance().format == ConvertConfig.ConvertFormat.BUTTER_KNIFE) {
            // Butter Knife always requires resource-id
            fieldJavaCode.append(String.format("@InjectView(R.id.%s)" + NL + "%s%s %s;" + NL, this.id, visibility, type, field));
        } else {
            fieldJavaCode.append(String.format("%s%s %s;" + NL, visibility, type, field));
        }
        return fieldJavaCode.toString();

    }


    public String toMethodString(ConvertExecutor.Tree viewNameTree){
        StringBuilder methodJavaCode = new StringBuilder();
        String type = ConvertConfig.getInstance().useSmartType ? findOptimalType(viewNameTree) : this.type;
        String symbol = getJavaSymbolName(ConvertConfig.getInstance());


        if (type.equals("View")) {
            methodJavaCode.append(String.format("\t%s = findViewById(R.id.%s);" + NL, symbol, this.id));
        } else {
            methodJavaCode.append(String.format("\t%s = (%s) findViewById(R.id.%s);" + NL, symbol, type, this.id));
        }
        return methodJavaCode.toString();

    }

    /**
     * 根据Prefix Config 生成Name
     * @param config Config
     * @return String Name
     */
    public String getJavaSymbolName(ConvertConfig config) {
        switch (config.prefix) {
            case NONE:
                return snakeCaseToCamelCase(id);
            case MEMBER:
                return snakeCaseToCamelCase("m_" + id);
            case UNDERSCORE:
                return "_" + snakeCaseToCamelCase(id);
            default:
                throw new IllegalStateException("assert");
        }
    }

    // 型名が一致する かつ this.type の親クラスであれば
    public String findOptimalType(ConvertExecutor.Tree viewNameTree) {
        List<String> split = Arrays.asList(id.split("_"));

        while (!split.isEmpty()) {
            String candidate = capitalizeJoin(split);

            if (traverseOptimalType(viewNameTree, candidate, type, false)) {
                return candidate;
            }

            split = split.subList(1, split.size());
        }

        return type;
    }

    /**
     * 首字母大写拼接
     * @param list List Strings
     * @return String
     */
    private String capitalizeJoin(List<String> list) {
        List<String> capitalized = Lists.newArrayList();

        for (String s : list) {
            capitalized.add(StringUtils.capitalize(s));
        }

        return StringUtils.join(capitalized, "");
    }

    private boolean traverseOptimalType(ConvertExecutor.Tree viewNamesTree, String candidate, String origin, boolean searchForOrigin) {
        if (searchForOrigin) {
            if (Objects.equal(viewNamesTree.name, origin)) {
                return true;
            }
        } else {
            if (Objects.equal(viewNamesTree.name, candidate)) {
                return traverseOptimalType(viewNamesTree, candidate, origin, true);
            }
        }

        for (ConvertExecutor.Tree child : viewNamesTree.children) {
            if (traverseOptimalType(child, candidate, origin, searchForOrigin)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 首字母大写(蛇驼峰式)
     * @param snakeCase String
     * @return String
     */
    private String snakeCaseToCamelCase(String snakeCase) {
        List<String> capitalized = Lists.newArrayList();

        boolean isFirst = true;
        for (String s : snakeCase.split("_")) {
            if (isFirst) {
                capitalized.add(s);
            } else {
                capitalized.add(StringUtils.capitalize(s));
            }

            isFirst = false;
        }

        return StringUtils.join(capitalized, "");
    }

    @Override
    public String toString() {
        return "AndroidViewInfo{" +
                "type='" + type + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
