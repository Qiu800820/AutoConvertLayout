package main.view;

import org.apache.http.util.TextUtils;

/**
 * Created by TUS on 2016/7/11.
 */
public class OnRefresh implements BaseListener{

    private String id;

    @Override
    public BaseListener addId(String id) {
        this.id = id;
        return this;
    }

    @Override
    public String toListenerString(String field) {
        if(TextUtils.isEmpty(field))
            return "";
        return "\n\t"+ field + ".setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {\n" +
                "\t\t\n" +
                "\t\t@Override\n" +
                "\t\tpublic void onRefresh() {\n" +
                "\t\t\t// TODO Create onRefresh Task \n" +
                "\t\t}\n" +
                "\t});\n";
    }
}
